package com.skfo763.rtc.manager.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.skfo763.rtc.manager.audio.MAudioManager
import java.lang.Exception

class MAudioManagerImpl(context: Context): AudioManager.OnAudioFocusChangeListener, MAudioManager {
    private val audioManager = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)

    override fun audioFocusDucking() {
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.requestAudioFocus(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        ).build()
                )
            } else {
                audioManager.requestAudioFocus(
                    this,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (e: Exception) {
            Log.e("MAudioManager","AudioFocusDucking: $e")
        }
    }

    override fun audioFocusLoss() {
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        ).build()
                )
            } else {
                audioManager.abandonAudioFocus { this }
            }
        } catch (e: Exception) {
            Log.e("MAudioManager","AudioFocusLoss: $e")
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when(focusChange) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED->{ }
            AudioManager.AUDIOFOCUS_GAIN->{
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                    AudioManager.STREAM_VOICE_CALL
                )
            }
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT->{}
            AudioManager.AUDIOFOCUS_LOSS->{}
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT->{}
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->{
                audioManager.setStreamVolume(
                    AudioManager.STREAM_VOICE_CALL,
                    0 ,
                    AudioManager.STREAM_VOICE_CALL
                )
            }
        }
    }
}