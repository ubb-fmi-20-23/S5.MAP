package com.example.miquel.androidofthingsexperiments

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.android.things.pio.Gpio
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var arduino: Arduino
    private val readSensorsTimer = Timer("ReadSensorsTimer", true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arduino = Arduino()
        val time = 5L * 1000L
        readSensorsTimer.scheduleAtFixedRate(ReadSensorsTask(arduino), 0L, time)
    }

    override fun onDestroy() {
        super.onDestroy()
        readSensorsTimer.cancel()
        arduino.close()
    }
}

