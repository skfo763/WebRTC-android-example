package com.skfo763.rtcandroid_example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skfo763.rtc.contracts.ErrorHandleData
import com.skfo763.rtc.contracts.IVideoChatViewModelListener
import com.skfo763.rtc.contracts.RtcUiEvent
import com.skfo763.rtc.core.VideoChatRtcManager
import com.skfo763.rtc.data.MatchModel
import com.skfo763.rtc.data.SignalServerInfo
import com.skfo763.rtc.data.UserJoinInfo
import com.skfo763.rtcandroid_example.*
import com.skfo763.rtcandroid_example.base.FragmentType
import com.skfo763.rtcandroid_example.base.MainActivityUseCase
import com.skfo763.rtcandroid_example.base.MessageType
import com.skfo763.rtcandroid_example.utils.TokenManager
import org.webrtc.CameraVideoCapturer

class MainViewModel(val rtcModule: VideoChatRtcManager, val useCase: MainActivityUseCase) : ViewModel(), IVideoChatViewModelListener {

    init {
        rtcModule.iVideoChatViewModelListener = this
    }

    private val _fragmentType = MutableLiveData<Pair<FragmentType, Boolean>>()
    private val _canScrollPager = MutableLiveData<Boolean>()
    private val _isLoading = MutableLiveData<Boolean>()
    private val _isFrontCamera = MutableLiveData<Boolean>(true)
    private val _matchData = MutableLiveData<MatchModel>()
    private val _message = MutableLiveData<Pair<MessageType, String?>>()


    val fragmentType: LiveData<Pair<FragmentType, Boolean>> = _fragmentType
    val canScrollPager: LiveData<Boolean> = _canScrollPager
    val isLoading: LiveData<Boolean> = _isLoading
    val isFrontCamera : LiveData<Boolean> = _isFrontCamera
    val matchData: LiveData<MatchModel>  = _matchData
    val message: LiveData<Pair<MessageType, String?>> = _message

    fun initRtcModule() {
        rtcModule.initializeVideoTrack()
        rtcModule.startCameraCapturer()
        rtcModule.setPeerInfo(SignalServerInfo())
    }

    fun onBackButtonClicked() {
        useCase.finishActivity()
    }

    fun onSwitchCameraButtonClicked() {
        _isLoading.value = true
        rtcModule.changeCameraFacing(object: CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(p0: Boolean) {
                _isFrontCamera.postValue(p0)
                _isLoading.postValue(false)
            }
            override fun onCameraSwitchError(p0: String?) {
                Log.e(this@MainViewModel::class.simpleName, p0 ?: "unknown error")
                _isLoading.postValue(false)
            }
        })
    }

    override fun onMatched(data: MatchModel) {
        _matchData.postValue(data)
    }

    override fun updateWaitInfo(text: String) {
        useCase.showToast(text)
    }

    override fun onUiEvent(uiEvent: RtcUiEvent, message: String?) {
        when(uiEvent) {
            RtcUiEvent.START_CALL -> {
                useCase.showTopBanner(R.string.face_chat_matching_success, R.color.green_64dd17)
                useCase.vibrate(200)
                _fragmentType.postValue(FragmentType.TYPE_CALLING to false)
            }
            RtcUiEvent.STOP_PROCESS_COMPLETE -> {
                message?.let { useCase.showTopBanner(it, R.color.pink_ff5a78) }
                _fragmentType.postValue(FragmentType.TYPE_WAITING to true)
            }
            RtcUiEvent.FINISH -> {
                useCase.finishActivity()
            }
            RtcUiEvent.RETRY -> {
                useCase.showTopBanner(R.string.voice_chat_disconnect_from_server, R.color.pink_ff5a78)
            }
        }
    }

    override fun onError(e: ErrorHandleData) {
        if(e.isCritical && e.showMsg) {
            _message.postValue(MessageType.RTC_ERROR to e.message)
        }
    }

    override fun getUserInfo() = UserJoinInfo(
        useCase.token,
        TokenManager.DEFAULT_PASSWORD
    )

    class Factory (
        private val context: Context,
        private val useCase: MainActivityUseCase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(VideoChatRtcManager.createFaceChatRtcManager(context), useCase) as T
        }
    }
}

