package com.skfo763.rtc.manager

import android.content.Context
import android.os.Build
import com.skfo763.rtc.data.*
import com.skfo763.rtc.inobs.AppSdpObserver
import com.skfo763.rtc.inobs.PeerConnectionObserver
import org.webrtc.*
import java.util.concurrent.atomic.AtomicBoolean

abstract class PeerManager(context: Context, private val observer: PeerConnectionObserver) {

    protected val rootEglBase = EglBase.create()
    private val iceServer = mutableListOf<PeerConnection.IceServer>()

    protected val peerConnectionFactory by lazy {
        buildPeerConnectionFactory(context)
    }

    protected var peerConnection: PeerConnection? = null
    private val isDisposed = AtomicBoolean(false)

    private val constraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_VIDEO, "true"))
        mandatory.add(MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_AUDIO, "true"))
        optional.add(MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT, "true"))
    }

    abstract fun PeerConnectionFactory.Builder.peerConnectionFactory(): PeerConnectionFactory.Builder

    val localStream: MediaStream by lazy {
        peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)
    }

    private fun buildPeerConnectionFactory(context: Context): PeerConnectionFactory {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .setFieldTrials(FIELD_TRIAL)
                .createInitializationOptions())
        return PeerConnectionFactory.builder()
                .setOptions(getPeerConnectionOptions())
                .setAudioDeviceModule(MAudioDeviceModule().also { it.release() })
                .setVideoDecoderFactory(getVideoDecoderFactory())
                .setVideoEncoderFactory(getVideoEncoderFactory())
                .peerConnectionFactory()
                .createPeerConnectionFactory()
    }

    private fun getPeerConnectionOptions(): PeerConnectionFactory.Options {
        return PeerConnectionFactory.Options().apply {
            networkIgnoreMask = 0
            // disableEncryption = true
            // disableNetworkMonitor = true
        }
    }

    private fun getVideoEncoderFactory(): DefaultVideoEncoderFactory {
        return DefaultVideoEncoderFactory(
                rootEglBase.eglBaseContext,
                false,
                true
        )
    }

    private fun getVideoDecoderFactory(): DefaultVideoDecoderFactory {
        return DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)
    }

    protected fun buildPeerConnection(): PeerConnection? {

        val rtcConfig = PeerConnection.RTCConfiguration(iceServer).apply {
            /* TCP candidates are only useful when connecting to a server that supports. ICE-TCP. */
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            enableDtlsSrtp = true

            /* Use ECDSA encryption. */
            // keyType = PeerConnection.KeyType.ECDSA
        }

        return peerConnectionFactory.createPeerConnection(rtcConfig, observer) ?: kotlin.run {
            observer.onPeerError(isCritical = true, showMessage = false, message = PEER_CREATE_ERROR)
            null
        }

    }

    fun setIceServer(signalServerInfo: SignalServerInfo) {
        iceServer.clear()
        iceServer.addAll(signalServerInfo.stunAndTurn.map { data ->
            PeerConnection.IceServer.builder(data.urlList)
                    .setUsername(data.userName)
                    .setPassword(data.credential)
                    .createIceServer()
        })
    }

    private val sdpObserver = object : AppSdpObserver() {
        override fun onCreateSuccess(p0: SessionDescription?) {
            peerConnection?.setLocalDescription(object : AppSdpObserver() {
                override fun onSetFailure(p0: String?) {
                    observer.onPeerError(true, showMessage = false, message = p0)
                }
                override fun onCreateFailure(p0: String?) {
                    observer.onPeerError(true, showMessage = false, message = p0)
                }
            }, p0)
            observer.onPeerCreate(p0)
        }
    }

    fun callOffer() {
        peerConnection?.createOffer(sdpObserver, constraints)
    }

    fun callAnswer() {
        peerConnection?.createAnswer(sdpObserver, constraints)
    }

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(sdpObserver, sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun closePeer() {
        peerConnection?.close()

    }

    open fun startLocalVoice() {
        // for voice peer manager
    }

    open fun disconnectPeer() {
        peerConnection?.dispose()
        peerConnection = null
        rootEglBase.release()
        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            peerConnectionFactory.dispose()
        }
    }

}