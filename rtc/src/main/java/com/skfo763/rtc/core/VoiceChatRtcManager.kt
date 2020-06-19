package com.skfo763.rtc.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.skfo763.rtc.contracts.PeerSignalCallback
import com.skfo763.rtc.contracts.RtcModuleInterface
import com.skfo763.rtc.contracts.RtcViewInterface
import com.skfo763.rtc.contracts.StopCallType
import com.skfo763.rtc.data.*
import com.skfo763.rtc.inobs.AppSdpObserver
import com.skfo763.rtc.inobs.PeerConnectionObserver
import com.skfo763.rtc.manager.*
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.audio.AudioDeviceModule
import java.util.concurrent.atomic.AtomicBoolean

class VoiceChatRtcManager private constructor(
    context: Context,
    audioDeviceModule: AudioDeviceModule,
    localView: SurfaceViewRenderer,
    remoteView: SurfaceViewRenderer,
    private val rtcViewInterface: RtcViewInterface
): PeerConnectionObserver(), PeerSignalCallback, RtcModuleInterface {

    companion object {
        @JvmStatic
        fun initVoiceChatRtcManager(
            context: Context,
            audioDeviceModule: AudioDeviceModule,
            localView: SurfaceViewRenderer,
            remoteView: SurfaceViewRenderer,
            rtcViewInterface: RtcViewInterface
        ): RtcModuleInterface {
            return VoiceChatRtcManager(context, audioDeviceModule,
                localView, remoteView, rtcViewInterface)
        }
    }

    /** for block Peer dispose duplicate : it is useful for hangup process **/
    private var isStart = AtomicBoolean(false)
    private var otherUserIdx: Int? = null

    private val socketManager: SocketManager = SocketManagerImpl(this)
    private val peerManager: PeerManager = PeerManagerImpl(context, this, audioDeviceModule)
    private val audioManager: MAudioManager = MAudioManagerImpl(context)

    private var localSurfaceView = localView
    private var remoteSurfaceView = remoteView

    private val appSdpObserver = object: AppSdpObserver() {
        override fun onCreateSuccess(desc: SessionDescription?) {
            super.onCreateSuccess(desc)
            desc?.let {
                socketManager.sendOfferAnswerToSocket(it)
            } ?: kotlin.run {
                onError(isCritical = false, showMessage = false, message = "Session description data is null")
            }
        }
    }

    override fun onIceCandidate(iceCandidate: IceCandidate?) {
        super.onIceCandidate(iceCandidate)
        iceCandidate?.let {
            socketManager.sendIceCandidateToSocket(it)
            peerManager.addIceCandidate(it)
        } ?: kotlin.run {
            onError(isCritical = false, showMessage = false, message = "Ice candidate data is null")
        }
    }

    override fun onAddStream(mediaStream: MediaStream?) {
        super.onAddStream(mediaStream)
        audioManager.audioFocusDucking()

        /*  TODO(remoteSurfaceView 초기화 필요)
        remoteSurfaceview?.let {
            var vTracks =  mStream?.videoTracks?.get(0)
            var aTracks = mStream?.audioTracks?.get(0)
            vTracks?.run {
                addSink(it)
                setEnabled(false)
            }
        } ?: kotlin.run {
            //  TODO : remoteSurfaceview add sink after INITIALIZE
            // remoteSurfaceView 가 null 이면 해당 뷰를 초기화 해주고 싱크해주기
            peerManager?.run {
                remoteSurfaceview = remoteView.apply { initSurfaceView() }
                var vTracks =  mStream?.videoTracks?.get(0)
                vTracks?.run{
                    addSink(remoteSurfaceview)
                    setEnabled(false)
                }
            }
        }
        */
    }

    override fun setLocalSurfaceView(local: SurfaceViewRenderer) {
        this.localSurfaceView = local
    }

    override fun setRemoteSurfaceView(remote: SurfaceViewRenderer) {
        this.remoteSurfaceView = remote
    }

    override fun setPeerInfo(peer: SignalServerInfo) {
        peerManager.setIceServer(peer)
        peerManager.initSurfaceView(localSurfaceView)
        peerManager.startLocalVideoCapture(localSurfaceView)
        peerManager.initSurfaceView(remoteSurfaceView)
    }

    override fun startWaiting(peer: SignalServerInfo) {
        socketManager.initializeSocket(peer.signalServerHost)
    }

    override fun stopCallSignFromClient(stoppedAt: StopCallType) {
        if(!isStart.get()) releasePeerAndSocket()
        when(stoppedAt) {
            StopCallType.POWER_DESTROY -> releasePeerAndSocket()
            StopCallType.AT_FRAGMENT -> socketManager.sendHangUpEventToSocket(HANGUP)
            StopCallType.GO_TO_STORE -> socketManager.sendHangUpEventToSocket(HANGUP)
            StopCallType.QUIT_ACTIVITY -> releasePeerAndSocket {
                rtcViewInterface.finishActivity()
            }
        }
    }

    override fun onConnected(connectData: Array<Any>) {
        val userInfo = rtcViewInterface.getUserInfo()
        val authInfo = JSONObject().apply {
            put(TOKEN, userInfo.token)
            put(PASSWORD, userInfo.password)
            if(isStart.get()) {
                put(STATUS, MATCHED)
                put(OTHER, otherUserIdx)
            }
        }
        socketManager.sendJoinToSocket(authInfo)
    }

    override fun createMatchingOffer() {
        isStart.set(true)
        peerManager.callOffer(appSdpObserver)
        rtcViewInterface.startCall()
        socketManager.sendCommonEventToSocket(CALL_STARTED)
    }

    override fun createMatchingAnswer() {
        isStart.set(true)
        peerManager.callAnswer(appSdpObserver)
        rtcViewInterface.startCall()
        socketManager.sendCommonEventToSocket(CALL_STARTED)
    }

    override fun onOfferReceived(description: SessionDescription) {
        peerManager.onRemoteSessionReceived(appSdpObserver, description)
        peerManager.callAnswer(appSdpObserver)
    }

    override fun onAnswerReceived(description: SessionDescription) {
        peerManager.onRemoteSessionReceived(appSdpObserver, description)
    }

    override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
        peerManager.addIceCandidate(iceCandidate)
    }

    override fun onMatched(data: JSONObject) {
        try {
            val isOffer = data.getBoolean(OFFER)
            val otherIdx = data.getInt(OTHER_IDX)
            val duration = data.get(DURATION_SECOND)
            rtcViewInterface.sendTimerAndIdx(duration, otherIdx)
            this.otherUserIdx = otherIdx

            if(isOffer)  createMatchingOffer()
            else createMatchingAnswer()
        } catch (e: Exception) {
            onError(true, message = e.message)
        }
    }

    override fun onHangUp(data: JSONObject) {
        val displayRating = data.optBoolean(DISPLAY_RATING)
        val matchIdx = data.optInt(MATCH_IDX)
        rtcViewInterface.sendFinishInfo(displayRating, matchIdx)
    }

    override fun onHangUpSuccess() {
        releasePeerAndSocket {
            rtcViewInterface.stopCall()
        }
    }

    override fun onTerminate(terminateState: String) {
        when(terminateState) {
            TIMEOUT -> {
                releasePeerAndSocket {
                    rtcViewInterface.stopCall()
                }
            }
            DISCONNECTION -> {
                releasePeerAndSocket {
                    rtcViewInterface.stopCall()
                }
            }
            QUICK_REFUND -> {
                releasePeerAndSocket {
                    rtcViewInterface.stopCall()
                    rtcViewInterface.refundCandy()
                }
            }
            else ->  { /* TODO(에러 핸들링) */ }
        }
    }

    override fun onError(isCritical: Boolean, showMessage: Boolean, message: String?) {
        if(isCritical) {
            rtcViewInterface.handleError(showMessage, message)
        } else {
            Log.e("RTC-Android", "message = $message, showMessage = $showMessage")
        }
    }

    override fun onWaitingStatusReceived(data: JSONObject) {
        val waitingText = data.getString(WAITING_TEXT)
        rtcViewInterface.updateWaitInfo(waitingText)
    }

    private fun releasePeerAndSocket(doAfterRelease: (() -> Unit)? = null) {
        Handler(Looper.getMainLooper()).post {
            isStart.set(false)
            audioManager.audioFocusLoss()
            localSurfaceView.release()
            remoteSurfaceView.release()
            socketManager.disconnectSocket()
            peerManager.disconnectPeer()
            doAfterRelease?.invoke()
        }
    }
}