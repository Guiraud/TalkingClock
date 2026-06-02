package org.fossify.clock.helpers

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.Locale

class TalkingClockSpeaker(context: Context) {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var isConfigured = false
    private var isLanguageReady = true

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
        }
    }

    fun speakTime(
        hours: Int,
        minutes: Int,
        seconds: Int,
        use24HourFormat: Boolean,
        intervalSeconds: Int,
    ): Boolean {
        val speech = textToSpeech ?: return false
        if (!isInitialized || speech.isSpeaking) {
            return false
        }

        configureIfNeeded(speech)
        if (!isLanguageReady) {
            return false
        }

        return speech.speak(
            TalkingClockAnnouncementFormatter.getSpokenTime(
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                use24HourFormat = use24HourFormat,
                intervalSeconds = intervalSeconds,
            ),
            TextToSpeech.QUEUE_FLUSH,
            Bundle(),
            "talking_clock_$hours-$minutes-$seconds",
        ) != TextToSpeech.ERROR
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
        isConfigured = false
    }

    private fun configureIfNeeded(speech: TextToSpeech) {
        if (isConfigured) {
            return
        }

        val languageStatus = speech.setLanguage(Locale.getDefault())
        isLanguageReady = languageStatus != TextToSpeech.LANG_MISSING_DATA &&
            languageStatus != TextToSpeech.LANG_NOT_SUPPORTED

        speech.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .build()
        )

        isConfigured = true
    }
}
