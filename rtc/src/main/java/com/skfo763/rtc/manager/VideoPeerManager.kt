package com.skfo763.rtc.manager

import android.content.Context
import com.skfo763.rtc.data.PEER_CREATE_ERROR
import com.skfo763.rtc.data.VIDEO_TRACK_ID
import com.skfo763.rtc.inobs.PeerConnectionObserver
import org.webrtc.*

class VideoPeerManager(
        private val context: Context,
        private val observer: PeerConnectionObserver
) : VoicePeerManager(context, observer) {

    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private val localVideoSource by lazy {
        peerConnectionFactory.createVideoSource(false)
    }

    private val localVideoTrack by lazy {
        peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)
    }

    private var remoteVideoTrack: VideoTrack? = null

    private val videoCaptureManager = VideoCaptureManager.getVideoCapture(context, localVideoSource.capturerObserver)

    override fun PeerConnectionFactory.Builder.peerConnectionFactory(): PeerConnectionFactory.Builder {
        return this
    }

    override fun disconnectPeer() {
        localVideoSource.dispose()
        super.disconnectPeer()
    }

    fun initSurfaceView(surfaceViewRenderer: SurfaceViewRenderer) {
        surfaceViewRenderer.setEnableHardwareScaler(true)
        surfaceViewRenderer.init(rootEglBase.eglBaseContext, null)
    }

    fun startCameraCapture() {
        try {
            surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        } catch(e: Exception) {
            e.printStackTrace()
        } finally {
            surfaceTextureHelper?.let {
                videoCaptureManager.initialize(it, 960, 540, 30)
                localAudioTrack.setEnabled(true)
                localVideoTrack.setEnabled(true)
            }
        }
    }

    fun addTrackToStream() {
        localStream.addTrack(localAudioTrack)
        localStream.addTrack(localVideoTrack)
    }

    fun addStreamToPeerConnection() {
        peerConnection = buildPeerConnection()
        peerConnection?.addStream(localStream) ?: run {
            observer.onPeerError(isCritical = true, showMessage = false, message = PEER_CREATE_ERROR)
        }
    }

    fun attachLocalTrackToSurface(localSurfaceView: SurfaceViewRenderer) {
        localVideoTrack.addSink(localSurfaceView)
    }

    fun detachLocalTrackFromSurface(surfaceView: SurfaceViewRenderer) {
        localVideoTrack.removeSink(surfaceView)
    }

    fun removeStreamFromPeerConnection() {
        peerConnection?.removeStream(localStream) ?: run {
            observer.onPeerError(isCritical = false, showMessage = false, message = PEER_CREATE_ERROR)
        }
    }

    fun removeTrackFromStream() {
        localStream.removeTrack(localAudioTrack)
        localStream.removeTrack(localVideoTrack)
    }

    fun stopCameraCapture() {
        videoCaptureManager.stopVideoCapture {
            observer.onPeerError(isCritical = true, showMessage = false, message = PEER_CREATE_ERROR)
        }
        surfaceTextureHelper = null
    }

    fun changeCameraFacing(handler: CameraVideoCapturer.CameraSwitchHandler) {
        videoCaptureManager.changeCameraFacing(handler)
    }

    fun startRemoteVideoCapture(remoteSurfaceView: SurfaceViewRenderer, mediaStream: MediaStream) {
        mediaStream.videoTracks.getOrNull(0)?.apply {
            remoteSurfaceView.setMirror(true)
            remoteVideoTrack = this
            addSink(remoteSurfaceView)
            setEnabled(true)
        } ?: run {
            observer.onPeerError(true, showMessage = false, message = PEER_CREATE_ERROR)
        }
    }

    fun stopRemotePreviewRendering(surfaceView: SurfaceViewRenderer) {
        remoteVideoTrack?.removeSink(surfaceView)
    }

}