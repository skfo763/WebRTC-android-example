package com.skfo763.rtcandroid_example.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skfo763.rtcandroid_example.R
import com.skfo763.rtcandroid_example.base.WaitingActivityUseCase
import com.skfo763.rtcandroid_example.utils.TokenManager

class WaitingActivityViewModel(val useCase: WaitingActivityUseCase) : ViewModel() {

    private val _genderState = MutableLiveData<String>(useCase.getResString(R.string.gender_male))

    val genderState: LiveData<String> = _genderState

    val onSwitchChanged: (Boolean) -> Unit = {
        if(it) {
            _genderState.value = useCase.getResString(R.string.gender_female)
        } else {
            _genderState.value = useCase.getResString(R.string.gender_male)
        }
    }

    fun getToken(isMan: Boolean): String {
        return TokenManager.getToken(isMan)
    }

    fun startCall() {
        useCase.startCall()
    }

    class Factory(private val useCase: WaitingActivityUseCase) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return WaitingActivityViewModel(useCase) as T
        }
    }
}