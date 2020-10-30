package com.skfo763.rtc.manager

import android.content.Context
import android.util.Log
import org.webrtc.*

class VideoCaptureManager(
        private val videoCapturer: VideoCapturer?,
        private val context: Context,
        private val captureObserver: CapturerObserver
) {

    companion object {
        @JvmStatic
        fun getVideoCapture(context: Context, observer: CapturerObserver): VideoCaptureManager {
            val videoCapturer = if (Camera2Enumerator.isSupported(context)) {
                Camera2Enumerator(context)
            } else {
                Camera1Enumerator(false)
            }.run {
                deviceNames.find { deviceName ->
                    isFrontFacing(deviceName)
                }?.let {
                    createCapturer(it, null)
                }
            }
            return VideoCaptureManager(videoCapturer, context, observer)
        }
    }

    private var isInitialized = false

    fun initialize(textureHelper: SurfaceTextureHelper, width: Int, height: Int, frameRate: Int) {
        videoCapturer?.let {
            try {
                if(!isInitialized) {
                    videoCapturer.initialize(textureHelper, context, captureObserver)
                    isInitialized = true
                }
                videoCapturer.startCapture(width, height, frameRate)
            } catch(e: RuntimeException) {
                isInitialized = false
                Log.e("webrtcTAG", e.message ?: "error")
            } catch(e: Exception) {
                isInitialized = false
                Log.e("webrtcTAG", e.message ?: "error")
            }
        } ?: run {
            isInitialized = false
            Log.e("webrtcTAG", "videoCapturer is null")
        }
    }

    fun changeCameraFacing(handler: CameraVideoCapturer.CameraSwitchHandler) {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(handler)
    }

    fun stopVideoCapture(doOnError: ((e: Exception) -> Unit)?) {
        try {
            isInitialized = false
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            doOnError?.invoke(e)
        } finally {
            videoCapturer?.dispose()
        }
    }

}