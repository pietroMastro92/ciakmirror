package dev.imirror.receiver.airplay

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import dev.imirror.receiver.util.Logger

/** Snapshot of URL-video playback for `GET /playback-info`. */
data class PlaybackInfo(
    val durationSec: Double,
    val positionSec: Double,
    val rate: Double,        // 0.0 = paused, 1.0 = playing
    val readyToPlay: Boolean,
)

/**
 * AirPlayVideoPlayer — plays an AirPlay "video URL" stream (the non-mirroring mode: a sender app
 * like Safari says "AirPlay this video" and POSTs a URL via `/play`).
 *
 * We use **Media3 ExoPlayer** instead of Android's platform MediaPlayer: Fire OS's HLS stack
 * (NuPlayer/LiveSession) rejects modern fMP4/CMAF playlists with error -1007 ("Unable to parse
 * ID3 tag") and crashes on some TS streams, leaving a black screen with no audio. ExoPlayer
 * ships its own HLS demuxer (TS + fMP4/CMAF) and is independent of the device firmware.
 *
 * Distinct from screen mirroring (H.264 over a data stream): here the TV fetches and plays the
 * media itself, and the sender only drives transport (`/rate`, `/scrub`, `/stop`) + polls
 * `/playback-info`.
 *
 * THREADING: the RTSP thread calls play/setRate/scrub/release/info, while ExoPlayer must only be
 * touched from the thread of its application Looper (the main thread). Every player command is
 * therefore posted to the main thread; [info] reads lock-free @Volatile snapshots updated by the
 * player listener + a 500 ms ticker.
 */
class AirPlayVideoPlayer(
    private val context: Context,
    private val surfaceProvider: () -> Surface?,
    private val onEnded: () -> Unit = {},
    private val onError: (String) -> Unit = {},
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private var initialSeekApplied = false

    @Volatile private var prepared = false
    @Volatile private var durationMs = 0L
    @Volatile private var positionMs = 0L
    @Volatile private var playing = false

    private val progressTicker = object : Runnable {
        override fun run() {
            val p = player ?: return
            durationMs = p.duration.coerceAtLeast(0L)
            positionMs = p.currentPosition.coerceAtLeast(0L)
            playing = p.isPlaying
            mainHandler.postDelayed(this, PROGRESS_INTERVAL_MS)
        }
    }

    /** Starts playing [url], seeking to [startPositionFraction] (0..1 of duration) once ready. */
    fun play(url: String, startPositionFraction: Double) {
        val fraction = startPositionFraction.coerceIn(0.0, 1.0)
        mainHandler.post {
            releaseInternal()
            Logger.i("AirPlay video: play url=$url start=$fraction")
            initialSeekApplied = false
            runCatching { startInternal(url, fraction) }
                .onFailure { Logger.e("AirPlay video: setup failed", it); onError("setup") }
        }
    }

    private fun startInternal(url: String, startFraction: Double) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(context.applicationContext, httpFactory)

        val exo = ExoPlayer.Builder(context.applicationContext).build()
        player = exo
        exo.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true,
        )
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        prepared = true
                        durationMs = exo.duration.coerceAtLeast(0L)
                        // Start-Position is a fraction of the total duration; apply it once.
                        if (!initialSeekApplied && startFraction > 0.0 && durationMs > 0) {
                            exo.seekTo((startFraction * durationMs).toLong())
                        }
                        initialSeekApplied = true
                        Logger.i("AirPlay video: ready dur=${durationMs}ms")
                    }
                    Player.STATE_ENDED -> {
                        Logger.i("AirPlay video: completed")
                        playing = false
                        onEnded()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                Logger.e("AirPlay video error: ${error.errorCodeName} (code=${error.errorCode})", error)
                prepared = false
                playing = false
                onError(error.errorCodeName)
            }
        })

        exo.setMediaSource(buildMediaSource(url, dataSourceFactory))
        surfaceProvider()?.let { exo.setVideoSurface(it) }
        exo.prepare()
        exo.playWhenReady = true
        mainHandler.post(progressTicker)
    }

    /** HLS (.m3u8, both TS and fMP4/CMAF segments) or progressive MP4/WebM. */
    private fun buildMediaSource(url: String, dataSourceFactory: DefaultDataSource.Factory): MediaSource {
        val item = MediaItem.fromUri(url)
        val path = url.substringBefore('?').lowercase()
        val isHls = path.endsWith(".m3u8") || path.endsWith(".m3u") ||
            url.lowercase().contains("format=m3u8")
        return if (isHls) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(item)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(item)
        }
    }

    /** rate ≤ 0 pauses, > 0 resumes. */
    fun setRate(rate: Float) {
        mainHandler.post { player?.playWhenReady = rate > 0f }
    }

    /** Seeks to [positionSec] seconds. */
    fun scrub(positionSec: Double) {
        mainHandler.post { player?.seekTo((positionSec * 1000).toLong()) }
    }

    /** Current playback snapshot for `/playback-info`, or null if nothing is loaded. */
    fun info(): PlaybackInfo? {
        if (player == null) return null
        if (!prepared) return PlaybackInfo(0.0, 0.0, 0.0, readyToPlay = false)
        return PlaybackInfo(
            durationSec = durationMs / 1000.0,
            positionSec = positionMs / 1000.0,
            rate = if (playing) 1.0 else 0.0,
            readyToPlay = true,
        )
    }

    /** Re-attach the streaming surface (SurfaceView recreated on foreground, or created late). */
    fun attachSurface() {
        mainHandler.post { player?.setVideoSurface(surfaceProvider()) }
    }

    fun release() {
        mainHandler.post { releaseInternal() }
    }

    private fun releaseInternal() {
        mainHandler.removeCallbacks(progressTicker)
        player?.let { p ->
            runCatching { p.stop() }
            runCatching { p.release() }
        }
        player = null
        prepared = false
        playing = false
        durationMs = 0L
        positionMs = 0L
        initialSeekApplied = false
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 500L

        // Some CDNs reject ExoPlayer's default UA; present a normal Safari UA.
        const val USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/17.0 Safari/605.1.15"
    }
}
