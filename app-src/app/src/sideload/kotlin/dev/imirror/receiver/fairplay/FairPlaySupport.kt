package dev.imirror.receiver.fairplay

import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
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
    private const val HELPER_VERSION = "4"   // bump when the helper binary changes
    private const val HELPER_PORT = 7001
    private const val HELPER_NAME = "iMirror Video"

    private val URL_REGEX = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"")
    private val POSITION_REGEX = Regex("\"position\"\\s*:\\s*([0-9.]+)")
    private val SCRUB_REGEX = Regex("\"control\"\\s*:\\s*\"scrub\".*?\"position\"\\s*:\\s*([0-9.]+)")
    private val RATE_REGEX = Regex("\"control\"\\s*:\\s*\"rate\".*?\"value\"\\s*:\\s*([0-9.]+)")
    private val STOP_REGEX = Regex("\"control\"\\s*:\\s*\"stop\"")
    private val TXT_REGEX = Regex("\"([A-Za-z0-9]+)=([^\"]*)\"")

    private var process: Process? = null
    private var reader: Thread? = null
    private var nsdManager: NsdManager? = null
    private var nsdListener: NsdManager.RegistrationListener? = null

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
        val command = listOf(
            binary.absolutePath,
            "-fairplay",
            "-listen", "0.0.0.0:$HELPER_PORT",
            "-name", HELPER_NAME,
            "-duration", "1800s"
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

                        if (line.contains("\"address\"") || line.contains("\"txt\"")) {
                            Log.i("iMirrorFP", "helper startup: $line")
                        }

                        // The helper prints its TXT record once at startup. Advertise the service
                        // through the app's NsdManager: the Go mDNS responder expires after a while
                        // and "iMirror Video" disappears from the AirPlay picker.
                        if (nsdListener == null && line.contains("\"txt\"")) {
                            val txt = TXT_REGEX.findAll(line)
                                .associate { it.groupValues[1] to it.groupValues[2] }
                            if (txt["pk"] != null && txt["pi"] != null) {
                                registerMdns(app, txt)
                            }
                        }

                        if (line.contains("\"control\"") ||
                            line.contains("\"path\":\"/rate\"") ||
                            line.contains("\"path\":\"/scrub\"") ||
                            line.contains("\"path\":\"/stop\"") ||
                            line.contains("\"path\":\"/play\"") ||
                            (line.contains("\"encrypted\":true") && !line.contains("\"/playback-info\""))
                        ) {
                            android.util.Log.i("iMirrorFP", line)
                        }

                        SCRUB_REGEX.find(line)?.let { match ->
                            match.groupValues.getOrNull(1)?.toDoubleOrNull()?.let {
                                android.util.Log.i("iMirrorFP", "onScrub $it")
                                onScrub(it)
                            }
                            return@forEach
                        }
                        RATE_REGEX.find(line)?.let { match ->
                            match.groupValues.getOrNull(1)?.toFloatOrNull()?.let {
                                android.util.Log.i("iMirrorFP", "onRate $it")
                                onRate(it)
                            }
                            return@forEach
                        }
                        if (STOP_REGEX.containsMatchIn(line)) {
                            android.util.Log.i("iMirrorFP", "onStop")
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
        nsdListener?.let { listener -> runCatching { nsdManager?.unregisterService(listener) } }
        nsdListener = null
        nsdManager = null
        process?.destroy()
        process = null
        Logger.i("FairPlay helper stopped")
    }

    /** Registers "iMirror Video" via Android's mDNS (stable, unlike the Go helper's responder). */
    private fun registerMdns(context: Context, txt: Map<String, String>) {
        runCatching {
            val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
            val info = NsdServiceInfo().apply {
                serviceName = HELPER_NAME
                serviceType = "_airplay._tcp"
                port = HELPER_PORT
                txt.forEach { (key, value) -> setAttribute(key, value) }
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(info: NsdServiceInfo) {
                    Log.i("iMirrorFP", "mDNS registered: ${info.serviceName}:$HELPER_PORT")
                }
                override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                    Log.e("iMirrorFP", "mDNS registration failed: $errorCode")
                }
                override fun onServiceUnregistered(info: NsdServiceInfo) {}
                override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
            }
            nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
            nsdManager = nsd
            nsdListener = listener
        }.onFailure { Log.e("iMirrorFP", "mDNS register error", it) }
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
