package dev.imirror.receiver.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostics — persistent, local-only trace for the media pipeline.
 *
 * WHY: release builds plant no Timber tree, so mirroring/decoder events disappear from logcat
 * (which also rolls over quickly on Fire OS). This writes the same events to
 * /sdcard/Android/data/<pkg>/files/imirror-debug.log, readable with adb pull, capped at 512 KB.
 *
 * PRIVACY: local file only, never uploaded. No media content: sizes, ports, frame counts, errors.
 */
object Diagnostics {

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun log(tag: String, message: String) {
        Log.i("iMirrorDiag", "[$tag] $message")
        val ctx = appContext ?: return
        runCatching {
            val dir = ctx.getExternalFilesDir(null) ?: return@runCatching
            val file = File(dir, "imirror-debug.log")
            if (file.length() > 512 * 1024) file.delete()
            val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
            file.appendText("$ts [$tag] $message\n")
        }
    }
}
