package com.skfo763.rtc.core

import com.skfo763.rtc.contracts.PeerSignalCallback
import com.skfo763.rtc.data.CALL_STARTED
import com.skfo763.rtc.manager.PeerManager
import com.skfo763.rtc.manager.PeerManagerImpl
import com.skfo763.rtc.manager.SocketManager
import com.skfo763.rtc.manager.SocketManagerImpl
import org.json.JSONObject
import org.webrtc.*
import java.util.concurrent.atomic.AtomicBoolean

class RtcManager: PeerConnection.Observer, PeerSignalCallback {

    /** for block Peer dispose duplicate : it is useful for hangup process **/
    private var isStart = AtomicBoolean(false)

    private val socketManager: SocketManager = SocketManagerImpl(this)
    private val peerManager: PeerManager = PeerManagerImpl()


    override fun onIceCandidate(p0: IceCandidate?) {
        TODO("Not yet implemented")
    }

    override fun onDataChannel(p0: DataChannel?) {
        TODO("Not yet implemented")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        TODO("Not yet implemented")
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        TODO("Not yet implemented")
    }

    override fun onAddStream(p0: MediaStream?) {
        TODO("Not yet implemented")
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        TODO("Not yet implemented")
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        TODO("Not yet implemented")
    }

    override fun onRemoveStream(p0: MediaStream?) {
        TODO("Not yet implemented")
    }

    override fun onRenegotiationNeeded() {
        TODO("Not yet implemented")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        TODO("Not yet implemented")
    }


    /* ----- */


    override fun onConnected() {
        TODO("Not yet implemented")
    }

    override fun createMatchingOffer() {
        isStart.set(true)
        peerManager.callOffer(sdpObserver)
        // TODO(UI 이벤트)
        socketManager.sendCommonEventToSocket(CALL_STARTED)
    }

    override fun createMatchingAnswer() {
        isStart.set(true)
        peerManager.callAnswer(sdpObserver)
        //TODO(UI 이벤트)
        socketManager.sendCommonEventToSocket(CALL_STARTED)
    }

    override fun onOfferReceived(description: SessionDescription) {
        TODO("Not yet implemented")
    }

    override fun onAnswerReceived(description: SessionDescription) {
        TODO("Not yet implemented")
    }

    override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
        TODO("Not yet implemented")
    }

    override fun onMatched(data: JSONObject) {
        TODO("Not yet implemented")
    }

    override fun onHangUp(data: JSONObject) {
        TODO("Not yet implemented")
    }

    override fun onHangUpSuccess() {
        TODO("Not yet implemented")
    }

    override fun onTerminate(terminateState: String) {

    }

    override fun onError(isCritical: Boolean, showMessage: Boolean, message: String?) {
        TODO("Not yet implemented")
    }

    override fun onWaitingStatusReceived(data: JSONObject) {
        TODO("Not yet implemented")
    }
}