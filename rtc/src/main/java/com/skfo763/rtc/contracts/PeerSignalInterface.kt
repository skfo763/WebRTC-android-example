package com.skfo763.rtc.contracts

import com.skfo763.rtc.data.UserJoinInfo
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface PeerSignalInterface {
    
    fun onOfferReceived(description: SessionDescription)

    fun onAnswerReceived(description: SessionDescription)

    fun onIceCandidateReceived(iceCandidate: IceCandidate)

    fun getUserInfo() : UserJoinInfo

    fun updateWaitingDescription(text: String)

    fun sendOthersIdAndDuration(otherIdx: Int, duration: Int)

    fun getStatusAboutCall(): Boolean

}