package com.example.at.twillio

import android.util.Log

const val TAG = "Demo"

fun Any.logd(message: String = "No message!", cause: Throwable? = null) {
  Log.d(TAG, message, cause)
}

fun Any.loge(message: String = "No message!", cause: Throwable? = null) {
  Log.e(TAG, message, cause)
}