package com.skfo763.rtc.manager

import android.content.Context
import com.skfo763.rtc.data.VIDEOCAPTURER_NULL
import org.webrtc.*
import java.lang.Exception

class VideoCaptureManagerImpl private constructor(
    override val videoCapturer: VideoCapturer?
): VideoCaptureManager {

    companion object {
        @JvmStatic
        fun getVideoCapture(context: Context, onError: ((errorMessage: String) -> Unit)? = null): VideoCaptureManager {
            val cameraEnumerator = if(Camera2Enumerator.isSupported(context)) {
                Camera2Enumerator(context)
            } else {
                Camera1Enumerator(false)
            }.apply {
                deviceNames.find { deviceName ->
                    isFrontFacing(deviceName)
                }?.let {
                    createCapturer(it, null)
                } ?: kotlin.run {
                    onError?.invoke(VIDEOCAPTURER_NULL)
                }
            }
            return VideoCaptureManagerImpl(cameraEnumerator as? VideoCapturer)
        }
    }

    override fun releaseVideoCapture(doOnError: ((e: Exception) -> Unit)?) {
        try {
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            doOnError?.invoke(e)
        } finally {
            videoCapturer?.dispose()
        }
    }
}