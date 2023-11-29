package com.example.miquel.androidofthingsexperiments

import android.util.Log
import java.util.*

class ReadSensorsTask(val arduino: Arduino) : TimerTask() {
    private val TAG = "TimerTask"

    override fun run() {
        readTemp()
        readHumidity()
    }

    private fun readHumidity() {
        arduino.write("H")
        Thread.sleep(100)
        val humidity = arduino.read()
        Log.i(TAG, "Humidity $humidity%")
    }

    private fun readTemp() {
        arduino.write("T")
        Thread.sleep(100)
        val output = arduino.read()
        Log.i(TAG, "Temperature ${output}C")
    }
}