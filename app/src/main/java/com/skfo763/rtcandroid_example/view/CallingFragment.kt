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
import kotlinx.android.synthetic.main.fragment_calling.*

class CallingFragment : BaseFragment() {

    companion object {
        fun newInstance(): CallingFragment {
            return CallingFragment()
        }
    }

    private val sharedViewModel: MainViewModel by activityViewModels()

    override fun onPageChangedComplete() {

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calling, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("hellohello", "calling - onViewCreated")

        sharedViewModel.setCallingLocalSurfaceView(calling_local)
        sharedViewModel.setCallingSurfaceRenderer(calling_remote)
    }

}