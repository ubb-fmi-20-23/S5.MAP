package com.example.androidthings.assistant.shared

import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import java.io.Closeable

class Display(private val display: AlphanumericDisplay = AlphanumericDisplay(Display.DISPLAY_I2C_BUS)) : Closeable {

  companion object {
    const val DISPLAY_I2C_BUS = "I2C1"
  }

  init {
    display.setEnabled(true)
    display.clear()
  }

  fun displayMessage(message: String) {
    display.display(message)
  }

  override fun close() {
    display.clear()
    display.setEnabled(false)
    display.close()
  }
}