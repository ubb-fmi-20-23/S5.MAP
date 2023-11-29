package ro.cojocar.dan.myapplication

import android.util.Log

val tag = "Arduino"

fun Any.logd(message: String = "No message", cause: Throwable? = null) {
  Log.d(tag, message, cause)
}

fun Any.logi(message: String = "No message", cause: Throwable? = null) {
  Log.i(tag, message, cause)
}

fun Any.logw(message: String = "No message", cause: Throwable? = null) {
  Log.w(tag, message, cause)
}

fun Any.loge(message: String = "No message", cause: Throwable? = null) {
  Log.e(tag, message, cause)
}