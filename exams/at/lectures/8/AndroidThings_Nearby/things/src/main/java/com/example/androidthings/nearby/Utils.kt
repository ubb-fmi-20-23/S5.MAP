package com.example.androidthings.nearby

import android.util.Log

const val TAG = "Demo"

fun Any.loge(message: String = "No message!", cause: Throwable? = null) {
  Log.e(TAG, message, cause)
}

fun Any.logw(message: String = "No message!", cause: Throwable? = null) {
  Log.w(TAG, message, cause)
}

fun Any.logd(message: String = "No message!", cause: Throwable? = null) {
  Log.d(TAG, message, cause)
}

fun Any.logi(message: String = "No message!", cause: Throwable? = null) {
  Log.i(TAG, message, cause)
}