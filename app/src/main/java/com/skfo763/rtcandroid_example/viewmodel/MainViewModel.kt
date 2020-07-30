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

    val fragmentType = MutableLiveData<Int>()

    fun setWaitingLocalSurface(surfaceViewRenderer: SurfaceViewRenderer) {
        rtcModule.addWaitingSurfaceView(surfaceViewRenderer)
    }

    fun setCallingSurfaceRenderer(remote: SurfaceViewRenderer) {
        rtcModule.addCallingSurfaceView(remote)
    }

    fun setPeerInfo() {
        rtcModule.setPeerInfo(signalServer)
    }

    fun setRtcWaiting() {
        rtcModule.startWaiting(signalServer)
    }

    fun getUserJoinInfo(): UserJoinInfo {
        return UserJoinInfo(token, PASSWORD, false)
    }

    fun setCallingLocalSurfaceView(baseLocal: SurfaceViewRenderer) {
        rtcModule.addCallingLocalSurfaceView(baseLocal)
    }

    fun startWaitingLocalSurfaceView() {
        rtcModule.startWaitingLocalSurfaceView()
    }

    fun startCallingLocalSurfaceView() {
        // rtcModule.startCallingLocalSurfaceView()
    }

}

