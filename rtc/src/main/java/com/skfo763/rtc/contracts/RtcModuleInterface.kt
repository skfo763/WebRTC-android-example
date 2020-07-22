package com.skfo763.rtc.contracts

import com.skfo763.rtc.data.SignalServerInfo
import org.webrtc.SurfaceViewRenderer

interface RtcModuleInterface {

    var rtcViewInterface: RtcViewInterface

    fun setPeerInfo(peer : SignalServerInfo)

    fun startWaiting(peer: SignalServerInfo)

    fun stopCallSignFromClient(stoppedAt: StopCallType, shouldCloseSocket: Boolean)

}