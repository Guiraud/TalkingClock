package org.fossify.clock.helpers

import org.fossify.commons.helpers.MINUTE_SECONDS
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TalkingClockAnnouncementFormatter {
    fun getSpokenTime(
        hours: Int,
        minutes: Int,
        seconds: Int,
        use24HourFormat: Boolean,
        intervalSeconds: Int,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (TalkingClockScheduler.getSafeIntervalSeconds(intervalSeconds) < MINUTE_SECONDS) {
            return seconds.toString()
        }

        val pattern = if (use24HourFormat) {
            FORMAT_24H
        } else {
            FORMAT_12H
        }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hours)
            set(Calendar.MINUTE, minutes)
            set(Calendar.SECOND, seconds)
            set(Calendar.MILLISECOND, 0)
        }

        return SimpleDateFormat(pattern, locale).format(calendar.time)
    }
}
