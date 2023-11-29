package ro.cojocar.dan.pidemo

import android.util.Log

fun Any.logi(message: String = "No message!", cause: Throwable? = null) {
    Log.i(this.javaClass.name, message, cause)
}

fun Any.logd(message: String = "No message!", cause: Throwable? = null) {
    Log.d(this.javaClass.name, message, cause)
}

fun Any.loge(message: String = "No message!", cause: Throwable? = null) {
    Log.e(this.javaClass.name, message, cause)
}