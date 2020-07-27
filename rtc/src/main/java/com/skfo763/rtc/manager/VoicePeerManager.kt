package com.skfo763.rtc.manager

import android.content.Context
import com.skfo763.rtc.data.AUDIO_TRACK_ID
import com.skfo763.rtc.data.LOCAL_STREAM_ID
import com.skfo763.rtc.data.PEER_CREATE_ERROR
import com.skfo763.rtc.inobs.PeerConnectionObserver
import org.webrtc.*

open class VoicePeerManager(context: Context, private val observer: PeerConnectionObserver) : PeerManager(context, observer) {

    private val localAudioSource: AudioSource by lazy {
        peerConnectionFactory.createAudioSource(MediaConstraints())
    }

    private val localAudioTrack: AudioTrack by lazy {
        peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, localAudioSource)
    }

    private val localStream: MediaStream by lazy {
        peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)
    }

    override fun PeerConnectionFactory.Builder.peerConnectionFactory(): PeerConnectionFactory.Builder {
        return this
    }

    override fun startLocalVoice() {
        localAudioTrack.setEnabled(true)
        localStream.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream) ?: run {
            observer.onPeerError(isCritical = true, showMessage = false, message = PEER_CREATE_ERROR)
        }
    }

    override fun disconnectPeer() {
        super.disconnectPeer()
        localAudioSource.dispose()
    }

}