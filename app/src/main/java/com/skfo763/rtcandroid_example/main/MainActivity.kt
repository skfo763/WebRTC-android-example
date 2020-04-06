package com.skfo763.rtcandroid_example.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.skfo763.rtc.data.DESTROY
import com.skfo763.rtc.utils.setLogDebug
import com.skfo763.rtcandroid_example.R
import com.skfo763.rtcandroid_example.VoiceExampleApplication
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import org.webrtc.MediaStream

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSON_REQUEST_CODE = 1
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }

    val channel = VoiceExampleApplication.coChannel
    private lateinit var roomName: String
    private val mainViewModel: MainViewModel by lazy { MainViewModel() }

    val receive = CoroutineScope(Dispatchers.Main).async {
        val receivedData = channel.channel.asFlow()
        receivedData.collect { data ->
            setLogDebug("received data: $data")
            when(data) {
                "initView" -> {
                    mainViewModel.setInitRender(local_view, remote_view)
                }
                DESTROY -> {
                    doOnDestroy()
                }
                is MediaStream -> {
                    data?.videoTracks[0]?.addSink(remote_view)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        observeLiveData()
        checkCameraPermission()
    }

    fun getExtras() {
        roomName = intent.getStringExtra("roomName") ?: ""
    }

    fun observeLiveData() {
        mainViewModel.progressStatus.observe(this, Observer {
            remote_view_loading.visibility = if(it) View.VISIBLE else View.GONE
        })
    }

    private fun checkCameraPermission() {
        if(ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION)
                != PackageManager.PERMISSION_GRANTED) {

            requestCameraPermission()
        } else {
            onCameraPermissionGranted()
        }
    }

    private fun requestCameraPermission(dialogShown: Boolean = false) {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION)
                && !dialogShown) {
            showPermissionRationalDialog()
        } else {
            ActivityCompat.requestPermissions(
                    this, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSON_REQUEST_CODE)
        }
    }

    private fun showPermissionRationalDialog() {
        AlertDialog.Builder(this)
                .setTitle("Camera Permission Required")
                .setMessage("This app need the camera to function")
                .setPositiveButton("Grant") { dialog, _ ->
                    dialog.dismiss()
                    requestCameraPermission(true)
                }
                .setNegativeButton("Deny") { dialog, _ ->
                    dialog.dismiss()
                    onCameraPermissionDenied()
                }
                .show()
    }

    private fun onCameraPermissionDenied() {
        Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG).show()
    }

    private fun onCameraPermissionGranted() {
        mainViewModel.onCameraPermissionGranted(application)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) { super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            CAMERA_PERMISSON_REQUEST_CODE -> {
                if(grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    onCameraPermissionGranted()
                } else {
                    onCameraPermissionDenied()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        doOnDestroy()
    }

    override fun onPause() {
        super.onPause()
        doOnPause()
    }

    fun doOnPause() {
        remote_view.release()
        local_view.release()
    }

    fun doOnDestroy() {
        mainViewModel.destroyPeerAndSocket()
        remote_view.release()
        local_view.release()
    }
}
