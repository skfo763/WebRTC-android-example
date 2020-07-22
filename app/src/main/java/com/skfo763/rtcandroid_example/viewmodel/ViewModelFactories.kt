package com.skfo763.rtcandroid_example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skfo763.rtc.contracts.RtcViewInterface
import com.skfo763.rtc.core.FaceChatRtcManager
import org.webrtc.SurfaceViewRenderer
import org.webrtc.audio.AudioDeviceModule

class ViewModelFactories(
    private val context: Context,
    private val localView: SurfaceViewRenderer,
    private val remoteView: SurfaceViewRenderer,
    private val audioDeviceModule: AudioDeviceModule,
    private val rtcViewInterface: RtcViewInterface
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(
            FaceChatRtcManager.initFaceChatRtcManager(
                context,
                audioDeviceModule,
                localView,
                remoteView,
                rtcViewInterface
            )
        ) as T
    }
}

