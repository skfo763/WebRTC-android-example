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

class FaceChatRtcManager private constructor(
    private val context: Context,
    private val iFaceChatViewModelListener: IFaceChatViewModelListener
) : PeerConnectionObserver(), PeerSignalCallback {


    companion object {
        @JvmStatic
        fun createFaceChatRtcManager(
            context: Context,
            iFaceChatViewModelListener: IFaceChatViewModelListener
        ): FaceChatRtcManager {
            return FaceChatRtcManager(context, iFaceChatViewModelListener)
        }
    }

    /** for block Peer dispose duplicate : it is useful for hangup process **/
    private val isStart = AtomicBoolean(false)
    private val isReleased = AtomicBoolean(false)
    private var otherUserIdx: Int? = null

    private var socketManager: SocketManager? = null
    private var peerManager: VideoPeerManager? = null
    private val audioManager :MAudioManager by lazy { MAudioManagerImpl(context) }

    private var waitingLocalView: SurfaceViewRenderer? = null
    private var callingLocalView: SurfaceViewRenderer? = null
    private var remoteView: SurfaceViewRenderer? = null

    fun addWaitingLocalSurfaceView(localView: SurfaceViewRenderer) {
        this.waitingLocalView = localView
    }

    fun addCallingLocalView(localView: SurfaceViewRenderer) {
        this.callingLocalView = localView
    }

    fun addRemoteView(remoteView: SurfaceViewRenderer) {
        this.remoteView = remoteView
    }

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
        audioManager.audioFocusDucking()

        if(remoteView != null && mediaStream != null) {
            peerManager?.startRemoteVideoCapture(remoteView!!, mediaStream)
        }
    }

    override fun onPeerError(isCritical: Boolean, showMessage: Boolean, message: String?) {
        this.onError(isCritical, showMessage, message)
    }

    fun setPeerInfo(peer: SignalServerInfo) {
        peerManager = VideoPeerManager(context, this)
        peerManager?.setIceServer(peer)
    }

    fun startWaitingSurfaceRendering() {
        waitingLocalView?.let {
            peerManager?.initSurfaceView(it)
            peerManager?.startLocalVideoCapture(it)
        }
    }

    fun startCallingSurfaceRendering() {
        callingLocalView?.let {
            peerManager?.initSurfaceView(it)
            peerManager?.startLocalVideoCapture(it)
            peerManager?.startLocalVoice()
        }
        remoteView?.let {
            peerManager?.initSurfaceView(it)
        }
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
                    iFaceChatViewModelListener.onUiEvent(VoiceChatUiEvent.STOP_PROCESS_COMPLETE)
                }
            }
            StopCallType.QUIT_ACTIVITY -> {
                if(!shouldCloseSocket && isStart.get()) socketManager?.sendHangUpEventToSocket(HANGUP, stoppedAt)
                else releasePeerAndSocket {
                    iFaceChatViewModelListener.onUiEvent(VoiceChatUiEvent.FINISH)
                }
            }
        }
    }

    override fun onConnected(connectData: Array<Any>) {
        val userInfo = iFaceChatViewModelListener.callUserInfo()
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
        peerManager?.callOffer(appSdpObserver)
        socketManager?.sendCommonEventToSocket(CALL_STARTED)
        if(!isStart.get()) {
            iFaceChatViewModelListener.onUiEvent(VoiceChatUiEvent.START_CALL)
        }
        isStart.set(true)
    }

    override fun createMatchingAnswer() {
        peerManager?.callAnswer(appSdpObserver)
        socketManager?.sendCommonEventToSocket(CALL_STARTED)
        if(!isStart.get()) {
            iFaceChatViewModelListener.onUiEvent(VoiceChatUiEvent.START_CALL)
        }
        isStart.set(true)
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
            iFaceChatViewModelListener.sendTimerAndIdx(duration, otherIdx)
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
        iFaceChatViewModelListener.sendFinishInfo(displayRating, matchIdx)
    }

    override fun onHangUpSuccess(stoppedAt: StopCallType) {
        val uiEvent = when(stoppedAt) {
            StopCallType.GO_TO_STORE -> VoiceChatUiEvent.FINISH_WITH_STORE
            StopCallType.QUIT_ACTIVITY -> VoiceChatUiEvent.FINISH
            else -> VoiceChatUiEvent.STOP_PROCESS_COMPLETE
        }
        releasePeerAndSocket {
            iFaceChatViewModelListener.onUiEvent(uiEvent)
        }
    }

    override fun onTerminate(terminateState: String) {
        when(terminateState) {
            TIMEOUT, DISCONNECTION, HANGUP -> {
                releasePeerAndSocket {
                    iFaceChatViewModelListener.onUiEvent(VoiceChatUiEvent.STOP_PROCESS_COMPLETE)
                }
            }
            QUICK_REFUND -> {
                releasePeerAndSocket {
                    iFaceChatViewModelListener.onUiEvent(VoiceChatUiEvent.STOP_PROCESS_COMPLETE)
                    iFaceChatViewModelListener.onUiEvent(VoiceChatUiEvent.QUICK_REFUND)
                }
            }
            else ->  {
                iFaceChatViewModelListener.onError(ErrorHandleData(true, "", true))
            }
        }
    }

    override fun onError(isCritical: Boolean, showMessage: Boolean, message: String?) {
        if(isCritical) {
            iFaceChatViewModelListener.onError(ErrorHandleData(true, message ?: "", showMessage))
        }
        Log.d("RTC-Android", "message = $message, showMessage = $showMessage")
    }

    override fun onWaitingStatusReceived(data: JSONObject) {
        val waitingText = data.getString(WAITING_TEXT)
        iFaceChatViewModelListener.updateWaitInfo(waitingText)
    }

    private fun releasePeerAndSocket(doAfterRelease: (() -> Unit)? = null) {
        Handler(Looper.getMainLooper()).post {
            isStart.set(false)
            isReleased.set(true)

            releaseWaitingSurface()
            releaseCallingSurface()

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

    fun releaseWaitingSurface() {
        waitingLocalView?.run {
            release()
            waitingLocalView = null
        }
    }

    fun releaseCallingSurface() {
        callingLocalView?.run {
            release()
            callingLocalView = null
        }

        remoteView?.run {
            release()
            remoteView = null
        }
    }

}
