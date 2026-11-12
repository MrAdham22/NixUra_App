package com.adhamelgendy.nixura.presentation.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import com.adhamelgendy.nixura.domain.recognition.HabitTopic

/*
 * The habit-category glyphs (cigarette, glass, phone, dumbbell, book...). Same 24 x 24 line style as the other
 * NixIcons, written as SVG path data so the app cards and the Android widgets draw exactly the same shapes.
 */

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

/** Path data of the icons that are not drawn by hand in NixIcons.kt. */
private val topicIconData: Map<NixIcon, String> = mapOf(
    NixIcon.Cigarette to "M3,13 H17 V17 H3 Z M13,13 V17 M17,10 C17,8.6 18.5,8.4 18.5,7 C18.5,5.6 17,5.4 17,4 " +
        "M20.5,10 C20.5,8.6 22,8.4 22,7 C22,5.6 20.5,5.4 20.5,4",
    NixIcon.WineGlass to "M7,3 H17 C17,8 15,11 12,11 C9,11 7,8 7,3 Z M7.4,6 H16.6 M12,11 V20 M8,20 H16",
    NixIcon.Cup to "M4,9 H16 V15 C16,18 14,20 11,20 H9 C6,20 4,18 4,15 Z M16,11 H17.5 C19,11 20,12 20,13.5 " +
        "C20,15 19,16 17.5,16 H16 M8,6 C8,5 9,4.6 9,3.5 M12,6 C12,5 13,4.6 13,3.5",
    NixIcon.Soda to "M6,8 H18 L16.5,21 H7.5 Z M5,8 H19 M12.5,8 L14,3 H17 M6.6,13 H17.4",
    NixIcon.Candy to circle(12f, 12f, 4f) + " M8.4,10.2 L4,7.5 V16.5 L8.4,13.8 M15.6,10.2 L20,7.5 V16.5 L15.6,13.8",
    NixIcon.Burger to "M4,11 C4,6.5 7.5,4.5 12,4.5 C16.5,4.5 20,6.5 20,11 Z " +
        "M3.5,14.5 C5,13.3 6.5,15.7 8,14.5 C9.5,13.3 11,15.7 12.5,14.5 C14,13.3 15.5,15.7 17,14.5 C18.5,13.3 19.5,15 20.5,14.5 " +
        "M4.5,17.5 H19.5 C19.5,19 18.5,20 17,20 H7 C5.5,20 4.5,19 4.5,17.5 Z",
    NixIcon.Phone to "M9.5,2.5 H14.5 A2.5,2.5 0 0 1 17,5 V19 A2.5,2.5 0 0 1 14.5,21.5 H9.5 A2.5,2.5 0 0 1 7,19 V5 " +
        "A2.5,2.5 0 0 1 9.5,2.5 Z M11,18.5 H13",
    NixIcon.Gamepad to "M7,7 H17 C19.8,7 22,9.5 22,12.5 C22,15.5 20.5,18 18.5,18 C17,18 16,16.5 15,15 H9 C8,16.5 7,18 5.5,18 " +
        "C3.5,18 2,15.5 2,12.5 C2,9.5 4.2,7 7,7 Z M6.5,10.5 V13.5 M5,12 H8 M15.5,11 H15.6 M17.5,13 H17.6",
    NixIcon.Tv to "M4.5,6.5 H19.5 A2,2 0 0 1 21.5,8.5 V17.5 A2,2 0 0 1 19.5,19.5 H4.5 A2,2 0 0 1 2.5,17.5 V8.5 " +
        "A2,2 0 0 1 4.5,6.5 Z M8,2.5 L12,6.5 L16,2.5",
    NixIcon.Pill to "M9.975,18.975 L18.975,9.975 A3.5,3.5 0 0 0 14.025,5.025 L5.025,14.025 A3.5,3.5 0 0 0 9.975,18.975 Z " +
        "M14.475,14.475 L9.525,9.525",
    NixIcon.Money to "M4,6.5 H20 A1.5,1.5 0 0 1 21.5,8 V16 A1.5,1.5 0 0 1 20,17.5 H4 A1.5,1.5 0 0 1 2.5,16 V8 " +
        "A1.5,1.5 0 0 1 4,6.5 Z " + circle(12f, 12f, 2.8f) + " M6,12 H6.1 M18,12 H18.1",
    NixIcon.Dice to "M7,4 H17 A3,3 0 0 1 20,7 V17 A3,3 0 0 1 17,20 H7 A3,3 0 0 1 4,17 V7 A3,3 0 0 1 7,4 Z " +
        "M8.5,8.5 H8.6 M15.5,8.5 H15.6 M12,12 H12.1 M8.5,15.5 H8.6 M15.5,15.5 H15.6",
    NixIcon.Thought to "M7,18 A4,4 0 0 1 6.2,10.1 A5.5,5.5 0 0 1 16.8,8.6 A4.5,4.5 0 0 1 17.5,18 Z M5,21 H5.1",
    NixIcon.Chair to "M7,3.5 V20.5 M7,12.5 H17 V20.5 M7,16 H17",
    NixIcon.Speech to "M5,4.5 H19 A2,2 0 0 1 21,6.5 V14.5 A2,2 0 0 1 19,16.5 H11 L6.5,20 V16.5 H5 A2,2 0 0 1 3,14.5 " +
        "V6.5 A2,2 0 0 1 5,4.5 Z M8,10.5 H8.1 M12,10.5 H12.1 M16,10.5 H16.1",
    NixIcon.Hand to "M8,13.5 V5.5 A1.5,1.5 0 0 1 11,5.5 V11 M11,10.5 V4 A1.5,1.5 0 0 1 14,4 V11 M14,11 V5.5 " +
        "A1.5,1.5 0 0 1 17,5.5 V14 C17,18 14.5,20.5 11.5,20.5 C9.5,20.5 8,19.5 6.8,17.8 L4.3,14.2 A1.5,1.5 0 0 1 6.6,12.3 L8,13.5",
    NixIcon.Drop to "M12,3 C12,3 18.5,10 18.5,14.5 A6.5,6.5 0 0 1 5.5,14.5 C5.5,10 12,3 12,3 Z M9,15 C9,16.5 10,17.5 11.5,17.5",
    NixIcon.Dumbbell to "M5,6.5 H8 V17.5 H5 Z M16,6.5 H19 V17.5 H16 Z M2.5,9 H5 V15 H2.5 Z M19,9 H21.5 V15 H19 Z M8,12 H16",
    NixIcon.Shoe to "M3,18 H20 C21,18 21.5,17.2 21.2,16.2 C20.8,14.8 19.5,14 17.5,13.5 L13.5,12.3 L11,8.5 " +
        "C10.6,8 10,8 9.6,8.5 C8.8,9.6 7.4,10 6,9.8 L4.2,9.5 C3.5,9.4 3,10 3,10.7 Z M3,15.5 H21.2 M12.4,10.9 L13.9,9.9 M13.9,12.4 L15.4,11.4",
    NixIcon.Book to "M12,6.5 C10,5 7,4.5 3,4.5 V18 C7,18 10,18.5 12,20 C14,18.5 17,18 21,18 V4.5 C17,4.5 14,5 12,6.5 Z M12,6.5 V20",
    NixIcon.Arch to "M6,21 V11.5 C6,7.5 10,6.5 12,3 C14,6.5 18,7.5 18,11.5 V21 M4,21 H20 " +
        "M9.5,21 V13.5 C9.5,11.8 11,11 12,9.5 C13,11 14.5,11.8 14.5,13.5 V21",
    NixIcon.Lotus to "M12,5 C14.5,7.5 14.5,13 12,17 C9.5,13 9.5,7.5 12,5 Z M12,17 C8,17 4.5,14.5 4,10.5 C6.5,10.5 9,11.5 10.3,13.5 " +
        "M12,17 C16,17 19.5,14.5 20,10.5 C17.5,10.5 15,11.5 13.7,13.5 M6,20 H18",
    NixIcon.Apple to "M12,7.5 C10.5,6.5 8.5,6.3 7,7 C4.8,8 4,10.5 4.3,13 C4.7,16.5 7,20.5 9.5,20.5 C10.5,20.5 11,20 12,20 " +
        "C13,20 13.5,20.5 14.5,20.5 C17,20.5 19.3,16.5 19.7,13 C20,10.5 19.2,8 17,7 C15.5,6.3 13.5,6.5 12,7.5 Z " +
        "M12,7.5 C12,5.5 12.5,4 14,3",
    NixIcon.Heart to "M12,20 C12,20 3.5,15 3.5,9 A4.5,4.5 0 0 1 12,6.8 A4.5,4.5 0 0 1 20.5,9 C20.5,15 12,20 12,20 Z",
    NixIcon.Music to "M9,18 V5.5 L19,3.5 V16 M9,9 L19,7 " + circle(6.5f, 18f, 2.5f) + " " + circle(16.5f, 16f, 2.5f),
)

private val topicIconPaths: Map<NixIcon, Path> by lazy {
    topicIconData.mapValues { (_, data) -> PathParser().parsePathString(data).toPath() }
}

/** True for the icons drawn from [topicIconData]. */
internal fun isTopicIcon(icon: NixIcon): Boolean = icon in topicIconData

/** Draws a path-data icon on the 24-unit grid scaled by [s], with a line width of [strokePx] pixels. */
internal fun DrawScope.drawTopicIcon(icon: NixIcon, c: Color, s: Float, strokePx: Float) {
    val path = topicIconPaths[icon] ?: return
    scale(s, Offset.Zero) {
        drawPath(path, c, style = Stroke(strokePx / s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** The glyph of a habit category, shown in the badge of its card and widget. */
fun HabitTopic.icon(): NixIcon = when (this) {
    HabitTopic.SMOKING -> NixIcon.Cigarette
    HabitTopic.ALCOHOL -> NixIcon.WineGlass
    HabitTopic.HOT_DRINK -> NixIcon.Cup
    HabitTopic.SOFT_DRINK -> NixIcon.Soda
    HabitTopic.SWEETS -> NixIcon.Candy
    HabitTopic.JUNK_FOOD -> NixIcon.Burger
    HabitTopic.PHONE -> NixIcon.Phone
    HabitTopic.GAMING -> NixIcon.Gamepad
    HabitTopic.TV -> NixIcon.Tv
    HabitTopic.PILLS -> NixIcon.Pill
    HabitTopic.MONEY -> NixIcon.Money
    HabitTopic.GAMBLING -> NixIcon.Dice
    HabitTopic.SLEEP -> NixIcon.Moon
    HabitTopic.TIME -> NixIcon.Clock
    HabitTopic.MIND -> NixIcon.Thought
    HabitTopic.SITTING -> NixIcon.Chair
    HabitTopic.SPEECH -> NixIcon.Speech
    HabitTopic.HANDS -> NixIcon.Hand
    HabitTopic.ADULT_CONTENT -> NixIcon.EyeOff
    HabitTopic.WATER -> NixIcon.Drop
    HabitTopic.FITNESS -> NixIcon.Dumbbell
    HabitTopic.MOVEMENT -> NixIcon.Shoe
    HabitTopic.LEARNING -> NixIcon.Book
    HabitTopic.PRAYER -> NixIcon.Arch
    HabitTopic.CALM -> NixIcon.Lotus
    HabitTopic.WRITING -> NixIcon.Pencil
    HabitTopic.HEALTHY_FOOD -> NixIcon.Apple
    HabitTopic.MORNING -> NixIcon.Sun
    HabitTopic.SELF_CARE -> NixIcon.Sparkle
    HabitTopic.PEOPLE -> NixIcon.Heart
    HabitTopic.MUSIC -> NixIcon.Music
    HabitTopic.TIDY -> NixIcon.CircleCheck
    HabitTopic.EYES -> NixIcon.Eye
    HabitTopic.OTHER_QUIT -> NixIcon.Shield
    HabitTopic.OTHER_BUILD -> NixIcon.Leaf
}
