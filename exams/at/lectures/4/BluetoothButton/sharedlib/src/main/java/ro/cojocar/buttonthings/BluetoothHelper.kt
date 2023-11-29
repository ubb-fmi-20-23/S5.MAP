package ro.cojocar.buttonthings

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.IntentFilter
import android.os.ParcelUuid
import android.util.Log

object BluetoothHelper {
  private val TAG = "BluetoothHelper"

  val ANDROID_THINGS_DEVICE_NAME = "AT"
  val MOBILE_DEVICE_NAME = "OnePlus 6T"

  val REQUEST_ENABLE_BT = 1
  // Stops scanning after 10 seconds.
  val SCAN_PERIOD: Long = 10000

  var bluetoothManager: BluetoothManager? = null
  var bluetoothGattServer: BluetoothGattServer? = null
    private set
  private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null

  /**
   * Callback to receive information about the advertisement process.
   */
  private val mAdvertiseCallback = object : AdvertiseCallback() {
    override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
      Log.i(TAG, "LE Advertise Started.")
    }

    override fun onStartFailure(errorCode: Int) {
      Log.w(TAG, "LE Advertise Failed: $errorCode")
    }
  }

  fun makeGattUpdateIntentFilter(): IntentFilter {
    val intentFilter = IntentFilter()
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
    intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
    return intentFilter
  }

  /**
   * Begin advertising over Bluetooth that this device is connectable
   * and supports the Remote LED Service.
   */
  fun startAdvertising() {
    val bluetoothAdapter = BluetoothHelper.bluetoothManager!!.adapter
    mBluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser

    if (mBluetoothLeAdvertiser == null) {
      Log.w(TAG, "Failed to create advertiser")
      return
    }

    val settings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setConnectable(true)
        .setTimeout(0)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .build()

    val data = AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .setIncludeTxPowerLevel(false)
        .addServiceUuid(ParcelUuid(RemoteLedProfile.REMOTE_LED_SERVICE))
        .build()

    mBluetoothLeAdvertiser!!
        .startAdvertising(settings, data, mAdvertiseCallback)
  }

  /**
   * Stop Bluetooth advertisements.
   */
  fun stopAdvertising() {
    if (mBluetoothLeAdvertiser == null) return

    mBluetoothLeAdvertiser!!.stopAdvertising(mAdvertiseCallback)
  }

  /**
   * Initialize the GATT server instance with the services/characteristics
   * from the Remote LED Profile.
   */
  fun startServer(context: Context, mGattServerCallback: BluetoothGattServerCallback) {
    bluetoothGattServer = bluetoothManager!!.openGattServer(context, mGattServerCallback)
    if (bluetoothGattServer == null) {
      Log.w(TAG, "Unable to create GATT server")
      return
    }

    bluetoothGattServer!!.addService(RemoteLedProfile.createRemoteLedService())
  }

  /**
   * Shut down the GATT server.
   */
  fun stopServer() {
    if (bluetoothGattServer == null) return

    bluetoothGattServer!!.close()
  }
}