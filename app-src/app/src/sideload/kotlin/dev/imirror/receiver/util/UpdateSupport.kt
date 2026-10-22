package dev.imirror.receiver.util

import android.app.Activity

/** Flavor sideload: self-update abilitato (controlla le Release GitHub). */
object UpdateSupport {
    fun check(activity: Activity) {
        UpdateChecker.check(activity)
    }
}
