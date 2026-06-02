package org.fossify.clock.helpers

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TalkingClockAnnouncementFormatterTest {
    @Test
    fun secondsOnlyIntervalAnnouncesOnlyCurrentSeconds() {
        assertEquals(
            "45",
            TalkingClockAnnouncementFormatter.getSpokenTime(
                hours = 14,
                minutes = 7,
                seconds = 45,
                use24HourFormat = true,
                intervalSeconds = 30,
                locale = Locale.US,
            )
        )
    }

    @Test
    fun minuteOrLongerIntervalAnnouncesCurrentTime() {
        assertEquals(
            "14:07",
            TalkingClockAnnouncementFormatter.getSpokenTime(
                hours = 14,
                minutes = 7,
                seconds = 45,
                use24HourFormat = true,
                intervalSeconds = 60,
                locale = Locale.US,
            )
        )
    }
}
