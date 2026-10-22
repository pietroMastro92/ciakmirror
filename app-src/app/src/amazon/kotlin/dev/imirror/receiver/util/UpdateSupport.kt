package dev.imirror.receiver.util

import android.app.Activity

/**
 * Punto d'ingresso per il controllo aggiornamenti.
 *
 * Questa è la versione del flavor "amazon": è un NO-OP totale.
 * La build per l'Amazon Appstore NON contiene alcun codice di self-update
 * (nessun download di APK, nessun URL di release, nessun permesso di
 * installazione pacchetti): gli aggiornamenti arrivano SOLO dall'Appstore.
 *
 * Il flavor "sideload" sostituisce questo file con la versione che delega
 * a UpdateChecker (self-updater via GitHub Releases).
 */
object UpdateSupport {
    fun check(activity: Activity) {
        // Nessuna azione: aggiornamenti gestiti dall'Amazon Appstore.
    }
}
