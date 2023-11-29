package com.example.androidthings.bluetooth.audio

import android.util.Log

const val TAG = "BTest"

fun Any.logi(message: String = "no message!") {
  Log.i(TAG, message)
}

fun Any.logd(message: String = "no message!", cause: Throwable? = null) {
  Log.d(TAG, message, cause)
}

fun Any.logw(message: String = "no message!", cause: Throwable? = null) {
  Log.w(TAG, message, cause)
}

fun Any.loge(message: String = "no message!", cause: Throwable? = null) {
  Log.e(TAG, message, cause)
}