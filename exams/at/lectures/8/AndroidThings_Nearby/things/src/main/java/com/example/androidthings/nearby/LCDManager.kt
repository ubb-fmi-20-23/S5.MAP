package com.example.androidthings.nearby

import com.nilhcem.androidthings.driver.lcd1602a.Lcd1602

class LCDManager {

  private var mLcd: Lcd1602? = null

  fun displayString(data: String) {
    try {
      logd("Writing: $data")
      if (mLcd == null)
        mLcd = Lcd1602(GPIO_LCD_RS, GPIO_LCD_EN, GPIO_LCD_D4, GPIO_LCD_D5, GPIO_LCD_D6, GPIO_LCD_D7)
      mLcd!!.clear()
      mLcd!!.print(data)
    } catch (e: Exception) {
      loge("Unable to initialize the lcd!", e)
    }
  }

  fun close() {
    if (mLcd != null) {
      try {
        mLcd!!.close()
      } catch (e: Exception) {
        loge("Unable to close the lcd", e)
      }

    }
  }

  companion object {

    private const val GPIO_LCD_RS = "BCM26"
    private const val GPIO_LCD_EN = "BCM19"

    private const val GPIO_LCD_D4 = "BCM21"
    private const val GPIO_LCD_D5 = "BCM20"
    private const val GPIO_LCD_D6 = "BCM16"
    private const val GPIO_LCD_D7 = "BCM12"
  }
}
