package com.skfo763.rtc.manager

import android.content.Context
import com.skfo763.rtc.data.*
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule

class PeerManagerImpl(
    context: Context,
    observer: PeerConnection.Observer,
    private val audioModule: AudioDeviceModule
) : PeerManager {

    private val rootEglBase = EglBase.create()

    private val iceServer = mutableListOf<PeerConnection.IceServer>()

    // rtc
    private val peerConnectionFactory = buildPeerConnectionFactory(context)
    private val localVideoSource = peerConnectionFactory.createVideoSource(false)
    private val localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
    private val videoCaptureManager: VideoCaptureManager = VideoCaptureManagerImpl.getVideoCapture(context)

    private val peerConnection by lazy { observer.buildPeerConnection() }

    // media stream
    private var surfaceTextureHelper : SurfaceTextureHelper? = null
    private var localVideoTrack : VideoTrack? = null
    private var localAudioTrack : AudioTrack? = null
    private var localStream : MediaStream? = null

    private val constraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_VIDEO,"true"))
        mandatory.add(MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_AUDIO, "true"))
        optional.add(MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT, "true"))
    }

    override fun initSurfaceView(surfaceViewRenderer: SurfaceViewRenderer) {
        surfaceViewRenderer.setMirror(true)
        surfaceViewRenderer.setEnableHardwareScaler(true)
        surfaceViewRenderer.init(rootEglBase.eglBaseContext, null)
    }

    override fun startLocalVideoCapture(localSurfaceView: SurfaceViewRenderer) {
        surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)
        localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, localAudioSource)
        localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)

        videoCaptureManager.videoCapturer?.let {
            it.initialize(surfaceTextureHelper, localSurfaceView.context, localVideoSource.capturerObserver)
            it.startCapture(240, 240, 60)
        }
        localVideoTrack?.addSink(localSurfaceView)
        localAudioTrack?.setEnabled(true)
        localStream?.addTrack(localAudioTrack)

        peerConnection?.addStream(localStream) ?: run {
            // TODO(에러 핸들링)
        }
    }

    override fun setIceServer(signalServerInfo: SignalServerInfo) {
        iceServer.clear()
        signalServerInfo.stunAndTurn.forEach { data ->
            iceServer.add(
                PeerConnection.IceServer.builder(data.urlList)
                    .setUsername(data.userName)
                    .setPassword(data.credential)
                    .createIceServer()
            )
        }
    }

    private fun initPeerConnectionFactory(context: Context) {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials(FIELD_TRIAL)
            .createInitializationOptions())
    }

    private fun buildPeerConnectionFactory(context: Context): PeerConnectionFactory {
        initPeerConnectionFactory(context)

        val factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(getVideoDecoderFactory())
            .setVideoEncoderFactory(getVideoEncoderFactory())
            .setOptions(getPeerConnectionOptions())
            .setAudioDeviceModule(audioModule)
            .createPeerConnectionFactory()

        audioModule.release()
        return factory
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

    private fun PeerConnection.Observer.buildPeerConnection(): PeerConnection? {
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

        return peerConnectionFactory.createPeerConnection(rtcConfig, this) ?: kotlin.run {
            // TODO(에러 핸들링)
            null
        }
    }

    override fun callOffer(sdpObserver: SdpObserver) {
        peerConnection?.createOffer(object: SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription( object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                        // TODO(에러 핸들링)
                    }
                    override fun onSetSuccess() {

                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }
                    override fun onCreateFailure(p0: String?) {
                        // TODO(에러 핸들링)
                    }
                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }
        }, constraints)
    }

    override fun callAnswer(sdpObserver: SdpObserver) {
        peerConnection?.createAnswer( object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription( object: SdpObserver {
                    override fun onSetFailure(p0: String?) {
                        // TODO(에러 핸들링)
                    }
                    override fun onSetSuccess() {

                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }
                    override fun onCreateFailure(p0: String?) {
                        // TODO(에러 핸들링)
                    }
                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }
        }, constraints)
    }

    override fun onRemoteSessionReceived(sdpObserver: SdpObserver, sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(sdpObserver, sessionDescription)
    }

    override fun addIceCandidate(iceCandidate: IceCandidate) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    override fun disconnectPeer() {
        peerConnection?.dispose()
        localAudioSource.dispose()
        videoCaptureManager.releaseVideoCapture { it.printStackTrace() }
        localVideoSource.dispose()
        surfaceTextureHelper?.dispose()
        peerConnectionFactory.dispose()
        rootEglBase.release()

        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }
}