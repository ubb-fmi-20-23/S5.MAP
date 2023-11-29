package com.example.androidthings.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class NearbyAdvertisingManager(private val ctx: Context, private val listener: EventListener) {
  private val client: ConnectionsClient

  private val connectionLifeCycleCB = object : ConnectionLifecycleCallback() {
    override fun onConnectionInitiated(s: String, connectionInfo: ConnectionInfo) {
      logi("Connection initiated. Endpoint [$s]")
      logi("Incoming endpoint [${connectionInfo.endpointName}]")
      logi("Is incoming [${connectionInfo.isIncomingConnection}]")
      // Let us accept the connection
      Nearby.getConnectionsClient(ctx).acceptConnection(s, payloadCallback)
    }

    override fun onConnectionResult(s: String, connectionResolution: ConnectionResolution) {
      logi("Connection result. Endpoint [$s]")
    }

    override fun onDisconnected(s: String) {
      logi("Disconnected. Endpoint [$s]")
    }
  }

  private val payloadCallback = object : PayloadCallback() {
    override fun onPayloadReceived(s: String, payload: Payload) {
      logi("Payload received")
      val b = payload.asBytes()
      val content = String(b!!)
      logi("Content [$content]")
      listener.onMessage(content)
    }

    override fun onPayloadTransferUpdate(s: String, payloadTransferUpdate: PayloadTransferUpdate) {
      logd("Payload Transfer update [$s]")
      if (payloadTransferUpdate.status == PayloadTransferUpdate.Status.SUCCESS) {
        logi("Payload received from Endpoint [$s]")
      }
    }
  }

  init {
    logd("Constructor..")
    val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
    client = Nearby.getConnectionsClient(ctx)
    client.startAdvertising("AndroidThings",
        SERVICE_ID,
        connectionLifeCycleCB,
        advertisingOptions)
        .addOnSuccessListener { logi("OnSuccess...") }
        .addOnFailureListener { e -> loge("Constructor OnFailure: ", e) }
  }
  //adb -s Android.local shell am force-stop com.android.iotlauncher.ota
  //adb -s Android.local shell am force-stop com.example.androidthings.nearby

  fun close() {
    client.stopAdvertising()
  }

  interface EventListener {
    fun onMessage(message: String)
  }

  companion object {
    private const val SERVICE_ID = "com.example.androidthings.nearby"
  }
}
