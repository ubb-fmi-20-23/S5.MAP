package ro.cojocar.buttonthings

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class BluetoothLeService : Service() {

  private var mBluetoothManager: BluetoothManager? = null
  private var mBluetoothAdapter: BluetoothAdapter? = null
  private var mBluetoothDeviceAddress: String? = null
  private var mBluetoothGatt: BluetoothGatt? = null

  // Implements callback methods for GATT events that the app cares about.  For example,
  // connection change and services discovered.
  private val mGattCallback = object : BluetoothGattCallback() {
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
      val intentAction: String
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        intentAction = ACTION_GATT_CONNECTED
        broadcastUpdate(intentAction)
        Log.i(TAG, "Connected to GATT server.")
        // Attempts to discover services after successful connection.
        Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt!!.discoverServices())

      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        intentAction = ACTION_GATT_DISCONNECTED
        Log.i(TAG, "Disconnected from GATT server.")
        broadcastUpdate(intentAction)
      }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
      } else {
        Log.w(TAG, "onServicesDiscovered received: $status")
      }
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt,
                                      characteristic: BluetoothGattCharacteristic,
                                      status: Int) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        broadcastUpdate(characteristic)
      }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                         characteristic: BluetoothGattCharacteristic) {
      broadcastUpdate(characteristic)
    }
  }

  private val mBinder = LocalBinder()

  /**
   * Retrieves a list of supported GATT services on the connected device. This should be
   * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
   *
   * @return A `List` of supported services.
   */
  val supportedGattServices: List<BluetoothGattService>?
    get() = if (mBluetoothGatt == null) null else mBluetoothGatt!!.services

  private fun broadcastUpdate(action: String) {
    val intent = Intent(action)
    sendBroadcast(intent)
  }

  private fun broadcastUpdate(characteristic: BluetoothGattCharacteristic) {
    val intent = Intent(BluetoothLeService.ACTION_DATA_AVAILABLE)

    // For all other profiles, writes the data formatted in HEX.
    val data = characteristic.value
    if (data != null && data.size > 0) {
      val stringBuilder = StringBuilder(data.size)
      for (byteChar in data)
        stringBuilder.append(String.format("%02X ", byteChar))
      intent.putExtra(EXTRA_DATA, "$data\n$stringBuilder")
    }

    sendBroadcast(intent)
  }

  inner class LocalBinder : Binder() {
    val service: BluetoothLeService
      get() = this@BluetoothLeService
  }

  override fun onBind(intent: Intent): IBinder? {
    return mBinder
  }

  override fun onUnbind(intent: Intent): Boolean {
    // After using a given device, you should make sure that BluetoothGatt.close() is called
    // such that resources are cleaned up properly.  In this particular example, close() is
    // invoked when the UI is disconnected from the Service.
    close()
    return super.onUnbind(intent)
  }

  /**
   * Initializes a reference to the local Bluetooth adapter.
   *
   * @return Return true if the initialization is successful.
   */
  fun notInitialize(): Boolean {
    // For API level 18 and above, get a reference to BluetoothAdapter through
    // BluetoothManager.
    if (mBluetoothManager == null) {
      mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
      if (mBluetoothManager == null) {
        Log.e(TAG, "Unable to initialize BluetoothManager.")
        return true
      }
    }

    mBluetoothAdapter = mBluetoothManager!!.adapter
    if (mBluetoothAdapter == null) {
      Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
      return true
    }

    return false
  }

  /**
   * Connects to the GATT server hosted on the Bluetooth LE device.
   *
   * @param address The device address of the destination device.
   * @return Return true if the connection is initiated successfully. The connection result
   * is reported asynchronously through the
   * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
   * callback.
   */
  fun connect(address: String?): Boolean {
    if (mBluetoothAdapter == null || address == null) {
      Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
      return false
    }

    // Previously connected device.  Try to reconnect.
    if (address == mBluetoothDeviceAddress && mBluetoothGatt != null) {
      Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
      return mBluetoothGatt!!.connect()
    }

    val device = mBluetoothAdapter!!.getRemoteDevice(address)
    if (device == null) {
      Log.w(TAG, "Device not found.  Unable to connect.")
      return false
    }
    // We want to directly connect to the device, so we are setting the autoConnect
    // parameter to false.
    mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
    Log.d(TAG, "Trying to create a new connection.")
    mBluetoothDeviceAddress = address
    return true
  }

  /**
   * Disconnects an existing connection or cancel a pending connection. The disconnection result
   * is reported asynchronously through the
   * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
   * callback.
   */
  fun disconnect() {
    if (mBluetoothAdapter == null || mBluetoothGatt == null) {
      Log.w(TAG, "BluetoothAdapter not initialized")
      return
    }
    mBluetoothGatt!!.disconnect()
  }

  /**
   * After using a given BLE device, the app must call this method to ensure resources are
   * released properly.
   */
  fun close() {
    if (mBluetoothGatt == null) {
      return
    }
    mBluetoothGatt!!.close()
    mBluetoothGatt = null
  }

  /**
   * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
   * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
   * callback.
   *
   * @param characteristic The characteristic to read from.
   */
  fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
    if (mBluetoothAdapter == null || mBluetoothGatt == null) {
      Log.w(TAG, "BluetoothAdapter not initialized")
      return
    }
    mBluetoothGatt!!.readCharacteristic(characteristic)
  }

  /**
   * Enables or disables notification on a give characteristic.
   *
   * @param characteristic Characteristic to act on.
   * @param enabled        If true, enable notification.  False otherwise.
   */
  fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic,
                                    enabled: Boolean) {
    if (mBluetoothAdapter == null || mBluetoothGatt == null) {
      Log.w(TAG, "BluetoothAdapter not initialized")
      return
    }
    mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)
  }

  companion object {
    private val TAG = BluetoothLeService::class.java.getSimpleName()

    val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
    val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
    val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
    val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
    val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
  }
}