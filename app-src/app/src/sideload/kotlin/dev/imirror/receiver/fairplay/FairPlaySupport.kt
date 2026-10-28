package dev.imirror.receiver.fairplay

import android.content.Context
import android.content.Intent
import dev.imirror.receiver.MainActivity
import dev.imirror.receiver.util.Logger
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.NetworkInterface.getNetworkInterfaces

/**
 * FairPlaySupport (flavor sideload) — personal-use bridge to the go-atv FairPlay SAP
 * receiver.
 *
 * The Go receiver binary is intentionally NOT committed to the public repository: it is
 * placed in src/sideload/assets only for local builds and is loaded at runtime. The
 * public/Amazon builds use the no-op variant in src/amazon.
 *
 * The helper advertises "iMirror Video" on port 7001, receives the iOS 27 encrypted
 * /play session, prints the media URL + playback controls, and the app mirrors those
 * controls onto ExoPlayer.
 */
object FairPlaySupport {

    private const val ASSET = "fairplaysap-arm"
    private const val HELPER_VERSION = "2"   // bump when the helper binary changes
    private const val HELPER_PORT = 7001
    private const val HELPER_NAME = "iMirror Video"

    private val URL_REGEX = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"")
    private val POSITION_REGEX = Regex("\"position\"\\s*:\\s*([0-9.]+)")
    private val SCRUB_REGEX = Regex("\"control\"\\s*:\\s*\"scrub\".*?\"position\"\\s*:\\s*([0-9.]+)")
    private val RATE_REGEX = Regex("\"control\"\\s*:\\s*\"rate\".*?\"value\"\\s*:\\s*([0-9.]+)")
    private val STOP_REGEX = Regex("\"control\"\\s*:\\s*\"stop\"")

    private var process: Process? = null
    private var reader: Thread? = null

    @Volatile private var running = false

    fun start(
        context: Context,
        onUrl: (url: String, startPositionSec: Double) -> Unit,
        onScrub: (positionSec: Double) -> Unit,
        onRate: (rate: Float) -> Unit,
        onStop: () -> Unit,
    ) {
        if (process != null) return
        val app = context.applicationContext
        val binary = File(app.filesDir, ASSET)
        val marker = File(app.filesDir, "fairplaysap.version")
        val needsCopy = !binary.exists() || binary.length() < 1_000_000L ||
            runCatching { marker.readText().trim() }.getOrNull() != HELPER_VERSION
        if (needsCopy) {
            val copied = runCatching {
                app.assets.open(ASSET).use { input ->
                    binary.outputStream().use { output -> input.copyTo(output) }
                }
                marker.writeText(HELPER_VERSION)
            }
            if (copied.isFailure) {
                Logger.e("FairPlay helper: asset not available (public build?)", copied.exceptionOrNull())
                return
            }
        }
        if (!binary.exists()) return
        binary.setExecutable(true, false)

        val ip = localIpv4()
        if (ip == null) {
            Logger.e("FairPlay helper: no IPv4 address")
            return
        }
        val command = listOf(
            binary.absolutePath,
            "-fairplay",
            "-listen", "0.0.0.0:$HELPER_PORT",
            "-mdns-ip", ip,
            "-name", HELPER_NAME,
            "-duration", "24h"
        )
        val proc = runCatching { ProcessBuilder(command).redirectErrorStream(true).start() }
            .getOrElse {
                Logger.e("FairPlay helper: start failed", it)
                return
            }
        process = proc
        running = true
        Logger.i("FairPlay helper started on $HELPER_PORT (ip=$ip)")

        reader = Thread {
            runCatching {
                proc.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        if (!running) return@forEach

                        SCRUB_REGEX.find(line)?.let { match ->
                            match.groupValues.getOrNull(1)?.toDoubleOrNull()?.let(onScrub)
                            return@forEach
                        }
                        RATE_REGEX.find(line)?.let { match ->
                            match.groupValues.getOrNull(1)?.toFloatOrNull()?.let(onRate)
                            return@forEach
                        }
                        if (STOP_REGEX.containsMatchIn(line)) {
                            onStop()
                            return@forEach
                        }
                        val urlMatch = URL_REGEX.find(line) ?: return@forEach
                        val url = urlMatch.groupValues[1]
                        val startSec = POSITION_REGEX.find(line)
                            ?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                        Logger.i("FairPlay helper captured media URL")
                        runCatching {
                            app.startActivity(
                                Intent(app, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                        onUrl(url, startSec)
                    }
                }
            }.onFailure { if (running) Logger.e("FairPlay helper reader error", it) }
        }.also { it.isDaemon = true; it.start() }
    }

    fun stop() {
        running = false
        reader?.interrupt()
        reader = null
        process?.destroy()
        process = null
        Logger.i("FairPlay helper stopped")
    }

    /** First non-loopback IPv4 address (Wi-Fi/Ethernet). */
    private fun localIpv4(): String? = runCatching {
        getNetworkInterfaces()?.toList()
            ?.filter { it.isUp && !it.isLoopback }
            ?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress
    }.getOrNull()
}
