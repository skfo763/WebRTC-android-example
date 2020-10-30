package com.skfo763.rtcandroid_example.utils

import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

fun AppCompatActivity.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

fun View.throttleOnClick(
    compositeDisposable : CompositeDisposable,
    throttleTime: Long = 500,
    scheduler: Scheduler = AndroidSchedulers.mainThread(),
    action: (() -> Unit)? = null
) {
    compositeDisposable.add(clicks().throttleFirst(throttleTime, TimeUnit.MILLISECONDS, scheduler)
        .subscribe ({
            action?.invoke()
        }) {
            Log.e("ThrottleOnClick", it.message ?: "unknown error")
        }
    )
}
