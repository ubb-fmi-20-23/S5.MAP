package ro.cojocar.mqttrainbow

import android.util.Log

private val TAG = "MQTT"

fun Any.loge(message: String = "missing message", cause: Throwable? = null) {
    Log.e(TAG, message, cause)
}

fun Any.logd(message: String = "missing message", cause: Throwable? = null) {
    Log.d(TAG, message, cause)
}