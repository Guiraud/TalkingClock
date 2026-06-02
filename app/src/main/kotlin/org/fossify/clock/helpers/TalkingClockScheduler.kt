package org.fossify.clock.helpers

object TalkingClockScheduler {
    fun getSafeIntervalSeconds(intervalSeconds: Int): Int {
        return if (intervalSeconds > 0) {
            intervalSeconds
        } else {
            DEFAULT_TALKING_CLOCK_INTERVAL_SECONDS
        }
    }

    fun shouldAnnounce(
        enabled: Boolean,
        intervalSeconds: Int,
        passedSeconds: Int,
        lastAnnouncedSecond: Int?,
    ): Boolean {
        if (!enabled || passedSeconds == lastAnnouncedSecond) {
            return false
        }

        return passedSeconds % getSafeIntervalSeconds(intervalSeconds) == 0
    }
}
