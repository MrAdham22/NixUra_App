package com.example.reclaimapp.presentation.auth

import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics

enum class AutofillKind { Email, Username, Password, NewPassword, OneTimeCode }

/**
 * Tells the platform what a field is so password managers / autofill can fill it.
 * Kept in this one tiny file: if a future Compose version moves `ContentType`, only this file changes.
 */
fun Modifier.autofill(kind: AutofillKind?): Modifier = if (kind == null) this else semantics {
    contentType = when (kind) {
        AutofillKind.Email -> ContentType.EmailAddress
        AutofillKind.Username -> ContentType.Username
        AutofillKind.Password -> ContentType.Password
        AutofillKind.NewPassword -> ContentType.NewPassword
        AutofillKind.OneTimeCode -> ContentType.SmsOtpCode
    }
}
