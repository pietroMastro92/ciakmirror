package dev.imirror.receiver.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import dev.imirror.receiver.BuildConfig
import dev.imirror.receiver.R
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Self-updater per le installazioni SIDELOAD (APK installato a mano):
 * controlla le release su GitHub e propone l'aggiornamento — download dell'APK
 * e avvio dell'installer di sistema. Le installazioni da store (Play/Amazon)
 * si aggiornano automaticamente e non usano questo percorso.
 *
 * Privacy: nessun dato inviato — è una semplice GET pubblica all'API di GitHub.
 */
object UpdateChecker {

    private const val API = "https://api.github.com/repos/pietroMastro92/ciakmirror/releases/latest"
    private const val PREFS = "updater"
    private const val KEY_SKIPPED = "skipped_version"

    /** Chiamato una volta all'avvio dell'activity principale. Non blocca mai l'UI. */
    fun check(activity: Activity) {
        thread(name = "update-check") {
            try {
                val conn = (URL(API).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Accept", "application/vnd.github+json")
                }
                if (conn.responseCode != 200) {
                    conn.disconnect()
                    return@thread
                }
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(body)
                val tag = json.optString("tag_name").removePrefix("v")
                if (tag.isEmpty() || !isNewer(tag, BuildConfig.VERSION_NAME)) return@thread

                val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                if (prefs.getString(KEY_SKIPPED, null) == tag) return@thread

                val apkUrl = findApk(json) ?: return@thread
                activity.runOnUiThread { showDialog(activity, tag, apkUrl) }
            } catch (_: Exception) {
                // Silenzioso: l'updater non deve mai disturbare l'esperienza normale
            }
        }
    }

    /** Confronto semver grezzo: true se [remote] > [local]. */
    private fun isNewer(remote: String, local: String): Boolean {
        fun parse(v: String) = v.split(".").mapNotNull { it.toIntOrNull() }
        val r = parse(remote)
        val l = parse(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }

    /** Preferisce l'APK universal, altrimenti armeabi-v7a. */
    private fun findApk(json: JSONObject): String? {
        val assets = json.optJSONArray("assets") ?: return null
        var armv7: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name")
            val url = a.optString("browser_download_url")
            if (!name.endsWith(".apk")) continue
            if (name.contains("universal")) return url
            if (name.contains("armeabi-v7a") || name.contains("armv7")) armv7 = url
        }
        return armv7
    }

    private fun showDialog(activity: Activity, tag: String, apkUrl: String) {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.update_title, tag))
            .setMessage(R.string.update_message)
            .setPositiveButton(R.string.update_now) { _, _ -> download(activity, tag, apkUrl) }
            .setNegativeButton(R.string.update_later) { d, _ ->
                activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_SKIPPED, tag).apply()
                d.dismiss()
            }
            .show()
    }

    private fun download(activity: Activity, tag: String, apkUrl: String) {
        thread(name = "update-download") {
            try {
                val apk = File(activity.cacheDir, "imirror-$tag.apk")
                URL(apkUrl).openStream().use { input ->
                    apk.outputStream().use { output -> input.copyTo(output) }
                }
                activity.runOnUiThread { install(activity, apk) }
            } catch (_: Exception) {
                activity.runOnUiThread {
                    Toast.makeText(activity, R.string.update_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun install(activity: Activity, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(activity, activity.packageName + ".updates", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
}
