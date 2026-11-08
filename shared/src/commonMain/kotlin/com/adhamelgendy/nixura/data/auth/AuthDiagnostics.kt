package com.adhamelgendy.nixura.data.auth

/** Writes a sign-in diagnostic to the platform log (Android tag `NixUraSignIn`). Callers never pass tokens, passwords or emails. */
internal expect fun logAuthDiagnostic(message: String)
