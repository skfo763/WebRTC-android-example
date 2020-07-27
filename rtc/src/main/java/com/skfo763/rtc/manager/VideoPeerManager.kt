package com.skfo763.rtc.manager

import android.content.Context
import com.skfo763.rtc.data.VIDEO_TRACK_ID
import com.skfo763.rtc.inobs.PeerConnectionObserver
import org.webrtc.PeerConnectionFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer

class VideoPeerManager(context: Context, private val observer: PeerConnectionObserver) : VoicePeerManager(context, observer) {

    private lateinit var surfaceTextureHelper: SurfaceTextureHelper

    private val localVideoSource by lazy {
        peerConnectionFactory.createVideoSource(false)
    }

    private val localVideoTrack by lazy {
        peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)
    }

    private val videoCaptureManager = VideoCaptureManagerImpl.getVideoCapture(context)

    override fun PeerConnectionFactory.Builder.peerConnectionFactory(): PeerConnectionFactory.Builder {
        return this
    }

    override fun initSurfaceView(surfaceViewRenderer: SurfaceViewRenderer) {
        surfaceViewRenderer.setMirror(true)
        surfaceViewRenderer.setEnableHardwareScaler(true)
        surfaceViewRenderer.init(rootEglBase.eglBaseContext, null)
    }

    override fun startLocalVideoCapture(localSurfaceView: SurfaceViewRenderer) {
        surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        videoCaptureManager.videoCapturer?.let {
            it.initialize(surfaceTextureHelper, localSurfaceView.context, localVideoSource.capturerObserver)
            it.startCapture(240, 240, 60)
        }
        localVideoTrack.addSink(localSurfaceView)
        startLocalVoice()
    }

    override fun startRemoteVideoCapture(localSurfaceView: SurfaceViewRenderer) {

    }

    override fun disconnectPeer() {
        super.disconnectPeer()
        localVideoSource.dispose()
        surfaceTextureHelper.dispose()
        videoCaptureManager.stopVideoCapture { it.printStackTrace() }
    }

}