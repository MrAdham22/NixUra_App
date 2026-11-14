package com.adhamelgendy.nixura.domain.sync

/**
 * Cloud sync is paused until it moves to Firestore (see AGENTS.md). While [AVAILABLE] is false the app shows no
 * "Sync now" button or sync status, so it never promises a backup that does not run. Flip it when sync ships.
 */
object CloudSync {
    const val AVAILABLE: Boolean = false
}
