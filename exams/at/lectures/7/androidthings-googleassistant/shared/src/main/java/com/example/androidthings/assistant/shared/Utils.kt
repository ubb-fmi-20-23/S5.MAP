package com.example.androidthings.assistant.shared

import android.content.ContentValues.TAG
import android.util.Log

const val TAG = "Assistant"

fun Any.logd(message: String = "No message!", cause: Throwable? = null) {
  Log.d(TAG, message, cause)
}

fun Any.logw(message: String = "No message!", cause: Throwable? = null) {
  Log.w(TAG, message, cause)
}

fun Any.loge(message: String = "No message!", cause: Throwable? = null) {
  Log.e(TAG, message, cause)
}

fun Any.logi(message: String = "No message!", cause: Throwable? = null) {
  Log.i(TAG, message, cause)
}