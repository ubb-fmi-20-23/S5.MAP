package com.example.miquel.androidofthingsexperiments

import android.util.Log
import com.google.android.things.pio.PeripheralManagerService
import com.google.android.things.pio.UartDevice

class Arduino(uartDevice: String = "UART0"): AutoCloseable {
    private val TAG = "Arduino"
    private val uart: UartDevice by lazy {
        PeripheralManagerService().openUartDevice(uartDevice).apply {
            setBaudrate(115200)
            setDataSize(8)
            setParity(UartDevice.PARITY_NONE)
            setStopBits(1)
        }
    }

    fun read(): String {
        val maxCount = 8
        val buffer = ByteArray(maxCount)
        var output = ""
        do {
            val count = uart.read(buffer, buffer.size)
            output += buffer.toReadableString()
            if(count == 0) break
            Log.d(TAG, "Read ${buffer.toReadableString()} $count bytes from peripheral")
        } while (true)
        return output
    }

    private fun ByteArray.toReadableString() = filter { it > 0.toByte() }
            .joinToString(separator = "") { it.toChar().toString() }

    fun write(value: String) {
        val count = uart.write(value.toByteArray(), value.length)
        Log.d(TAG, "Wrote $value $count bytes to peripheral")
    }

    override fun close() {
        uart.close()
    }
}


