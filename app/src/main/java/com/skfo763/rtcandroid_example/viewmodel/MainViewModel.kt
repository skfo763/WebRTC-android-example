package com.skfo763.rtcandroid_example.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.skfo763.rtc.core.FaceChatRtcManager
import com.skfo763.rtc.data.SignalServerInfo
import com.skfo763.rtc.data.StunAndTurn
import com.skfo763.rtc.data.UserJoinInfo
import com.skfo763.rtcandroid_example.*
import io.reactivex.disposables.CompositeDisposable
import org.webrtc.SurfaceViewRenderer

class MainViewModel(val rtcModule: FaceChatRtcManager) : ViewModel() {

    var token: String = ""

    private val stunAndTurnList = listOf(
        StunAndTurn(
            listOf(STUN_SERVER, TURN_SERVER),
            USERNAME,
            CREDENTIAL
        )
    )

    private val signalServer = SignalServerInfo(SIGNAL_SERVER, stunAndTurnList, PASSWORD)
    private val compositeDisposable = CompositeDisposable()

    val fragmentType = MutableLiveData<Int>()

    fun setLocalSurface(surfaceViewRenderer: SurfaceViewRenderer) {
        rtcModule.addLocalSurfaceView(surfaceViewRenderer)
    }

    fun setRemoteSurface(surfaceViewRenderer: SurfaceViewRenderer) {
        rtcModule.addRemoteView(surfaceViewRenderer)
    }

    fun setPeerInfo() {
        rtcModule.setPeerInfo(signalServer)
    }

    fun startLocalRendering() {
        rtcModule.startLocalSurfaceRendering()
    }

    fun startRemoteRendering() {
        rtcModule.startRemoteSurfaceRendering()
    }

    fun setRtcWaiting() {
        rtcModule.startWaiting(signalServer)
    }

    fun getUserJoinInfo(): UserJoinInfo {
        return UserJoinInfo(token, PASSWORD, false)
    }

}

