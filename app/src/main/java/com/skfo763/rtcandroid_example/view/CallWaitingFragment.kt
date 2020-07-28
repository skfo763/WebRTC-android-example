package com.skfo763.rtcandroid_example.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.skfo763.rtcandroid_example.R
import com.skfo763.rtcandroid_example.base.BaseFragment
import com.skfo763.rtcandroid_example.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_call_waiting.*

class CallWaitingFragment : BaseFragment() {

    companion object {
        fun newInstance(): CallWaitingFragment {
            return CallWaitingFragment()
        }
    }

    private val sharedViewModel by activityViewModels<MainViewModel>()

    override fun onPageChangedComplete() {

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_call_waiting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // sharedViewModel.setWaitingLocalSurface(waiting_local)
    }

    override fun onStart() {
        super.onStart()
        sharedViewModel.apply {
            setPeerInfo()
            // startWaitingRender()
            setRtcWaiting()
        }
    }


}