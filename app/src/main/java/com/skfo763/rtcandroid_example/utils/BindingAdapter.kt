package com.skfo763.rtcandroid_example.utils

import androidx.appcompat.widget.SwitchCompat
import androidx.databinding.BindingAdapter
import androidx.viewpager2.widget.ViewPager2

object BindingAdapter {

    @JvmStatic
    @BindingAdapter("isScrollale")
    fun ViewPager2.setIsScrollable(isScrollable: Boolean) {
        this.isUserInputEnabled = isScrollable
    }

    @JvmStatic
    @BindingAdapter("setSwitchCheckedListener")
    fun SwitchCompat.setOnSwitchCheckedListener(onSwitchChecked: (Boolean) -> Unit) {
        this.setOnCheckedChangeListener { _, isChecked ->
            onSwitchChecked(isChecked)
        }
    }
}