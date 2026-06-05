package org.fossify.clock.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.adapters.TimeZonesAdapter
import org.fossify.clock.databinding.FragmentClockBinding
import org.fossify.clock.dialogs.AddTimeZonesDialog
import org.fossify.clock.dialogs.EditTimeZoneDialog
import org.fossify.clock.extensions.colorCompoundDrawable
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.getAllTimeZonesModified
import org.fossify.clock.extensions.getClosestEnabledAlarmString
import org.fossify.clock.extensions.getFormattedDate
import org.fossify.clock.helpers.FORMAT_12H_WITH_SECONDS
import org.fossify.clock.helpers.FORMAT_24H_WITH_SECONDS
import org.fossify.clock.helpers.TalkingClockScheduler
import org.fossify.clock.helpers.TalkingClockSpeaker
import org.fossify.clock.helpers.getPassedSeconds
import org.fossify.clock.models.MyTimeZone
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import java.util.Calendar

class ClockFragment : Fragment() {
    private val ONE_SECOND = 1000L

    private var passedSeconds = 0
    private var calendar = Calendar.getInstance()
    private val updateHandler = Handler(Looper.getMainLooper())
    private var lastTalkingClockSecond: Int? = null
    private var talkingClockSpeaker: TalkingClockSpeaker? = null

    private lateinit var binding: FragmentClockBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentClockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        setupDateTime()

        val safeContext = context ?: return
        binding.clockDate.setTextColor(safeContext.getProperTextColor())
    }

    override fun onPause() {
        super.onPause()
        updateHandler.removeCallbacksAndMessages(null)
        talkingClockSpeaker?.shutdown()
        talkingClockSpeaker = null
        lastTalkingClockSecond = null
    }

    private fun setupDateTime() {
        calendar = Calendar.getInstance()
        passedSeconds = getPassedSeconds()
        updateCurrentTime()
        updateDate()
        updateAlarm()
        setupViews()
    }

    private fun setupViews() {
        val safeContext = context ?: return
        binding.apply {
            safeContext.updateTextColors(clockFragment)
            clockTime.setTextColor(safeContext.getProperTextColor())
            val clockFormat = if (safeContext.config.use24HourFormat) {
                FORMAT_24H_WITH_SECONDS
            } else {
                FORMAT_12H_WITH_SECONDS
            }

            clockTime.format24Hour = clockFormat
            clockTime.format12Hour = clockFormat
            clockFab.setOnClickListener {
                fabClicked()
            }

            shortcutInterval1m.setOnClickListener { enableTalkingClock(60) }
            shortcutInterval2m.setOnClickListener { enableTalkingClock(120) }
            shortcutInterval5m.setOnClickListener { enableTalkingClock(300) }
            shortcutIntervalOther.setOnClickListener { showCustomIntervalPicker() }
            shortcutDuration10m.setOnClickListener { enableTalkingClockDuration(10 * 60) }
            shortcutDuration20m.setOnClickListener { enableTalkingClockDuration(20 * 60) }
            shortcutDuration30m.setOnClickListener { enableTalkingClockDuration(30 * 60) }

            updateTimeZones()
        }
    }

    private fun updateCurrentTime() {
        val hours = (passedSeconds / 3600) % 24
        val minutes = (passedSeconds / 60) % 60
        val seconds = passedSeconds % 60
        if (seconds == 0) {
            if (hours == 0 && minutes == 0) {
                updateDate()
            }

            (binding.timeZonesList.adapter as? TimeZonesAdapter)?.updateTimes()
        }

        context?.let { safeContext ->
            @Suppress("DEPRECATION")
            val isCurrentTab = isResumed && userVisibleHint

            if (safeContext.config.talkingClockEndTime != -1L && System.currentTimeMillis() > safeContext.config.talkingClockEndTime) {
                safeContext.config.talkingClockEnabled = false
                safeContext.config.talkingClockEndTime = -1L
            }

            val isTalkingClockEnabled = safeContext.config.talkingClockEnabled && isCurrentTab
            if (!isTalkingClockEnabled) {
                talkingClockSpeaker?.shutdown()
                talkingClockSpeaker = null
                lastTalkingClockSecond = null
            } else if (TalkingClockScheduler.shouldAnnounce(
                    enabled = true,
                    intervalSeconds = safeContext.config.talkingClockIntervalSeconds,
                    passedSeconds = passedSeconds,
                    lastAnnouncedSecond = lastTalkingClockSecond,
                )
            ) {
                val speaker = talkingClockSpeaker ?: TalkingClockSpeaker(safeContext).also {
                    talkingClockSpeaker = it
                }
                if (speaker.speakTime(
                        hours = hours,
                        minutes = minutes,
                        seconds = seconds,
                        use24HourFormat = safeContext.config.use24HourFormat,
                        intervalSeconds = safeContext.config.talkingClockIntervalSeconds,
                    )
                ) {
                    lastTalkingClockSecond = passedSeconds
                }
            }
        }

        updateHandler.postDelayed({
            passedSeconds++
            updateCurrentTime()
        }, ONE_SECOND)
    }

    private fun updateDate() {
        calendar = Calendar.getInstance()
        val formattedDate = requireContext().getFormattedDate(calendar)
        (binding.timeZonesList.adapter as? TimeZonesAdapter)?.todayDateString = formattedDate
    }

    fun updateAlarm() {
        val safeContext = context ?: return
        safeContext.getClosestEnabledAlarmString { nextAlarm ->
            binding.apply {
                clockAlarm.beVisibleIf(nextAlarm.isNotEmpty())
                clockAlarm.text = nextAlarm
                clockAlarm.colorCompoundDrawable(safeContext.getProperTextColor())
            }
        }
    }

    private fun updateTimeZones() {
        val safeContext = activity as? SimpleActivity ?: return
        val selectedTimeZones = safeContext.config.selectedTimeZones
        binding.timeZonesList.beVisibleIf(selectedTimeZones.isNotEmpty())
        if (selectedTimeZones.isEmpty()) {
            return
        }

        val selectedTimeZoneIDs = selectedTimeZones.map { it.toInt() }
        val timeZones = safeContext.getAllTimeZonesModified()
            .filter { selectedTimeZoneIDs.contains(it.id) } as ArrayList<MyTimeZone>
        val currAdapter = binding.timeZonesList.adapter
        if (currAdapter == null) {
            TimeZonesAdapter(safeContext, timeZones, binding.timeZonesList) {
                EditTimeZoneDialog(safeContext, it as MyTimeZone) {
                    updateTimeZones()
                }
            }.apply {
                this@ClockFragment.binding.timeZonesList.adapter = this
            }
        } else {
            (currAdapter as TimeZonesAdapter).apply {
                updatePrimaryColor()
                updateBackgroundColor(safeContext.getProperBackgroundColor())
                updateTextColor(safeContext.getProperTextColor())
                updateItems(timeZones)
            }
        }
    }

    private fun fabClicked() {
        val safeContext = activity as? SimpleActivity ?: return
        AddTimeZonesDialog(safeContext) {
            updateTimeZones()
        }
    }

    private fun enableTalkingClock(intervalSeconds: Int) {
        val safeContext = context ?: return
        safeContext.config.talkingClockEnabled = true
        safeContext.config.talkingClockIntervalSeconds = intervalSeconds
        safeContext.config.talkingClockEndTime = -1L
        safeContext.toast(org.fossify.clock.R.string.talking_clock)
    }

    private fun showCustomIntervalPicker() {
        val safeContext = activity as? SimpleActivity ?: return
        org.fossify.clock.dialogs.MyTimePickerDialogDialog(safeContext, safeContext.config.talkingClockIntervalSeconds) {
            safeContext.config.talkingClockEnabled = true
            safeContext.config.talkingClockIntervalSeconds = TalkingClockScheduler.getSafeIntervalSeconds(it)
            safeContext.config.talkingClockEndTime = -1L
            safeContext.toast(org.fossify.clock.R.string.talking_clock)
        }
    }

    private fun enableTalkingClockDuration(durationSeconds: Int) {
        val safeContext = context ?: return
        safeContext.config.talkingClockEnabled = true
        if (safeContext.config.talkingClockIntervalSeconds <= 0) {
            safeContext.config.talkingClockIntervalSeconds = 60
        }
        safeContext.config.talkingClockEndTime = System.currentTimeMillis() + (durationSeconds * 1000L)
        safeContext.toast(org.fossify.clock.R.string.talking_clock)
    }

}
