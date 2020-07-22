package com.skfo763.rtc.inobs

import org.webrtc.*

abstract class PeerConnectionObserver: PeerConnection.Observer {
    override fun onIceCandidate(iceCandidate: IceCandidate?) {
        
    }

    override fun onDataChannel(p0: DataChannel?) {
        
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        
    }

    override fun onAddStream(mediaStream: MediaStream?) {

    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        
    }

    override fun onRemoveStream(mediaStream: MediaStream?) {
        
    }

    override fun onRenegotiationNeeded() {
        
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        
    }

    abstract fun onPeerError(isCritical: Boolean, showMessage: Boolean = false, message: String? = null)

}