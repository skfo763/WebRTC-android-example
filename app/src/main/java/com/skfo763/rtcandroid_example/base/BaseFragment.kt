package com.skfo763.rtcandroid_example.base

import android.widget.Toast
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {

    /**
     * 다른 프래그먼트에서 해당 프래그먼트로 왔을 때,
     * ViewPager 의 스크롤링이 완전히 종료되었을 때 호출됩니다.
     */
    protected abstract fun onPageChanged()

    /**
     * 해당 프래그먼트가 다른 프래그먼트에 의해 가려지기 시작할 때
     * 강제로 넘어가는 경우 - setCurrentItem 을 통해서 - 넘어가는 로직 실행 전에 호출
     */
    protected abstract fun onPageHideStart(nextPage: FragmentType)

    /**
     * 백버튼이 눌렸을 때 호출됨
     */
    protected abstract fun goBackConfirmed()


    fun onPageChangeComplete() {
        if (!isAdded) return
        onPageChanged()
    }

    fun onPageHideStartFromActivity(nextPage: FragmentType) {
        if(!isAdded) return
        onPageHideStart(nextPage)
    }

    fun onBackPressedComplete(doOnFragmentNull: () -> Unit) {
        if(isAdded) {
            goBackConfirmed()
        } else {
            doOnFragmentNull.invoke()
        }
    }

    fun showToast(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }




}