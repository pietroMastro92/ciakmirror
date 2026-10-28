package dev.imirror.receiver.fairplay

import android.content.Context

/** Amazon/store flavor: no FairPlay SAP helper (it is not distributed). */
object FairPlaySupport {
    fun start(context: Context, onUrl: (String) -> Unit) {
        // Intentionally empty: the Amazon build has no encrypted-video helper.
    }

    fun stop() {
        // Intentionally empty.
    }
}
