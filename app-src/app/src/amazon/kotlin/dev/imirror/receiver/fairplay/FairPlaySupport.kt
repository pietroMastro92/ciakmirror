package dev.imirror.receiver.fairplay

import android.content.Context

/** Amazon/store flavor: no FairPlay SAP helper (it is not distributed). */
object FairPlaySupport {
    fun start(
        context: Context,
        onUrl: (url: String, startPositionSec: Double) -> Unit,
        onScrub: (positionSec: Double) -> Unit,
        onRate: (rate: Float) -> Unit,
        onStop: () -> Unit,
    ) {
        // Intentionally empty: the Amazon build has no encrypted-video helper.
    }

    fun stop() {
        // Intentionally empty.
    }
}
