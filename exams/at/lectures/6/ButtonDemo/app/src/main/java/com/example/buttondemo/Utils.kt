package com.example.buttondemo

import android.util.Log

fun Any.logd(message: String = "No message!", cause: Throwable? = null) {
  Log.d(this.javaClass.simpleName, message, cause)
}

fun Any.loge(message: String = "No message!", cause: Throwable? = null) {
  Log.e(this.javaClass.simpleName, message, cause)
}