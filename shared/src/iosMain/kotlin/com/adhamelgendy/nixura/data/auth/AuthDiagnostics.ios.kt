package com.adhamelgendy.nixura.data.auth

import platform.Foundation.NSLog

internal actual fun logAuthDiagnostic(message: String) {
    NSLog("NixUraSignIn: %@", message)
}
