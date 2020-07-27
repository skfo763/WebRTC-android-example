package com.skfo763.rtcandroid_example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skfo763.rtc.contracts.IFaceChatViewModelListener
import com.skfo763.rtc.core.FaceChatRtcManager
import org.webrtc.SurfaceViewRenderer
import org.webrtc.audio.AudioDeviceModule

class ViewModelFactories(
    private val context: Context,
    private val rtcViewListener: IFaceChatViewModelListener
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(
            FaceChatRtcManager.createFaceChatRtcManager(
                context,
                rtcViewListener
            )
        ) as T
    }
}

