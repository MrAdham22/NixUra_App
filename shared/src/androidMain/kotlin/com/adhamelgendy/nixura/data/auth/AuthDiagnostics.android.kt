package com.adhamelgendy.nixura.data.auth

internal actual fun logAuthDiagnostic(message: String) {
    runCatching { android.util.Log.w("NixUraSignIn", message) } // not available in host unit tests
}
