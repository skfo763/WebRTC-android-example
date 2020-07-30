package com.skfo763.rtc.manager

import android.content.Context
import android.util.Log
import com.skfo763.rtc.data.PEER_CREATE_ERROR
import com.skfo763.rtc.data.VIDEO_TRACK_ID
import com.skfo763.rtc.inobs.PeerConnectionObserver
import org.webrtc.*
import java.lang.Exception

class VideoPeerManager(private val context: Context, private val observer: PeerConnectionObserver) : VoicePeerManager(context, observer) {

    private var surfaceTextureHelper: SurfaceTextureHelper? = null


    private val localVideoSource = peerConnectionFactory.createVideoSource(true)
    private val localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)

    private var videoCaptureManager: VideoCaptureManager? = null

    override fun PeerConnectionFactory.Builder.peerConnectionFactory(): PeerConnectionFactory.Builder {
        return this
    }

    override fun initSurfaceView(surfaceViewRenderer: SurfaceViewRenderer) {
        super.initSurfaceView(surfaceViewRenderer)
        surfaceViewRenderer.setMirror(true)
        surfaceViewRenderer.setEnableHardwareScaler(true)
        surfaceViewRenderer.init(rootEglBase.eglBaseContext, null)
    }

    override fun startLocalVideoCapture(localSurfaceView: SurfaceViewRenderer) {
        super.startLocalVideoCapture(localSurfaceView)

        surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        videoCaptureManager = VideoCaptureManagerImpl.getVideoCapture(context)
        videoCaptureManager?.videoCapturer?.stopCapture()
        videoCaptureManager?.videoCapturer?.dispose()

        videoCaptureManager?.videoCapturer?.let {
            it.initialize(surfaceTextureHelper, localSurfaceView.context, localVideoSource.capturerObserver)
            it.startCapture(720, 720, 60)
        }
    }

    fun startSurfaceRtc(localSurfaceView: SurfaceViewRenderer) {
        localVideoTrack.addSink(localSurfaceView)
        localVideoTrack.setEnabled(true)
        startLocalVoice()
    }

    override fun startRemoteVideoCapture(
        remoteSurfaceView: SurfaceViewRenderer,
        mediaStream: MediaStream
    ) {
        super.startRemoteVideoCapture(remoteSurfaceView, mediaStream)
        val videoTrack = mediaStream.videoTracks.getOrNull(0)
        val audioTrack = mediaStream.audioTracks.getOrNull(0)

        if(videoTrack == null || audioTrack == null) {
            observer.onPeerError(true, showMessage = false, message = PEER_CREATE_ERROR)
        }
        videoTrack?.addSink(remoteSurfaceView)
        videoTrack?.setEnabled(true)
    }

    override fun disconnectPeer() {
        super.disconnectPeer()
        localVideoSource?.dispose()
        surfaceTextureHelper?.dispose()
        videoCaptureManager?.stopVideoCapture { it.printStackTrace() }
    }

}