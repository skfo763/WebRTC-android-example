package com.skfo763.rtc.core

import android.app.Application
import android.content.Context
import android.util.Log
import com.skfo763.rtc.RTCApp
import com.skfo763.rtc.data.LOG_TAG
import com.skfo763.rtc.utils.setLogDebug
import org.webrtc.*

open class RTCPeerClient(context: Application, observer: PeerConnection.Observer) {

    companion object {
        const val LOCAL_STREAM_ID = "ARDAMSs0"
        const val VIDEO_TRACK_ID = "ARDAMSv0"
        const val AUDIO_TRACK_ID = "ARDAMSa0"
    }

    // coroutines channel
    val channel = RTCApp.coChannel.channel

    // eglBase
    private val rootEglbase : EglBase = EglBase.create()

    // ice Candidate stun server
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
            .createIceServer()
    )

    // rtc
    private val peerConnectionFactory by lazy { buildPeerConnectionFactory() }
    private val videoCapture by lazy { context.getVideoCapture() }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy {
        peerConnectionFactory.createAudioSource(MediaConstraints())
    }
    private val peerConnection: PeerConnection? by lazy { observer.buildPeerConnection() }

    private val constraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
        mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
        optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
    }

    init {
        context.initPeerConnectionFactory()
    }

    private fun Application.initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(this)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglbase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    rootEglbase.eglBaseContext,
                    true,
                    true
                )
            ).setOptions(
                PeerConnectionFactory.Options().apply {
                    networkIgnoreMask = 0
                }
            ).createPeerConnectionFactory()
    }

    private fun PeerConnection.Observer.buildPeerConnection(): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServer).apply {
            enableDtlsSrtp = true
        }
        return peerConnectionFactory.createPeerConnection(rtcConfig, this)
    }

    fun SurfaceViewRenderer.initSurfaceView() = this.run {
        setMirror(true)
        setEnableHardwareScaler(true)
        init(rootEglbase.eglBaseContext, null)
    }

    private fun Context.getVideoCapture() =
        Camera2Enumerator(this).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }

    fun SurfaceViewRenderer.startLocalVideoCapture() {
        val surfaceTextureHelpder = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglbase.eglBaseContext)
        (videoCapture as VideoCapturer).initialize(surfaceTextureHelpder, this.context, localVideoSource.capturerObserver)
        videoCapture.startCapture(240, 240, 60)

        val localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)
        localVideoTrack.addSink(this)

        val localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, localAudioSource)
        localAudioTrack.setEnabled(true)

        val localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)
        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)

        peerConnection?.addStream(localStream)
    }

    private fun PeerConnection.offer(sdpObserver: SdpObserver) {
        createOffer(object: SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                setLocalDescription(object: SdpObserver {
                    override fun onSetFailure(p0: String?) {
                        Log.d(LOG_TAG, "call:setFailure : $p0")
                    }

                    override fun onSetSuccess() {
                        Log.d(LOG_TAG,"call:setSuccess : ")
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {
                        Log.d(LOG_TAG, "call:onCreateSuccess : $p0")
                    }

                    override fun onCreateFailure(p0: String?) {
                        Log.d(LOG_TAG,"call:onCreateFailure : $p0")
                    }
                }, desc)
                sdpObserver.onCreateSuccess(desc)
            }
        }, constraints)
    }

    private fun PeerConnection.answer(sdpObserver: SdpObserver) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
            optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        }

        createAnswer(object: SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                setLocalDescription(object: SdpObserver {
                    override fun onSetFailure(p0: String?) {
                        Log.d(LOG_TAG, "call:setFailure : $p0")
                    }

                    override fun onSetSuccess() {
                        Log.d(LOG_TAG,"call:setSuccess : ")
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {
                        Log.d(LOG_TAG, "call:onCreateSuccess : $p0")
                    }

                    override fun onCreateFailure(p0: String?) {
                        Log.d(LOG_TAG,"call:onCreateFailure : $p0")
                    }
                }, desc)
            }
        }, constraints)
    }

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(object: SdpObserver {
            override fun onSetFailure(p0: String?) {
                setLogDebug("remote:onSetFailure : $p0")
            }

            override fun onSetSuccess() {
                setLogDebug("remote:onSetSuccess")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {
                setLogDebug("remote:onCreateSuccess: $p0")
            }

            override fun onCreateFailure(p0: String?) {
                setLogDebug("remote:onCreateFailure : $p0")
            }
        }, sessionDescription)
    }

    fun SdpObserver.offer() = peerConnection?.run { offer(this@offer) }
    fun SdpObserver.answer() = peerConnection?. run { answer(this@answer) }

    fun addIceCandidate(iceCandidate: IceCandidate?) {
        if(iceCandidate == null) return
        setLogDebug("addIceCandidate ====> $iceCandidate")
        peerConnection?.addIceCandidate(iceCandidate)
    }
}