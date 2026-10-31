package com.example.reclaimapp.domain.usecase

import com.example.reclaimapp.domain.model.AppSettings
import com.example.reclaimapp.domain.model.HabitTracking
import com.example.reclaimapp.domain.model.UserProfile
import com.example.reclaimapp.domain.repository.HabitTrackingRepository
import com.example.reclaimapp.domain.repository.PreferencesRepository
import com.example.reclaimapp.domain.widget.DAY
import com.example.reclaimapp.domain.widget.EventWidgetState
import com.example.reclaimapp.domain.widget.FakeEventRepository
import com.example.reclaimapp.domain.widget.FakeGoodHabitRepository
import com.example.reclaimapp.domain.widget.FixedClock
import com.example.reclaimapp.domain.widget.GoodWidgetState
import com.example.reclaimapp.domain.widget.HOUR
import com.example.reclaimapp.domain.widget.QuitWidgetState
import com.example.reclaimapp.domain.widget.event
import com.example.reclaimapp.domain.widget.goodHabit
import com.example.reclaimapp.domain.widget.habitTracking
import com.example.reclaimapp.domain.widget.runSuspend
import com.example.reclaimapp.domain.widget.utcDays
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WidgetUseCasesTest {
    private val now = 10 * DAY + 9 * HOUR
    private val today = 10L
    private val clock = FixedClock(now)

    private fun repo(vararg items: com.example.reclaimapp.domain.model.GoodHabitWithLogs) = FakeGoodHabitRepository(items.toList())
    private fun inc(r: FakeGoodHabitRepository, offset: Long = 0L) = IncrementGoodHabitTodayUseCase(r, clock, utcDays(offset))
    private fun dec(r: FakeGoodHabitRepository, offset: Long = 0L) = DecrementGoodHabitTodayUseCase(r, clock, utcDays(offset))

    // ---- increment ----
    @Test fun incrementFromNothingCreatesOne() {
        val r = repo(goodHabit(1, 8))
        assertEquals(1, runSuspend { inc(r)(1) }.getOrThrow())
        assertEquals(1, r.countOn(1, today))
    }

    @Test fun incrementAddsToExistingCount() {
        val r = repo(goodHabit(1, 8, today to 3))
        assertEquals(4, runSuspend { inc(r)(1) }.getOrThrow())
    }

    @Test fun doubleIncrementAddsTwo() {
        val r = repo(goodHabit(1, 8))
        runSuspend { inc(r)(1); inc(r)(1) }
        assertEquals(2, r.countOn(1, today))
    }

    @Test fun incrementClampsAtMaxDailyCount() {
        val r = repo(goodHabit(1, 8, today to MAX_DAILY_COUNT))
        assertEquals(MAX_DAILY_COUNT, runSuspend { inc(r)(1) }.getOrThrow())
        assertEquals(0, r.writes)
    }

    @Test fun incrementCanExceedTargetUpToMax() {
        val r = repo(goodHabit(1, 2, today to 2))
        assertEquals(3, runSuspend { inc(r)(1) }.getOrThrow())
    }

    @Test fun incrementOnlyTouchesToday() {
        val r = repo(goodHabit(1, 8, (today - 1) to 5, (today + 1) to 7))
        runSuspend { inc(r)(1) }
        assertEquals(5, r.countOn(1, today - 1)); assertEquals(7, r.countOn(1, today + 1)); assertEquals(1, r.countOn(1, today))
    }

    @Test fun incrementUsesLocalDayAcrossMidnightOffset() {
        // 22:30 UTC on day 10 = day 11 in UTC+3.
        val late = FixedClock(10 * DAY + 22 * HOUR + 30 * 60_000L)
        val r = repo(goodHabit(1, 8))
        runSuspend { IncrementGoodHabitTodayUseCase(r, late, utcDays(3 * HOUR))(1) }
        assertEquals(1, r.countOn(1, 11)); assertEquals(0, r.countOn(1, 10))
    }

    @Test fun incrementJustBeforeAndAfterLocalMidnightHitsDifferentDays() {
        val c = FixedClock(11 * DAY - 1)
        val r = repo(goodHabit(1, 8))
        val use = IncrementGoodHabitTodayUseCase(r, c, utcDays())
        runSuspend { use(1) }
        c.now = 11 * DAY
        runSuspend { use(1) }
        assertEquals(1, r.countOn(1, 10)); assertEquals(1, r.countOn(1, 11))
    }

    @Test fun incrementUnknownHabitFails() {
        val r = repo(goodHabit(1, 8))
        assertTrue(runSuspend { inc(r)(99) }.isFailure)
        assertEquals(0, r.writes)
    }

    @Test fun incrementArchivedHabitFailsBecauseItIsNotListed() {
        // observeGoodHabits() only returns active habits, so an archived one is simply absent.
        val r = repo()
        assertTrue(runSuspend { inc(r)(1) }.isFailure)
    }

    @Test fun invalidIdFails() {
        val r = repo(goodHabit(1, 8))
        assertTrue(runSuspend { inc(r)(0) }.isFailure)
        assertTrue(runSuspend { dec(r)(-3) }.isFailure)
    }

    // ---- decrement ----
    @Test fun decrementSubtractsOne() {
        val r = repo(goodHabit(1, 8, today to 3))
        assertEquals(2, runSuspend { dec(r)(1) }.getOrThrow())
        assertEquals(2, r.countOn(1, today))
    }

    @Test fun decrementFloorsAtZeroWithoutWriting() {
        val r = repo(goodHabit(1, 8))
        assertEquals(0, runSuspend { dec(r)(1) }.getOrThrow())
        assertEquals(0, r.writes)
    }

    @Test fun decrementToZeroClearsTheDay() {
        val r = repo(goodHabit(1, 8, today to 1))
        runSuspend { dec(r)(1) }
        assertTrue(r.state.value.first().logs.none { it.dayIndex == today })
    }

    @Test fun incrementThenDecrementRestoresCount() {
        val r = repo(goodHabit(1, 8, today to 4))
        runSuspend { inc(r)(1); dec(r)(1) }
        assertEquals(4, r.countOn(1, today))
    }

    @Test fun otherHabitsAreUntouched() {
        val r = repo(goodHabit(1, 8, today to 2), goodHabit(2, 5, today to 4, name = "Walk"))
        runSuspend { inc(r)(1) }
        assertEquals(4, r.countOn(2, today))
    }

    // ---- concurrency: two taps while the first write is still in flight ----
    @Test fun concurrentDoubleTapDoesNotLoseAnUpdate() {
        val r = repo(goodHabit(1, 8))
        val gate = CompletableDeferred<Unit>()
        r.gate = gate
        val use = inc(r)
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val first = scope.launch { use(1) }   // reads 0, blocks inside setCount while holding the lock
        val second = scope.launch { use(1) }  // must wait for the lock, then read 1
        assertFalse(first.isCompleted); assertFalse(second.isCompleted)
        gate.complete(Unit)                   // let writes through (Unconfined resumes synchronously)
        assertTrue(first.isCompleted && second.isCompleted)
        assertEquals(2, r.countOn(1, today))
    }

    // ---- LoadWidgetStateUseCase ----
    private class FakeHabits(val list: List<HabitTracking>) : HabitTrackingRepository {
        override fun observeActiveHabitTracking(): Flow<List<HabitTracking>> = flowOf(list)
        override suspend fun createHabitAndStartTracking(name: String, startAtEpochMillis: Long, colorArgb: Long?): Long = error("unused")
        override suspend fun recordRelapse(habitId: Long, atEpochMillis: Long) = error("unused")
        override suspend fun updateHabit(habitId: Long, name: String, colorArgb: Long?) = error("unused")
        override suspend fun archiveHabit(habitId: Long, atEpochMillis: Long) = error("unused")
    }

    private class FakePrefs(var settings: AppSettings = AppSettings()) : PreferencesRepository {
        val flow = MutableStateFlow(settings)
        override fun observeProfile(): Flow<UserProfile> = flowOf(UserProfile())
        override fun observeSettings(): Flow<AppSettings> = flow
        override suspend fun saveProfile(profile: UserProfile) = error("unused")
        override suspend fun saveSettings(settings: AppSettings) { flow.value = settings }
    }

    private fun loader(
        habits: List<HabitTracking> = emptyList(),
        good: FakeGoodHabitRepository = repo(),
        events: FakeEventRepository = FakeEventRepository(emptyList()),
        prefs: FakePrefs = FakePrefs(),
    ) = LoadWidgetStateUseCase(FakeHabits(habits), good, events, prefs, clock, utcDays())

    @Test fun loaderQuitState() {
        val s = runSuspend { loader(habits = listOf(habitTracking(1, startAt = now - 2 * DAY)) ).quit(1) }
        assertEquals(2 * DAY, assertIs<QuitWidgetState.Active>(s).elapsedMillis)
    }

    @Test fun loaderGoodStateMatchesInAppCount() {
        val s = runSuspend { loader(good = repo(goodHabit(1, 3, today to 2))).good(1) }
        assertEquals(2, assertIs<GoodWidgetState.Active>(s).count)
    }

    @Test fun loaderEventNearestWhenUnbound() {
        val s = runSuspend { loader(events = FakeEventRepository(listOf(event(1, now + 5 * DAY), event(2, now + DAY)))).event(null) }
        assertEquals(2L, assertIs<EventWidgetState.Upcoming>(s).eventId)
    }

    @Test fun loaderEventMissingWhenBoundEventDeleted() {
        val s = runSuspend { loader(events = FakeEventRepository(listOf(event(1, now + DAY)))).event(42) }
        assertEquals(EventWidgetState.EventMissing, s)
    }

    @Test fun loaderAppearanceFollowsSettings() {
        val prefs = FakePrefs(AppSettings(darkTheme = false, languageCode = "en"))
        val a = runSuspend { loader(prefs = prefs).appearance() }
        assertFalse(a.darkTheme); assertEquals("en", a.languageCode)
    }

    @Test fun widgetStateSeesWidgetWriteImmediately() {
        val g = repo(goodHabit(1, 3))
        val l = loader(good = g)
        runSuspend { inc(g)(1) }
        assertEquals(1, assertIs<GoodWidgetState.Active>(runSuspend { l.good(1) }).count)
    }

    // ---- change observer ----
    @Test fun changeObserverEmitsOnEveryRepositoryChange() {
        val events = FakeEventRepository(emptyList())
        val prefs = FakePrefs()
        val g = repo(goodHabit(1, 3))
        val obs = ObserveWidgetDataChangesUseCase(FakeHabits(emptyList()), g, events, prefs)
        var emissions = 0
        val job = CoroutineScope(Dispatchers.Unconfined).launch { obs().collect { emissions++ } }
        val afterInitial = emissions
        assertTrue(afterInitial >= 1, "initial state should be emitted")
        events.state.value = listOf(event(1, 5))
        val afterEvent = emissions
        assertTrue(afterEvent > afterInitial, "event change should emit")
        runSuspend { inc(g)(1) }
        val afterLog = emissions
        assertTrue(afterLog > afterEvent, "good-habit log change should emit")
        prefs.flow.value = AppSettings(darkTheme = false)
        assertTrue(emissions > afterLog, "theme change should emit")
        job.cancel()
    }
}
