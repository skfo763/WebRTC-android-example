package com.skfo763.rtcandroid_example.viewmodel

import androidx.lifecycle.ViewModel
import com.skfo763.rtc.contracts.IVoiceChatViewModelListener
import com.skfo763.rtc.core.FaceChatRtcManager
import com.skfo763.rtc.data.SignalServerInfo
import com.skfo763.rtc.data.StunAndTurn
import com.skfo763.rtc.data.UserJoinInfo
import com.skfo763.rtcandroid_example.*
import com.skfo763.rtcandroid_example.utils.TokenManager
import com.skfo763.rtcandroid_example.view.MainActivity

class MainViewModel(private val rtcModule: FaceChatRtcManager) : ViewModel() {

    private val stunAndTurnList = listOf(
        StunAndTurn(
            listOf(STUN_SERVER, TURN_SERVER),
            USERNAME,
            CREDENTIAL
        )
    )

    private val signalServer = SignalServerInfo(SIGNAL_SERVER, stunAndTurnList, PASSWORD)

    fun setPeerInfo() {
        rtcModule.setPeerInfo(signalServer)
    }

    fun setRtcWaiting() {
        rtcModule.startWaiting(signalServer)
    }

    fun getUserJoinInfo(): UserJoinInfo {
        return UserJoinInfo(TokenManager.getToken(false), PASSWORD, false)
    }

}

