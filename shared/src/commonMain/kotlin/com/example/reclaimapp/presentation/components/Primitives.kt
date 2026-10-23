package com.example.reclaimapp.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType

@Composable
fun NixCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Nix.colors.surface,
    borderColor: Color = Nix.colors.border,
    padding: PaddingValues = PaddingValues(18.dp),
    shapeRadius: Dp = 22.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(shapeRadius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(color, shape)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(eyebrow.uppercase(), style = NixType.Eyebrow, color = Nix.colors.accent)
            Spacer(Modifier.height(6.dp))
            Text(title, style = NixType.Title, color = Nix.colors.textPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = NixType.Body, color = Nix.colors.textSecondary)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = NixType.Eyebrow, color = Nix.colors.textMuted, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

enum class NixButtonStyle { Primary, Secondary, Ghost, Danger }

@Composable
fun NixButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NixButtonStyle = NixButtonStyle.Primary,
    icon: NixIcon? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val c = Nix.colors
    val bg = when (style) {
        NixButtonStyle.Primary -> c.accent
        NixButtonStyle.Secondary -> c.surfaceRaised
        NixButtonStyle.Ghost -> Color.Transparent
        NixButtonStyle.Danger -> c.danger.copy(alpha = 0.14f)
    }
    val fg = when (style) {
        NixButtonStyle.Primary -> c.onAccent
        NixButtonStyle.Secondary -> c.textPrimary
        NixButtonStyle.Ghost -> c.textSecondary
        NixButtonStyle.Danger -> c.danger
    }
    val border = when (style) {
        NixButtonStyle.Secondary -> c.border
        NixButtonStyle.Danger -> c.danger.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(shape)
            .background(if (enabled) bg else bg.copy(alpha = bg.alpha * 0.45f), shape)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            if (icon != null) {
                NixIconView(icon, fg, size = 18.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = NixType.Button, color = if (enabled) fg else fg.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun NixIconButton(
    icon: NixIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Nix.colors.textPrimary,
    background: Color = Nix.colors.surface,
    size: Dp = 42.dp,
    bordered: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background, CircleShape)
            .then(if (bordered) Modifier.border(1.dp, Nix.colors.border, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        NixIconView(icon, tint, size = size * 0.5f)
    }
}

@Composable
fun NixChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
) {
    val c = Nix.colors
    val shape = RoundedCornerShape(50)
    val bg by animateColorAsState(if (selected) c.accent else c.surfaceRaised, tween(150))
    val fg = if (selected) c.onAccent else c.textSecondary
    Row(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, if (selected) Color.Transparent else c.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = NixType.SmallStrong, color = fg, maxLines = 1)
    }
}

@Composable
fun NixTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLength: Int = 200,
) {
    val c = Nix.colors
    Column(modifier) {
        if (label != null) {
            Text(label, style = NixType.SmallStrong, color = c.textSecondary)
            Spacer(Modifier.height(6.dp))
        }
        BasicTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            singleLine = singleLine,
            textStyle = NixType.Body.copy(color = c.textPrimary),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surfaceRaised)
                        .border(1.dp, c.border, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    if (value.isEmpty()) Text(placeholder, style = NixType.Body, color = c.textMuted)
                    inner()
                }
            },
        )
    }
}

@Composable
fun NixSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = Nix.colors
    val track by animateColorAsState(if (checked) c.accent else c.surfaceRaised, tween(160))
    val thumbX by animateDpAsState(if (checked) 22.dp else 2.dp, tween(160))
    Box(
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .border(1.dp, c.border, RoundedCornerShape(50))
            .clickable(interactionSource = MutableInteractionSource(), indication = null) { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = thumbX, y = 2.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) c.onAccent else c.textSecondary),
        )
    }
}

@Composable
fun NixProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = Nix.colors.accent, track: Color = Nix.colors.surfaceRaised, height: Dp = 8.dp) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(500))
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxWidth(animated).height(height).clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
fun Avatar(initial: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val c = Nix.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(c.accent, c.accentWarm))),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial.take(1).uppercase(), style = NixType.Heading, color = Color(0xFF1D1A39))
    }
}

@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val c = Nix.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.danger.copy(alpha = 0.14f))
            .border(1.dp, c.danger.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onDismiss)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, style = NixType.Body, color = c.danger, modifier = Modifier.weight(1f))
        NixIconView(NixIcon.Close, c.danger, size = 18.dp)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
) {
    Dialog(onDismissRequest = onDismiss) {
        NixCard(color = Nix.colors.surface, padding = PaddingValues(22.dp)) {
            Text(title, style = NixType.Heading, color = Nix.colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text(message, style = NixType.Body, color = Nix.colors.textSecondary)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NixButton(cancelText, onDismiss, Modifier.weight(1f), style = NixButtonStyle.Secondary)
                NixButton(
                    confirmText,
                    onConfirm,
                    Modifier.weight(1f),
                    style = if (danger) NixButtonStyle.Danger else NixButtonStyle.Primary,
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: NixIcon,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = Nix.colors
    NixCard(modifier.fillMaxWidth(), padding = PaddingValues(28.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { NixIconView(icon, c.accent, size = 26.dp) }
            Spacer(Modifier.height(16.dp))
            Text(title, style = NixType.Heading, color = c.textPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(body, style = NixType.Body, color = c.textSecondary, textAlign = TextAlign.Center)
            if (actionText != null && onAction != null) {
                Spacer(Modifier.height(18.dp))
                NixButton(actionText, onAction, icon = NixIcon.Plus)
            }
        }
    }
}

@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 99,
    suffix: String = "",
    step: Int = 1,
    valueWidth: Dp = 72.dp,
) {
    val c = Nix.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        NixIconButton(NixIcon.Minus, { onChange((value - step).coerceAtLeast(min)) }, size = 40.dp)
        Text(
            "$value$suffix",
            style = NixType.Heading,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(valueWidth),
        )
        NixIconButton(NixIcon.Plus, { onChange((value + step).coerceAtMost(max)) }, size = 40.dp)
    }
}
