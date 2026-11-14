package com.adhamelgendy.nixura.presentation.screen

import com.adhamelgendy.nixura.presentation.util.forDisplay
import com.adhamelgendy.nixura.domain.recognition.GoodHabitFillStyle
import com.adhamelgendy.nixura.domain.recognition.PrayerHabitDetector
import com.adhamelgendy.nixura.presentation.components.GoodHabitButton
import com.adhamelgendy.nixura.presentation.components.PrayerBeads
import com.adhamelgendy.nixura.presentation.components.MAX_PRAYER_BEADS
import com.adhamelgendy.nixura.domain.widget.WidgetKind
import com.adhamelgendy.nixura.presentation.components.AddToHomeScreenButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.adhamelgendy.nixura.presentation.components.rememberCompletionPulse
import com.adhamelgendy.nixura.presentation.components.rememberGoodFill
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.domain.usecase.DeletableItem
import com.adhamelgendy.nixura.presentation.deletion.rememberDeleteFlow
import com.adhamelgendy.nixura.presentation.components.ErrorBanner
import com.adhamelgendy.nixura.presentation.components.MonthCalendar
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconButton
import com.adhamelgendy.nixura.presentation.components.NixProgressBar
import com.adhamelgendy.nixura.presentation.components.SectionLabel
import com.adhamelgendy.nixura.presentation.components.SubScreen
import com.adhamelgendy.nixura.presentation.goodhabits.GoodHabitsViewModel
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.util.formatDate
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GoodHabitDetailScreen(habitId: Long, navigator: Navigator) {
    val vm: GoodHabitsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val c = Nix.colors
    val habit = state.habits.firstOrNull { it.id == habitId }
    val deleteFlow = rememberDeleteFlow(onDeleted = { navigator.back() })

    val todayCivil = CivilCalendar.civilFromDayIndex(state.todayIndex)
    // months since year 0, so it survives rotation as a single Int
    var monthIndex by rememberSaveable { mutableStateOf(todayCivil.year * 12 + todayCivil.month - 1) }
    var selectedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val shownYear = monthIndex.floorDiv(12)
    val shownMonth = monthIndex.mod(12) + 1
    val currentMonthIndex = todayCivil.year * 12 + todayCivil.month - 1

    SubScreen(
        title = habit?.name?.forDisplay() ?: "",
        onBack = { navigator.back() },
        actions = {
            if (habit != null) {
                NixIconButton(NixIcon.Pencil, { navigator.push(Route.GoodHabitForm(habitId)) }, size = 40.dp)
                Spacer(Modifier.width(8.dp))
                NixIconButton(NixIcon.Trash, { deleteFlow.confirm(DeletableItem.GoodHabit(habit.id), habit.name) }, size = 40.dp, tint = c.danger)
            }
        },
    ) {
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }
        if (habit == null) {
            if (!state.isLoading) Text(s.habitNotFound, style = NixType.Body, color = c.textSecondary)
            return@SubScreen
        }

        // Today (a weekly habit: this week)
        val weekly = habit.isWeekly
        val look = rememberGoodFill(habit.progressCount, habit.progressTarget, habit.fillStyle)
        val pulse = rememberCompletionPulse(habit.goalReached)
        NixCard(Modifier.graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }, paint = look.paint) {
            Text((if (weekly) s.thisWeekLabel else s.todayLabel).uppercase(), style = NixType.Eyebrow, color = look.contentMuted)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${habit.progressCount} / ${habit.progressTarget}", style = NixType.counter(40), color = look.content)
                    val left = habit.progressTarget - habit.progressCount
                    val note = when {
                        weekly && habit.weekGoalMet -> s.weekGoalReached
                        weekly -> s.daysToGoThisWeek(left)
                        habit.doneToday -> s.goalReached
                        else -> s.timesToGo(left)
                    }
                    Text(note, style = NixType.Small, color = look.contentMuted)
                }
                val prayer = habit.fillStyle == GoodHabitFillStyle.PRAYER
                GoodHabitButton(prayer, look, NixIcon.Minus, { vm.onChangeToday(habit.id, -1) }, size = 46.dp, tint = look.content, background = look.content.copy(alpha = 0.10f), secondary = true)
                Spacer(Modifier.width(10.dp))
                GoodHabitButton(
                    prayer, look,
                    if (weekly && habit.doneToday) NixIcon.Tick else NixIcon.Plus, { vm.onChangeToday(habit.id, +1) }, size = 52.dp,
                    tint = if (habit.doneToday) look.background else c.onAccent,
                    background = if (habit.doneToday) look.content else c.accent,
                    haptic = true,
                )
            }
            if (habit.fillStyle == GoodHabitFillStyle.PRAYER && !weekly && habit.dailyTarget in 2..MAX_PRAYER_BEADS) {
                Spacer(Modifier.height(14.dp))
                PrayerBeads(habit.todayCount, habit.dailyTarget, look, if (habit.dailyTarget == PrayerHabitDetector.DAILY_PRAYERS) s.prayerNames else emptyList())
            }
        }

        // Stats
        if (weekly) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(s.statCurrentStreak, s.weeksCount(habit.currentStreak), Modifier.weight(1f), info = s.infoCurrentStreakWeekly)
                StatTile(s.statBestStreak, s.weeksCount(habit.bestStreak), Modifier.weight(1f), info = s.infoBestStreakWeekly)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(s.statDaysDone, habit.totalTimesDone.toString(), Modifier.weight(1f), info = s.infoDaysDoneWeekly)
                StatTile(s.statWeeksCompleted, habit.daysCompleted.toString(), Modifier.weight(1f), info = s.infoWeeksCompleted)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(s.statCurrentStreak, s.daysCount(habit.currentStreak), Modifier.weight(1f), info = s.infoCurrentStreak)
                StatTile(s.statBestStreak, s.daysCount(habit.bestStreak), Modifier.weight(1f), info = s.infoBestStreak)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(s.statTotalTimes, habit.totalTimesDone.toString(), Modifier.weight(1f), info = s.infoTotalTimes)
                StatTile(s.statDaysCompleted, habit.daysCompleted.toString(), Modifier.weight(1f), info = s.infoDaysCompleted)
            }
        }
        NixCard {
            Row {
                Text(if (weekly) s.thisWeekLabel else s.last7Days, style = NixType.BodyStrong, color = c.textSecondary, modifier = Modifier.weight(1f))
                Text("${(habit.completion7 * 100).toInt()}%", style = NixType.BodyStrong, color = c.textPrimary)
            }
            Spacer(Modifier.height(8.dp))
            NixProgressBar(habit.completion7, color = c.positive)
            Spacer(Modifier.height(14.dp))
            Row {
                Text(if (weekly) s.last4Weeks else s.last30Days, style = NixType.BodyStrong, color = c.textSecondary, modifier = Modifier.weight(1f))
                Text("${(habit.completion30 * 100).toInt()}%", style = NixType.BodyStrong, color = c.textPrimary)
            }
            Spacer(Modifier.height(8.dp))
            NixProgressBar(habit.completion30, color = c.positive)
        }

        // Calendar
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(s.calendarTitle)
            NixCard {
                MonthCalendar(
                    year = shownYear,
                    month = shownMonth,
                    countsByDay = habit.countsByDay,
                    target = if (weekly) 1 else habit.dailyTarget,
                    todayIndex = state.todayIndex,
                    createdDayIndex = habit.createdDayIndex,
                    selectedDay = selectedDay,
                    onSelectDay = { selectedDay = if (selectedDay == it) null else it },
                    onPrevMonth = { monthIndex -= 1 },
                    onNextMonth = { monthIndex += 1 },
                    canGoNext = monthIndex < currentMonthIndex,
                )
            }
            val day = selectedDay
            if (day != null) {
                val count = habit.countsByDay[day] ?: 0
                NixCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(formatDate(day, s, includeWeekday = true), style = NixType.BodyStrong, color = c.textPrimary)
                            Text(
                                when {
                                    !weekly -> s.todayProgress(count, habit.dailyTarget)
                                    count > 0 -> s.legendDone
                                    day == state.todayIndex -> s.notDoneYet
                                    else -> s.legendMissed
                                },
                                style = NixType.Small,
                                color = c.textSecondary,
                            )
                        }
                        NixIconButton(NixIcon.Minus, { vm.onSetCount(habit.id, day, count - 1) }, size = 40.dp)
                        Spacer(Modifier.width(8.dp))
                        // A weekly habit's day counts once: done shows a tick instead of a + that would do nothing.
                        NixIconButton(if (weekly && count > 0) NixIcon.Tick else NixIcon.Plus, { vm.onSetCount(habit.id, day, count + 1) }, size = 40.dp)
                    }
                }
                Text(s.editPastHint, style = NixType.Small, color = c.textMuted)
            }
        }

        // An extra, so it comes after the habit itself.
        AddToHomeScreenButton(WidgetKind.GOOD_QUICK_LOG, habit.id)
    }
}
