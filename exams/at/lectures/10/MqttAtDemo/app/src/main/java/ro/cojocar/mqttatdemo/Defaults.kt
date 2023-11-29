package ro.cojocar.mqttatdemo

import android.os.Build

object BoardDefaults {
    private const val DEVICE_RPI3 = "rpi3"
    private const val DEVICE_IMX7D_PICO = "imx7d_pico"

    val gPIOLed: String
        get() = when (Build.DEVICE) {
            DEVICE_RPI3 -> "BCM14"
            DEVICE_IMX7D_PICO -> "GPIO2_IO02"
            else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
        }

    val topicName: String
        get() = when (Build.DEVICE) {
            DEVICE_RPI3 -> "topic/rpi3"
            DEVICE_IMX7D_PICO -> "topic/pico"
            else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
        }


}