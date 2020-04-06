package com.skfo763.rtcandroid_example.main

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.skfo763.rtc.core.AddSdpObserver
import com.skfo763.rtc.core.PeerConnectionObserver
import com.skfo763.rtc.core.RTCPeerClient
import com.skfo763.rtc.core.SignalingClient
import com.skfo763.rtc.data.ANSWER
import com.skfo763.rtc.data.CREATE_OFFER
import com.skfo763.rtc.data.OFFER
import com.skfo763.rtc.data.SessionDescriptionsType
import com.skfo763.rtc.utils.setLogDebug
import com.skfo763.rtc.views.CustomSurfaceViewRenderer
import com.skfo763.rtcandroid_example.VoiceExampleApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

class MainViewModel: ViewModel() {

    val coChannel = VoiceExampleApplication.coChannel
    lateinit var peerClient: RTCPeerClient
    lateinit var signalingClient: SignalingClient

    private val _progressStatus = MutableLiveData<Boolean>().apply { value = true }
    val progressStatus: LiveData<Boolean> get() = _progressStatus

    val receive = CoroutineScope(Dispatchers.Main).async {
        val receivedData = coChannel.channel.asFlow()
        receivedData.collect { data -> // 받은 아이들을 수집하여 그것을 진행한다.
            setLogDebug("receive Data : $data")
            when(data) {
                CREATE_OFFER -> {
                    setLogDebug("create offer")
                    peerClient.run{ sdpObserver.offer() }
                }
                is SessionDescriptionsType -> {
                    when( data.type ){
                        OFFER -> {
                            setLogDebug("get sd offer")
                            peerClient.run { onRemoteSessionReceived(data.description);  sdpObserver.answer() }
                            _progressStatus.postValue(false)
                        }
                        ANSWER -> {
                            peerClient.run { onRemoteSessionReceived(data.description) }
                            _progressStatus.postValue(false)
                        }
                    }
                }
                is IceCandidate -> {
                    peerClient.run { addIceCandidate(data) }
                }
            }
        }
    }

    val sdpObserver = object: AddSdpObserver() {
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
            p0?.let {
                setLogDebug("onCreateSuccess")
                signalingClient.socketOnListener?.sendRTCinfo(it)
            }
        }
    }

    fun onCameraPermissionGranted(application: Application) {
        peerClient = RTCPeerClient(
            application,
            object: PeerConnectionObserver() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    if(p0 == null) return
                    setLogDebug("onIceCandidate: $p0")
                    signalingClient.socketOnListener?.sendRTCinfo(p0)
                    peerClient.addIceCandidate(p0)
                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    coChannel.run { sendMediaStream(p0) }
                }
            }
        )

        peerClient.apply {
            coChannel.run {
                runMain { sendString("initView") }
            }
            signalingClient = SignalingClient()
        }
    }

    fun setInitRender(
        local: CustomSurfaceViewRenderer,
        remote: CustomSurfaceViewRenderer
    ) {
        peerClient.run {
            local.initSurfaceView()
            local.startLocalVideoCapture()

            remote.initSurfaceView()
        }
    }

    fun destroyPeerAndSocket() {
        signalingClient.destroy()
    }
}