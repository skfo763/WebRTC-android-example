package com.skfo763.rtc.manager

import com.skfo763.rtc.contracts.StopCallType
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SocketManager {

    fun initializeSocket(url: String)

    fun sendJoinToSocket(userInfo: JSONObject)

    fun sendOfferAnswerToSocket(sessionDescription: SessionDescription)

    fun sendIceCandidateToSocket(iceCandidate: IceCandidate)

    fun sendHangUpEventToSocket(data: Any, stoppedAt: StopCallType)

    fun sendCommonEventToSocket(data: String)

    fun disconnectSocket()
}