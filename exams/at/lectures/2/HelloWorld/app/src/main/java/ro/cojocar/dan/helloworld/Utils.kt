package ro.cojocar.dan.helloworld

import android.util.Log

fun Any.logd(message: String = "No message", cause: Throwable? = null) {
  Log.d(this.javaClass.canonicalName, message, cause)
}

fun Any.loge(message: String = "No message", cause: Throwable? = null) {
  Log.e(this.javaClass.canonicalName, message, cause)
}