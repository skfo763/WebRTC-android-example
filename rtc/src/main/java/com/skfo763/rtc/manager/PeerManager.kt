package com.skfo763.rtc.manager

import android.content.Context
import com.skfo763.rtc.data.MAudioDeviceModule
import com.skfo763.rtc.data.*
import com.skfo763.rtc.inobs.PeerConnectionObserver
import org.webrtc.*

abstract class PeerManager(context: Context, private val observer: PeerConnectionObserver) {

    protected val rootEglBase = EglBase.create()
    private val iceServer = mutableListOf<PeerConnection.IceServer>()

    protected val peerConnectionFactory by lazy {
        buildPeerConnectionFactory(context)
    }

    protected val peerConnection by lazy {
        buildPeerConnection()
    }

    private val constraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_VIDEO, "true"))
        mandatory.add(MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_AUDIO, "true"))
        optional.add(MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT, "true"))
    }

    abstract fun PeerConnectionFactory.Builder.peerConnectionFactory(): PeerConnectionFactory.Builder

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
                true,
                true
        )
    }

    private fun getVideoDecoderFactory(): DefaultVideoDecoderFactory {
        return DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)
    }

    private fun buildPeerConnection(): PeerConnection? {

        val rtcConfig = PeerConnection.RTCConfiguration(iceServer).apply {
            /* TCP candidates are only useful when connecting to a server that supports. ICE-TCP. */
            iceTransportsType = PeerConnection.IceTransportsType.RELAY
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            /* Enable DTLS for normal calls and disable for loopback calls. */
            enableDtlsSrtp = true
            // sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
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

    fun callOffer(sdpObserver: SdpObserver) {
        peerConnection?.createOffer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                        observer.onPeerError(true, showMessage = false, message = p0)
                    }

                    override fun onSetSuccess() {

                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onCreateFailure(p0: String?) {
                        observer.onPeerError(true, showMessage = false, message = p0)
                    }
                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }
        }, constraints)
    }

    fun callAnswer(sdpObserver: SdpObserver) {
        peerConnection?.createAnswer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                        observer.onPeerError(true, showMessage = false, message = p0)
                    }

                    override fun onSetSuccess() {

                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onCreateFailure(p0: String?) {
                        observer.onPeerError(true, showMessage = false, message = p0)
                    }
                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }
        }, constraints)
    }

    fun onRemoteSessionReceived(sdpObserver: SdpObserver, sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(sdpObserver, sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    open fun disconnectPeer() {
        peerConnection?.dispose()
        peerConnectionFactory.dispose()
        rootEglBase.release()
        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }

    open fun startLocalVoice() {
        // for voice peer manager
    }

    open fun initSurfaceView(surfaceViewRenderer: SurfaceViewRenderer) {
        // for video peer manager
    }

    open fun startLocalVideoCapture(localSurfaceView: SurfaceViewRenderer) {
        // for video peer manager
    }

    open fun startRemoteVideoCapture(localSurfaceView: SurfaceViewRenderer) {
        // for video peer manager
    }

}