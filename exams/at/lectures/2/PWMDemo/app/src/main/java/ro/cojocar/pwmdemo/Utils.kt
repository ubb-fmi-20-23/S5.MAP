package ro.cojocar.pwmdemo

import android.util.Log

fun Any.logi(message: String = "No message") {
    Log.i(this.javaClass.canonicalName, message)
}

fun Any.logd(message: String = "No message", cause: Throwable? = null) {
    Log.d(this.javaClass.canonicalName, message, cause)
}

fun Any.loge(message: String = "No message", cause: Throwable? = null) {
    Log.e(this.javaClass.canonicalName, message, cause)
}