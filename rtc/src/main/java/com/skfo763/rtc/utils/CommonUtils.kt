package com.skfo763.rtc.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.skfo763.rtc.data.LOG_TAG
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import java.text.SimpleDateFormat
import java.util.*

var dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)

fun getTimeHour() = "Time : ${dateFormat.format(Date(System.currentTimeMillis()))}"
fun setLogDebug(msg: String) = Log.d(LOG_TAG, "[${getTimeHour()}] $msg")

// Put a |key|->|value| mapping in |json|.
fun JSONObject.jsonPut(key: String, value: Any) {
    try {
        put(key, value)
    } catch (e: JSONException) {
        throw RuntimeException(e)
    }
}

// Converts a Java candidate to a JSONObject.
fun toJsonCandidate(candidate: IceCandidate?): JSONObject {
    val json = JSONObject().apply {
        candidate?.let {
            jsonPut("label", it.sdpMLineIndex)
            jsonPut("id", it.sdpMid)
            jsonPut("candidate", it.sdp)
        }
    }
    return json
}

// Converts a JSON candidate to a Java object.
@Throws(JSONException::class)
fun toJavaCandidate(json: JSONObject): IceCandidate? {
    return IceCandidate(
        json.getString("id"), json.getInt("label"), json.getString("candidate")
    )
}


// Log |msg| and Toast about it.
fun Context.logAndToast(tag: String, msg: String) {
    Log.d(tag, msg)
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}