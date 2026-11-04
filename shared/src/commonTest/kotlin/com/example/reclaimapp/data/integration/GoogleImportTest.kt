package com.example.reclaimapp.data.integration

import com.example.reclaimapp.domain.integration.GoogleFeed
import com.example.reclaimapp.domain.model.EventCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoogleImportTest {
    private val day = 86_400_000L

    // Day index → midnight UTC; the real app uses the phone's time zone.
    private val startOfDay: (Long) -> Long = { it * day }

    @Test
    fun holidayCalendarFollowsTheCountry() {
        assertEquals("en.eg#holiday@group.v.calendar.google.com", GoogleImport.holidayCalendarId("EG"))
        assertEquals("en.de#holiday@group.v.calendar.google.com", GoogleImport.holidayCalendarId("de"))
        assertTrue(GoogleImport.isHolidayCalendar("en.usa#holiday@group.v.calendar.google.com"))
        assertFalse(GoogleImport.isHolidayCalendar("primary"))
    }

    @Test
    fun taskWithDueDateIsAnAllDayEvent() {
        val t = GTask(id = "t1", title = "Pay rent", due = "2026-11-05T00:00:00.000Z", updated = "2026-11-01T10:00:00.000Z")
        val e = GoogleImport.task("list1", t, startOfDay)!!
        assertEquals("tasks|list1|t1", e.externalId)
        assertEquals("Pay rent", e.title)
        assertTrue(e.allDay)
        assertFalse(e.cancelled)
        assertEquals(Rfc3339.parseDayIndex("2026-11-05")!! * day, e.startEpochMillis)
    }

    @Test
    fun doneOrDeletedTaskIsRemovedAndUndatedTaskIsSkipped() {
        assertTrue(GoogleImport.task("l", GTask(id = "a", status = "completed", due = "2026-11-05T00:00:00.000Z"), startOfDay)!!.cancelled)
        assertTrue(GoogleImport.task("l", GTask(id = "b", deleted = true), startOfDay)!!.cancelled)
        assertNull(GoogleImport.task("l", GTask(id = "c", title = "Someday"), startOfDay))
    }

    @Test
    fun eventIdsArePerFeed() {
        val g = GEvent(id = "e1", summary = "Birthday", start = GEventTime(date = "2026-12-01"))
        val birthday = GoogleImport.event(GoogleFeed.BIRTHDAYS, g, startOfDay)!!
        assertEquals("birthdays|e1", birthday.externalId)
        assertTrue(birthday.allDay)
        assertEquals("events|e1", GoogleImport.event(GoogleFeed.EVENTS, g, startOfDay)!!.externalId)
        assertTrue(GoogleImport.event(GoogleFeed.EVENTS, g.copy(status = "cancelled"), startOfDay)!!.cancelled)
    }

    @Test
    fun eachFeedGivesItsCategory() {
        val g = GEvent(id = "e", summary = "x", start = GEventTime(date = "2026-12-01"))
        assertEquals(EventCategory.EVENT, GoogleImport.event(GoogleFeed.EVENTS, g, startOfDay)!!.category)
        assertEquals(EventCategory.BIRTHDAY, GoogleImport.event(GoogleFeed.BIRTHDAYS, g, startOfDay)!!.category)
        assertEquals(EventCategory.HOLIDAY, GoogleImport.event(GoogleFeed.HOLIDAYS, g, startOfDay)!!.category)
        val t = GTask(id = "t", title = "x", due = "2026-11-05T00:00:00.000Z")
        assertEquals(EventCategory.TASK, GoogleImport.task("l", t, startOfDay)!!.category)
    }

    @Test
    fun otherIsTheLastCategoryInPickers() {
        assertEquals(EventCategory.OTHER, EventCategory.pickerOrder.last())
        assertEquals(EventCategory.entries.toSet(), EventCategory.pickerOrder.toSet())
    }

    @Test
    fun timedEventKeepsItsTime() {
        val g = GEvent(id = "e2", summary = "Dentist", start = GEventTime(dateTime = "2026-11-05T09:30:00Z"))
        val e = GoogleImport.event(GoogleFeed.EVENTS, g, startOfDay)!!
        assertFalse(e.allDay)
        assertEquals(Rfc3339.parseInstant("2026-11-05T09:30:00Z"), e.startEpochMillis)
    }

    @Test
    fun feedKeysRoundTripAndDefaultsLeaveHolidaysOut() {
        GoogleFeed.entries.forEach { assertEquals(it, GoogleFeed.of(it.key)) }
        assertNull(GoogleFeed.of("en.eg#holiday@group.v.calendar.google.com")) // an old calendar-id selection is ignored
        assertEquals(listOf(GoogleFeed.EVENTS, GoogleFeed.BIRTHDAYS, GoogleFeed.TASKS), GoogleFeed.defaults)
    }
}
