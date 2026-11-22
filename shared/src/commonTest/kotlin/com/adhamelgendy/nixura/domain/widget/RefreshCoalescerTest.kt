package com.adhamelgendy.nixura.domain.widget

import com.adhamelgendy.nixura.domain.model.AppSettings
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.model.UserProfile
import com.adhamelgendy.nixura.domain.money.HabitCost
import com.adhamelgendy.nixura.domain.repository.HabitTrackingRepository
import com.adhamelgendy.nixura.domain.repository.PreferencesRepository
import com.adhamelgendy.nixura.domain.usecase.ObserveWidgetDataChangesUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshCoalescerTest {

    // ---- RefreshCoalescer ----

    @Test fun oneRequestDrawsOnce() = runTest {
        val passes = mutableListOf<Set<Int>>()
        val c = RefreshCoalescer<Int> { passes += it }
        c.request(setOf(7))
        assertEquals(listOf(setOf(7)), passes)
    }

    @Test fun requestsDuringADrawAreMergedIntoOneMorePass() = runTest(UnconfinedTestDispatcher()) {
        val passes = mutableListOf<Set<Int>>()
        val gate = CompletableDeferred<Unit>()
        val c = RefreshCoalescer<Int> { keys ->
            passes += keys
            if (passes.size == 1) gate.await() // the first draw is slow
        }
        val first = async { c.request(setOf(1)) }
        val more = listOf(async { c.request(setOf(2)) }, async { c.request(setOf(3, 1)) }, async { c.request(setOf(2)) })
        assertFalse(first.isCompleted)
        gate.complete(Unit)
        (more + first).forEach { it.await() }
        // Four taps' redraws, two passes: the running one, then one for everything asked meanwhile (in asked order).
        assertEquals(listOf(setOf(1), linkedSetOf(2, 3, 1)), passes)
        assertEquals(listOf(2, 3, 1), passes[1].toList())
    }

    @Test fun aRequestOnlyReturnsAfterAPassThatStartedAfterIt() = runTest(UnconfinedTestDispatcher()) {
        // The "database" version: each tap writes (version++) then asks for a redraw. A redraw must show a version
        // at least as new as the write of every request it answers, i.e. shown == saved.
        var version = 0
        val shownAtStart = mutableListOf<Int>()
        val gate = CompletableDeferred<Unit>()
        val c = RefreshCoalescer<Int> {
            shownAtStart += version
            if (shownAtStart.size == 1) gate.await()
        }
        version = 1
        val tap1 = async { c.request(setOf(1)) }  // draws version 1, blocked
        version = 2
        val tap2 = async { c.request(setOf(1)) }  // written while tap 1 draws: must not be answered by that pass
        assertFalse(tap2.isCompleted)
        gate.complete(Unit)
        tap1.await(); tap2.await()
        assertEquals(listOf(1, 2), shownAtStart)
    }

    @Test fun aFailingDrawFailsOnlyItsOwnRequests() = runTest {
        var calls = 0
        val c = RefreshCoalescer<Int> { if (++calls == 1) error("launcher gone") }
        assertFailsWith<IllegalStateException> { c.request(setOf(1)) }
        c.request(setOf(1)) // the next tap is drawn normally
        assertEquals(2, calls)
    }

    @Test fun sequentialRequestsEachGetTheirOwnPass() = runTest {
        val passes = mutableListOf<Set<Int>>()
        val c = RefreshCoalescer<Int> { passes += it }
        repeat(3) { c.request(setOf(it)) }
        assertEquals(listOf(setOf(0), setOf(1), setOf(2)), passes)
    }

    // ---- ObserveWidgetDataChangesUseCase: which widget kinds a change touches ----

    /** Room re-emits after any write to a table, even with equal rows: a SharedFlow lets the test do the same. */
    private class Habits : HabitTrackingRepository {
        val flow = MutableSharedFlow<List<HabitTracking>>(replay = 1).also { it.tryEmit(emptyList()) }
        override fun observeActiveHabitTracking(): Flow<List<HabitTracking>> = flow
        override suspend fun createHabitAndStartTracking(name: String, startAtEpochMillis: Long, colorArgb: Long?, cost: HabitCost?, hideNameOutside: Boolean): Long = error("unused")
        override suspend fun updateCost(habitId: Long, cost: HabitCost?) = error("unused")
        override suspend fun recordRelapse(habitId: Long, atEpochMillis: Long) = error("unused")
        override suspend fun updateHabit(habitId: Long, name: String, colorArgb: Long?, hideNameOutside: Boolean) = error("unused")
        override suspend fun setHideNameOutside(habitId: Long, hidden: Boolean) = error("unused")
        override suspend fun archiveHabit(habitId: Long, atEpochMillis: Long) = error("unused")
    }

    private class Prefs : PreferencesRepository {
        val flow = MutableStateFlow(AppSettings())
        override fun observeProfile(): Flow<UserProfile> = flowOf(UserProfile())
        override fun observeSettings(): Flow<AppSettings> = flow
        override suspend fun saveProfile(profile: UserProfile) = error("unused")
        override suspend fun saveSettings(settings: AppSettings) { flow.value = settings }
    }

    private class Setup {
        val habits = Habits()
        val good = FakeGoodHabitRepository(listOf(goodHabit(1, 5)))
        val events = FakeEventRepository(emptyList())
        val prefs = Prefs()
        val useCase = ObserveWidgetDataChangesUseCase(habits, good, events, prefs)
    }

    @Test fun goodLogChangeTouchesOnlyQuickLogWidgets() = runTest(UnconfinedTestDispatcher()) {
        val s = Setup()
        val seen = mutableListOf<Set<WidgetKind>>()
        backgroundScope.launch { s.useCase().collect { seen += it } }
        assertTrue(seen.isEmpty()) // the current state is not a change
        s.good.setCount(1, 10, 1)
        assertEquals(listOf(setOf(WidgetKind.GOOD_QUICK_LOG)), seen)
    }

    @Test fun equalReEmissionIsNotAChange() = runTest(UnconfinedTestDispatcher()) {
        val s = Setup()
        val seen = mutableListOf<Set<WidgetKind>>()
        backgroundScope.launch { s.useCase().collect { seen += it } }
        s.habits.flow.emit(emptyList()) // same rows again
        assertTrue(seen.isEmpty())
    }

    @Test fun eventChangeTouchesOnlyEventWidgets() = runTest(UnconfinedTestDispatcher()) {
        val s = Setup()
        val seen = mutableListOf<Set<WidgetKind>>()
        backgroundScope.launch { s.useCase().collect { seen += it } }
        s.events.state.value = listOf(event(1, 5 * DAY))
        assertEquals(listOf(setOf(WidgetKind.EVENT)), seen)
    }

    @Test fun settingsChangeTouchesEveryWidget() = runTest(UnconfinedTestDispatcher()) {
        val s = Setup()
        val seen = mutableListOf<Set<WidgetKind>>()
        backgroundScope.launch { s.useCase().collect { seen += it } }
        s.prefs.flow.value = AppSettings().let { it.copy(darkTheme = !it.darkTheme) }
        assertEquals(listOf(WidgetKind.entries.toSet()), seen)
    }
}
