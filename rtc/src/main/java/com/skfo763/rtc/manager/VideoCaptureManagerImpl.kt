package com.skfo763.rtc.manager

import android.content.Context
import android.util.Log
import com.skfo763.rtc.data.VIDEOCAPTURER_NULL
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.VideoCapturer

class VideoCaptureManagerImpl private constructor(
        override val videoCapturer: VideoCapturer?
) : VideoCaptureManager {

    companion object {
        @JvmStatic
        fun getVideoCapture(context: Context): VideoCaptureManager {
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
            return VideoCaptureManagerImpl(videoCapturer as? VideoCapturer)
        }
    }

    override fun stopVideoCapture(doOnError: ((e: Exception) -> Unit)?) {
        try {
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            doOnError?.invoke(e)
        } finally {
            videoCapturer?.dispose()
        }
    }

}