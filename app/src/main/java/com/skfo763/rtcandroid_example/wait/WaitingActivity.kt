package com.skfo763.rtcandroid_example.wait

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.skfo763.rtcandroid_example.R
import com.skfo763.rtcandroid_example.main.MainActivity
import kotlinx.android.synthetic.main.activity_waiting.*

class WaitingActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_VOICE_CHAT = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        appCompatButton.setOnClickListener {
            startActivityForResult(
                Intent(this, MainActivity::class.java).apply {
                    putExtra("roomName", editText.text.toString())
                }, REQUEST_CODE_VOICE_CHAT
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            REQUEST_CODE_VOICE_CHAT -> {
                AlertDialog.Builder(this)
                    .setTitle("알림")
                    .setMessage("통화가 취소되었습니다.")
                    .create()
                    .show()
            }
        }
    }
}
