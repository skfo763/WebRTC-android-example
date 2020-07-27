package com.skfo763.rtc.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.skfo763.rtc.contracts.*
import com.skfo763.rtc.data.*
import com.skfo763.rtc.inobs.AppSdpObserver
import com.skfo763.rtc.inobs.PeerConnectionObserver
import com.skfo763.rtc.manager.*
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import java.util.concurrent.atomic.AtomicBoolean

class VoiceChatRtcManager private constructor(
    private val context: Context,
    localView: SurfaceViewRenderer,
    remoteView: SurfaceViewRenderer
) : PeerConnectionObserver(), PeerSignalCallback {

    companion object {
        @JvmStatic
        fun createVoiceChatRtcManager(
            context: Context,
            localView: SurfaceViewRenderer,
            remoteView: SurfaceViewRenderer
        ): VoiceChatRtcManager {
            return VoiceChatRtcManager(context, localView, remoteView)
        }
    }

    /** for block Peer dispose duplicate : it is useful for hangup process **/
    private val isStart = AtomicBoolean(false)
    private val isReleased = AtomicBoolean(false)
    private var otherUserIdx: Int? = null

    private var socketManager: SocketManager? = null
    private var peerManager: PeerManager? = null
    private val audioManager :MAudioManager by lazy { MAudioManagerImpl(context) }

    private var localSurfaceView: SurfaceViewRenderer? = localView
    private var remoteSurfaceView: SurfaceViewRenderer? = remoteView

    lateinit var iVoiceChatViewModelListener: IVoiceChatViewModelListener

    private val appSdpObserver = object: AppSdpObserver() {
        override fun onCreateSuccess(desc: SessionDescription?) {
            super.onCreateSuccess(desc)
            desc?.let {
                socketManager?.sendOfferAnswerToSocket(it)
            } ?: kotlin.run {
                onPeerError(isCritical = false, showMessage = false, message = "Session description data is null")
            }
        }
    }

    override fun onIceCandidate(iceCandidate: IceCandidate?) {
        super.onIceCandidate(iceCandidate)
        iceCandidate?.let {
            socketManager?.sendIceCandidateToSocket(it)
            peerManager?.addIceCandidate(it)
        } ?: kotlin.run {
            onPeerError(isCritical = false, showMessage = false, message = "Ice candidate data is null")
        }
    }

    override fun onAddStream(mediaStream: MediaStream?) {
        super.onAddStream(mediaStream)
        audioManager?.audioFocusDucking()
    }

    override fun onPeerError(isCritical: Boolean, showMessage: Boolean, message: String?) {
        this@VoiceChatRtcManager.onError(isCritical, showMessage, message)
    }

    fun setPeerInfo(peer: SignalServerInfo) {
        peerManager = VoicePeerManager(context, this)
        peerManager?.setIceServer(peer)
        peerManager?.startLocalVoice()
    }

    fun startWaiting(peer: SignalServerInfo) {
        socketManager = SocketManagerImpl(this)
        socketManager?.initializeSocket(peer.signalServerHost)
    }

    fun stopCallSignFromClient(stoppedAt: StopCallType, shouldCloseSocket: Boolean) {
        when(stoppedAt) {
            StopCallType.POWER_DESTROY -> releasePeerAndSocket()
            StopCallType.GO_TO_STORE -> socketManager?.sendHangUpEventToSocket(HANGUP, stoppedAt)
            StopCallType.AT_FRAGMENT -> {
                if(!shouldCloseSocket && isStart.get()) socketManager?.sendHangUpEventToSocket(HANGUP, stoppedAt)
                else releasePeerAndSocket {
                    iVoiceChatViewModelListener.onUiEvent(VoiceChatUiEvent.STOP_PROCESS_COMPLETE)
                }
            }
            StopCallType.QUIT_ACTIVITY -> {
                if(!shouldCloseSocket && isStart.get()) socketManager?.sendHangUpEventToSocket(HANGUP, stoppedAt)
                else releasePeerAndSocket {
                    iVoiceChatViewModelListener.onUiEvent(VoiceChatUiEvent.FINISH)
                }
            }
        }
    }

    override fun onConnected(connectData: Array<Any>) {
        val userInfo = iVoiceChatViewModelListener.callUserInfo()
        val authInfo = JSONObject().apply {
            put(TOKEN, userInfo.token)
            put(PASSWORD, userInfo.password)
            put(BLNID_MODE, userInfo.isBlindMode)
            if(isStart.get()) {
                put(STATUS, MATCHED)
                put(OTHER, otherUserIdx)
            }
        }
        socketManager?.sendJoinToSocket(authInfo)
    }

    override fun createMatchingOffer() {
        isStart.set(true)
        peerManager?.callOffer(appSdpObserver)
        iVoiceChatViewModelListener.onUiEvent(VoiceChatUiEvent.START_CALL)
        socketManager?.sendCommonEventToSocket(CALL_STARTED)
    }

    override fun createMatchingAnswer() {
        isStart.set(true)
        peerManager?.callAnswer(appSdpObserver)
        iVoiceChatViewModelListener.onUiEvent(VoiceChatUiEvent.START_CALL)
        socketManager?.sendCommonEventToSocket(CALL_STARTED)
    }

    override fun onOfferReceived(description: SessionDescription) {
        peerManager?.run {
            onRemoteSessionReceived(appSdpObserver, description)
            callAnswer(appSdpObserver)
        }
    }

    override fun onAnswerReceived(description: SessionDescription) {
        peerManager?.onRemoteSessionReceived(appSdpObserver, description)
    }

    override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
        peerManager?.addIceCandidate(iceCandidate)
    }

    override fun onMatched(data: JSONObject) {
        try {
            val isOffer = data.getBoolean(OFFER)
            val otherIdx = data.getInt(OTHER_IDX)
            val duration = data.getInt(DURATION_SECOND)
            iVoiceChatViewModelListener.sendTimerAndIdx(duration, otherIdx)
            this.otherUserIdx = otherIdx

            if(isOffer) createMatchingOffer()
            else createMatchingAnswer()
        } catch (e: Exception) {
            onPeerError(true, message = e.message)
        }
    }

    override fun onHangUp(data: JSONObject) {
        val displayRating = data.optBoolean(DISPLAY_RATING)
        val matchIdx = data.optInt(MATCH_IDX)
        iVoiceChatViewModelListener.sendFinishInfo(displayRating, matchIdx)
    }

    override fun onHangUpSuccess(stoppedAt: StopCallType) {
        val uiEvent = when(stoppedAt) {
            StopCallType.GO_TO_STORE -> VoiceChatUiEvent.FINISH_WITH_STORE
            StopCallType.QUIT_ACTIVITY -> VoiceChatUiEvent.FINISH
            else -> VoiceChatUiEvent.STOP_PROCESS_COMPLETE
        }
        releasePeerAndSocket {
            iVoiceChatViewModelListener.onUiEvent(uiEvent)
        }
    }

    override fun onTerminate(terminateState: String) {
        when(terminateState) {
            TIMEOUT, DISCONNECTION, HANGUP -> {
                releasePeerAndSocket {
                    iVoiceChatViewModelListener.onUiEvent(VoiceChatUiEvent.STOP_PROCESS_COMPLETE)
                }
            }
            QUICK_REFUND -> {
                releasePeerAndSocket {
                    iVoiceChatViewModelListener.onUiEvent(VoiceChatUiEvent.STOP_PROCESS_COMPLETE)
                    iVoiceChatViewModelListener.onUiEvent(VoiceChatUiEvent.QUICK_REFUND)
                }
            }
            else ->  {
                iVoiceChatViewModelListener.onError(ErrorHandleData(true, "", true))
            }
        }
    }

    override fun onError(isCritical: Boolean, showMessage: Boolean, message: String?) {
        if(isCritical) {
            iVoiceChatViewModelListener.onError(ErrorHandleData(true, message ?: "", showMessage))
        }
        Log.d("RTC-Android", "message = $message, showMessage = $showMessage")
    }

    override fun onWaitingStatusReceived(data: JSONObject) {
        val waitingText = data.getString(WAITING_TEXT)
        iVoiceChatViewModelListener.updateWaitInfo(waitingText)
    }

    private fun releasePeerAndSocket(doAfterRelease: (() -> Unit)? = null) {
        // if(isReleased.get()) return
        Handler(Looper.getMainLooper()).post {
            isStart.set(false)
            isReleased.set(true)

            localSurfaceView?.run {
                release()
                localSurfaceView = null
            }

            remoteSurfaceView?.run {
                release()
                remoteSurfaceView = null
            }

            socketManager?.run {
                otherUserIdx = null
                disconnectSocket()
                socketManager = null
            }

            peerManager?.run {
                disconnectPeer()
                peerManager = null
            }

            audioManager?.audioFocusLoss()

            doAfterRelease?.invoke()
        }
    }
}