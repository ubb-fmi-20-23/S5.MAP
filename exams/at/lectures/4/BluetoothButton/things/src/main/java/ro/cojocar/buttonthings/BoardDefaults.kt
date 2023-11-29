package ro.cojocar.buttonthings

import android.os.Build

internal object BoardDefaults {
  private val DEVICE_RPI3 = "rpi3"
  private val DEVICE_IMX7D_PICO = "imx7d_pico"
  private var sBoardVariant = ""

  /**
   * Return the GPIO pin that the LED is connected on.
   * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
   * that turns on when the GPIO pin is HIGH, and off when low.
   */
  val gpioForLED: String
    get() {
      when (boardVariant) {
        DEVICE_RPI3 -> return "BCM6"
        DEVICE_IMX7D_PICO -> return "GPIO2_IO02"
        else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
      }
    }

  /**
   * Return the GPIO pin that the Button is connected on.
   */
  val gpioForButton: String
    get() {
      when (boardVariant) {
        DEVICE_RPI3 -> return "BCM21"
        DEVICE_IMX7D_PICO -> return "GPIO6_IO14"
        else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
      }
    }

  private val boardVariant: String
    get() {
      if (!sBoardVariant.isEmpty()) {
        return sBoardVariant
      }
      sBoardVariant = Build.DEVICE
      return sBoardVariant
    }
}
