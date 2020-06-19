package com.skfo763.rtc.contracts

import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface PeerSignalCallback {

    fun onConnected(connectData: Array<Any>)

    fun createMatchingOffer()

    fun createMatchingAnswer()

    fun onOfferReceived(description: SessionDescription)

    fun onAnswerReceived(description: SessionDescription)

    fun onIceCandidateReceived(iceCandidate: IceCandidate)

    fun onMatched(data: JSONObject)

    fun onHangUp(data: JSONObject)

    fun onHangUpSuccess()

    fun onTerminate(terminateState: String)

    fun onError(isCritical: Boolean, showMessage: Boolean = false, message: String? = null)

    fun onWaitingStatusReceived(data: JSONObject)

}