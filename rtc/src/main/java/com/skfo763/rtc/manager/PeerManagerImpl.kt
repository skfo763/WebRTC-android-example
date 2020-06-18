package com.skfo763.rtc.manager

import android.content.Context
import com.skfo763.rtc.data.*
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule

class PeerManagerImpl(
    observer: PeerConnection.Observer,
    surfaceViewRenderer: SurfaceViewRenderer,
    private val videoCaptureManager: VideoCaptureManager,
    private val signalServerInfo: SignalServerInfo,
    private val audioModule: AudioDeviceModule,
    private val context: Context
) : PeerManager {

    private val rootEglBase = EglBase.create()
    private val iceServer: List<PeerConnection.IceServer> = mutableListOf<PeerConnection.IceServer>().apply {
        signalServerInfo.stunAndTurn.forEach { data ->
            add(
                PeerConnection.IceServer.builder(data.urlList)
                    .setUsername(data.userName)
                    .setPassword(data.credential)
                    .createIceServer()
            )
        }
    }

    private val peerConnectionFactory =  buildPeerConnectionFactory()
    private val localVideoSource =  peerConnectionFactory.createVideoSource(false)
    private val localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
    private val peerConnection = observer.buildPeerConnection()

    private val surfaceTextureHelper : SurfaceTextureHelper
    private val localVideoTrack : VideoTrack
    private val localAudioTrack : AudioTrack
    private val localStream : MediaStream

    val constraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_VIDEO,"true"))
        mandatory.add(MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_AUDIO, "true"))
        optional.add(MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT, "true"))
    }

    init {
        surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)

        videoCaptureManager.videoCapturer?.initialize(surfaceTextureHelper, surfaceViewRenderer.context, localVideoSource.capturerObserver)
        videoCaptureManager.videoCapturer?.startCapture(240, 240, 60)

        localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)
        localVideoTrack.addSink(surfaceViewRenderer)

        localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, localAudioSource)
        localAudioTrack.setEnabled(true)

        localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)
        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)

        peerConnection?.addStream(localStream) ?: run {
            // TODO(에러 핸들링)
        }
    }

    private fun initPeerConnectionFactory(context: Context) {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials(FIELD_TRIAL)
            .createInitializationOptions())
    }

    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
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

    private val localDescriptionSdpObserver = object: SdpObserver {
        override fun onSetFailure(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun onSetSuccess() {
            TODO("Not yet implemented")
        }

        override fun onCreateSuccess(p0: SessionDescription?) {
            TODO("Not yet implemented")
        }

        override fun onCreateFailure(p0: String?) {
            TODO("Not yet implemented")
        }

    }

    override fun callOffer(sdpObserver: SdpObserver) {
        peerConnection?.createOffer(object: SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection.setLocalDescription( object : SdpObserver {
                    override fun onSetFailure(p0: String?) {

                    }

                    override fun onSetSuccess() {

                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onCreateFailure(p0: String?) {

                    }
                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }
        }, constraints)
    }

    override fun callAnswer(sdpObserver: SdpObserver) {
        peerConnection?.createAnswer( object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection.setLocalDescription( object: SdpObserver {
                    override fun onSetFailure(p0: String?) {

                    }
                    override fun onSetSuccess() {

                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }
                    override fun onCreateFailure(p0: String?) {

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

    override fun disconnect() {
        peerConnection?.dispose()
        localAudioSource.dispose()
        videoCaptureManager.releaseVideoCapture { it.printStackTrace() }
        localVideoSource.dispose()
        surfaceTextureHelper.dispose()
        peerConnectionFactory.dispose()
        rootEglBase.release()

        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }
}