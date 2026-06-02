package org.fossify.clock.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TalkingClockSchedulerTest {
    @Test
    fun disabledClockNeverAnnounces() {
        assertFalse(
            TalkingClockScheduler.shouldAnnounce(
                enabled = false,
                intervalSeconds = 60,
                passedSeconds = 120,
                lastAnnouncedSecond = null,
            )
        )
    }

    @Test
    fun enabledClockAnnouncesOnlyOnIntervalBoundary() {
        assertTrue(
            TalkingClockScheduler.shouldAnnounce(
                enabled = true,
                intervalSeconds = 60,
                passedSeconds = 120,
                lastAnnouncedSecond = null,
            )
        )

        assertFalse(
            TalkingClockScheduler.shouldAnnounce(
                enabled = true,
                intervalSeconds = 60,
                passedSeconds = 121,
                lastAnnouncedSecond = null,
            )
        )
    }

    @Test
    fun sameSecondIsNotAnnouncedTwice() {
        assertFalse(
            TalkingClockScheduler.shouldAnnounce(
                enabled = true,
                intervalSeconds = 60,
                passedSeconds = 120,
                lastAnnouncedSecond = 120,
            )
        )
    }

    @Test
    fun invalidIntervalFallsBackToDefaultInterval() {
        assertEquals(
            DEFAULT_TALKING_CLOCK_INTERVAL_SECONDS,
            TalkingClockScheduler.getSafeIntervalSeconds(0),
        )

        assertTrue(
            TalkingClockScheduler.shouldAnnounce(
                enabled = true,
                intervalSeconds = 0,
                passedSeconds = DEFAULT_TALKING_CLOCK_INTERVAL_SECONDS,
                lastAnnouncedSecond = null,
            )
        )
    }
}
