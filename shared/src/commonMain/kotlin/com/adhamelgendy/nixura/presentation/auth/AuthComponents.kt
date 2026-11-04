package com.adhamelgendy.nixura.presentation.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhamelgendy.nixura.domain.auth.AuthError
import com.adhamelgendy.nixura.domain.auth.AuthErrorKind
import com.adhamelgendy.nixura.domain.auth.AuthValidation
import com.adhamelgendy.nixura.domain.auth.PasswordProblem
import com.adhamelgendy.nixura.domain.auth.PasswordStrength
import com.adhamelgendy.nixura.domain.auth.PasswordStrengthMeter
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconButton
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.platform.rememberReducedMotion
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.strings.Strings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.theme.NixUraPalette

/** Localised, user-safe text for a typed error. Never shows server text. */
fun authErrorText(error: AuthError, s: Strings): String = when (error.kind) {
    AuthErrorKind.InvalidCredentials -> s.errInvalidCredentials
    AuthErrorKind.EmailNotVerified -> s.errNotVerified
    AuthErrorKind.InvalidCode -> s.errInvalidCode
    AuthErrorKind.AccountLocked -> s.errLocked(error.retryAfterSeconds)
    AuthErrorKind.RateLimited -> s.errRateLimited
    AuthErrorKind.WeakPassword -> error.passwordProblems.firstNotNullOfOrNull { passwordProblemText(it, s) } ?: s.errInvalidInput
    AuthErrorKind.InvalidInput -> s.errInvalidInput
    AuthErrorKind.Network -> s.errNetwork
    AuthErrorKind.Server -> s.errServer
    AuthErrorKind.SessionExpired -> s.errSessionExpired
    AuthErrorKind.Unknown -> s.errUnknown
}

/** Maps a server problem id ("too_common") to text. */
fun passwordProblemText(id: String, s: Strings): String? = when (id) {
    "too_short" -> s.pwTooShort
    "too_long" -> s.pwTooLong
    "too_common" -> s.pwTooCommon
    "same_as_email" -> s.pwSameAsEmail
    "low_variety" -> s.pwLowVariety
    "breached" -> s.pwBreached
    "same_as_current" -> s.pwSameAsCurrent
    else -> null
}

fun passwordProblemText(problem: PasswordProblem, s: Strings): String = when (problem) {
    PasswordProblem.TooShort -> s.pwTooShort
    PasswordProblem.TooLong -> s.pwTooLong
    PasswordProblem.TooCommon -> s.pwTooCommon
    PasswordProblem.SameAsEmail -> s.pwSameAsEmail
    PasswordProblem.LowVariety -> s.pwLowVariety
}

/** The leaf tile used on the landing and account screens. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 56.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.32f))
            .background(Brush.linearGradient(listOf(NixUraPalette.Rose, NixUraPalette.Orange))),
        contentAlignment = Alignment.Center,
    ) { NixIconView(NixIcon.Leaf, NixUraPalette.Navy, size = size * 0.57f) }
}

/** The brand's main action: rose-to-orange gradient, a slight press-in, and a spinner while [loading]. */
@Composable
fun AuthGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    trailingIcon: NixIcon? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val scale by animateFloatAsState(if (pressed && !reducedMotion) 0.97f else 1f, label = "buttonPress")
    val active = enabled && !loading
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.45f
            }
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(NixUraPalette.Rose, NixUraPalette.Orange)), shape)
            .clickable(interactionSource = interaction, indication = null, enabled = active, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(color = NixUraPalette.Navy, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(text, style = NixType.Button.copy(fontSize = 16.sp), color = NixUraPalette.Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                NixIconView(trailingIcon, NixUraPalette.Navy, size = 18.dp, strokeWidth = 2.4f)
            }
        }
    }
}

/**
 * Full-screen account layout: animated backdrop, back button with the brand, a large title and the form on a glass
 * card. Scrolls with the keyboard.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Nix.colors
    val cardShape = RoundedCornerShape(26.dp)
    Box(Modifier.fillMaxSize()) {
        AuraBackdrop()
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth().riseIn(0), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) NixIconButton(NixIcon.Back, onBack)
                Spacer(Modifier.weight(1f))
                Text("NixUra", style = NixType.Heading, color = c.textSecondary)
                Spacer(Modifier.width(10.dp))
                BrandMark(size = 34.dp)
            }
            Spacer(Modifier.height(30.dp))
            Text(title, style = NixType.Display, color = c.textPrimary, modifier = Modifier.riseIn(80).semantics { heading() })
            if (subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(subtitle, style = NixType.Body, color = c.textSecondary, modifier = Modifier.riseIn(150))
            }
            Spacer(Modifier.height(26.dp))
            Column(
                Modifier.fillMaxWidth().riseIn(230)
                    .clip(cardShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.11f), cardShape)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}

/** Text field with label, inline error, optional password show/hide and autofill hint. */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    autofill: AutofillKind? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    error: String? = null,
    maxLength: Int = 128,
    enabled: Boolean = true,
) {
    val c = Nix.colors
    val s = LocalStrings.current
    var visible by rememberSaveable { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        when {
            error != null -> c.danger
            focused -> c.accent
            else -> c.border
        },
        label = "fieldBorder",
    )
    val leadingIcon = when {
        isPassword -> NixIcon.Lock
        keyboardType == KeyboardType.Email -> NixIcon.Mail
        else -> null
    }
    Column(modifier) {
        Text(label, style = NixType.SmallStrong, color = c.textSecondary)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            enabled = enabled,
            singleLine = true,
            interactionSource = interaction,
            textStyle = NixType.Body.copy(color = c.textPrimary),
            cursorBrush = SolidColor(c.accent),
            visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onImeAction?.invoke() }, onGo = { onImeAction?.invoke() }),
            modifier = Modifier.fillMaxWidth().autofill(autofill),
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(14.dp)).background(c.surfaceRaised)
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                        .padding(start = 16.dp, end = if (isPassword) 4.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        NixIconView(leadingIcon, if (focused) c.accent else c.textMuted, size = 20.dp)
                        Spacer(Modifier.width(12.dp))
                    }
                    Box(Modifier.weight(1f).padding(vertical = 14.dp)) { inner() }
                    if (isPassword) {
                        val toggleLabel = if (visible) s.authHidePassword else s.authShowPassword
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button) { visible = !visible }
                                .semantics { contentDescription = toggleLabel },
                            contentAlignment = Alignment.Center,
                        ) {
                            NixIconView(if (visible) NixIcon.EyeOff else NixIcon.Eye, c.accent)
                        }
                    }
                }
            },
        )
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(error, style = NixType.Small, color = c.danger)
        }
    }
}

/** Four-segment meter plus a word. Display only: the policy check is what actually gates sign-up. */
@Composable
fun PasswordStrengthBar(password: String, email: String, modifier: Modifier = Modifier) {
    if (password.isEmpty()) return
    val s = LocalStrings.current
    val c = Nix.colors
    val strength = PasswordStrengthMeter.evaluate(password, email)
    val (filled, label, color) = when (strength) {
        PasswordStrength.Weak, PasswordStrength.Empty -> Triple(1, s.authStrengthWeak, c.danger)
        PasswordStrength.Fair -> Triple(2, s.authStrengthFair, c.accentWarm)
        PasswordStrength.Good -> Triple(3, s.authStrengthGood, c.accent)
        PasswordStrength.Strong -> Triple(4, s.authStrengthStrong, c.positive)
    }
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = s.authStrengthLabel(label) }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (i < filled) color else c.surfaceRaised))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = NixType.Small, color = color)
    }
}

/**
 * Six visual boxes over one hidden text field. Typing advances automatically, anything pasted or suggested by the
 * keyboard (clipboard chip, "123 456") is reduced to digits, and the code is submitted via [onComplete] when full.
 */
@Composable
fun OtpInput(
    code: String,
    onCodeChange: (String) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hasError: Boolean = false,
) {
    val c = Nix.colors
    val s = LocalStrings.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = code,
        onValueChange = {
            val clean = AuthValidation.sanitizeOtp(it)
            onCodeChange(clean)
            if (clean.length == AuthValidation.OTP_LENGTH) onComplete()
        },
        enabled = enabled,
        singleLine = true,
        cursorBrush = SolidColor(Color.Transparent),
        textStyle = NixType.Body.copy(color = Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (AuthValidation.isCompleteOtp(code)) onComplete() }),
        modifier = modifier.fillMaxWidth().focusRequester(focus).autofill(AutofillKind.OneTimeCode)
            .semantics { contentDescription = s.authCodeBoxesDescription },
        decorationBox = { inner ->
            Box {
                Row(
                    Modifier.fillMaxWidth().clickable(interactionSource = MutableInteractionSource(), indication = null) {
                        focus.requestFocus()
                        keyboard?.show()
                    },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (i in 0 until AuthValidation.OTP_LENGTH) {
                        val active = i == code.length
                        val border = when {
                            hasError -> c.danger
                            active -> c.accent
                            else -> c.border
                        }
                        Box(
                            Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp))
                                .background(c.surfaceRaised).border(if (active) 2.dp else 1.dp, border, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(code.getOrNull(i)?.toString().orEmpty(), style = NixType.Title, color = c.textPrimary, textAlign = TextAlign.Center)
                        }
                    }
                }
                Box(Modifier.size(1.dp)) { inner() } // the real (invisible) field that receives the keyboard input
            }
        },
    )
}

/** A tappable text link with a 48dp touch target. */
@Composable
fun AuthLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color? = null) {
    val c = Nix.colors
    Box(
        modifier.heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, style = NixType.BodyStrong, color = if (enabled) color ?: c.accent else c.textMuted)
    }
}

@Composable
fun AuthErrorText(error: AuthError?, modifier: Modifier = Modifier) {
    if (error == null) return
    Text(authErrorText(error, LocalStrings.current), style = NixType.Body, color = Nix.colors.danger, modifier = modifier.fillMaxWidth())
}
