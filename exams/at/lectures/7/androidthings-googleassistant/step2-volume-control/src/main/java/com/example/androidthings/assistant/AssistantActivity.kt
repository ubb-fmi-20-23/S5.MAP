/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.assistant

import android.app.Activity
import android.content.Context
import android.media.*
import android.media.MediaRecorder.AudioSource
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.widget.ArrayAdapter
import android.widget.ListView
import com.example.androidthings.assistant.shared.*
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.google.assistant.embedded.v1alpha2.*
import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import io.grpc.auth.MoreCallCredentials
import io.grpc.stub.StreamObserver
import org.json.JSONException
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

class AssistantActivity : Activity(), Button.OnButtonEventListener {
  private var mOutputBufferSize: Int = 0

  // gRPC client and stream observers.
  private var mAssistantService: EmbeddedAssistantGrpc.EmbeddedAssistantStub? = null
  private var mAssistantRequestObserver: StreamObserver<AssistRequest>? = null
  private val mAssistantResponseObserver = object : StreamObserver<AssistResponse> {
    override fun onNext(value: AssistResponse) {
      if (value.eventType != null) {
        logd("converse response event: " + value.eventType)
      }
      if (value.speechResultsList != null && value.speechResultsList.size > 0) {
        for (result in value.speechResultsList) {
          val spokenRequestText = result.transcript
          if (!spokenRequestText.isEmpty()) {
            logi("assistant request text: $spokenRequestText")
            mMainHandler!!.post { mAssistantRequestsAdapter!!.add(spokenRequestText) }
          }
        }
      }
      if (value.dialogStateOut != null) {
        val volume = value.dialogStateOut.volumePercentage
        if (volume > 0) {
          mVolumePercentage = volume
          logi("assistant volume changed: $mVolumePercentage")
          mAudioTrack!!.setVolume(AudioTrack.getMaxVolume() * mVolumePercentage / 100.0f)
        }
        mConversationState = value.dialogStateOut.conversationState
      }
      if (value.audioOut != null) {
        val audioData = ByteBuffer.wrap(value.audioOut.audioData.toByteArray())
        logd("converse audio size: " + audioData.remaining())
        mAssistantResponses.add(audioData)
      }
    }

    override fun onError(t: Throwable) {
      loge("converse error:", t)
    }

    override fun onCompleted() {
      mAudioTrack = AudioTrack.Builder()
          .setAudioFormat(AUDIO_FORMAT_OUT_MONO)
          .setBufferSizeInBytes(mOutputBufferSize)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()
      if (mAudioOutputDevice != null) {
        mAudioTrack!!.preferredDevice = mAudioOutputDevice
      }
      mAudioTrack!!.play()

      for (audioData in mAssistantResponses) {
        logd("Playing a bit of audio")
        mAudioTrack!!.write(audioData, audioData.remaining(),
            AudioTrack.WRITE_BLOCKING)
      }
      mAssistantResponses.clear()
      mAudioTrack!!.stop()

      logi("assistant response finished")
      if (mLed != null) {
        try {
          mLed!!.value = false
        } catch (e: IOException) {
          loge("error turning off LED:", e)
        }

      }
    }
  }

  // Audio playback and recording objects.
  private var mAudioTrack: AudioTrack? = null
  private var mAudioRecord: AudioRecord? = null

  // Audio routing configuration: use default routing.
  private var mAudioInputDevice: AudioDeviceInfo? = null
  private var mAudioOutputDevice: AudioDeviceInfo? = null

  // Hardware peripherals.
  private var mButton: Button? = null
  private var mLed: Gpio? = null

  // Assistant Thread and Runnables implementing the push-to-talk functionality.
  private var mConversationState: ByteString? = null
  private var mAssistantThread: HandlerThread? = null
  private var mAssistantHandler: Handler? = null
  private val mAssistantResponses = ArrayList<ByteBuffer>()
  private val mStartAssistantRequest = Runnable {
    logi("starting assistant request")
    mAudioRecord!!.startRecording()
    mAssistantRequestObserver = mAssistantService!!.assist(mAssistantResponseObserver)
    val converseConfigBuilder = AssistConfig.newBuilder()
        .setAudioInConfig(ASSISTANT_AUDIO_REQUEST_CONFIG)
        .setAudioOutConfig(ASSISTANT_AUDIO_RESPONSE_CONFIG)
        .setDeviceConfig(DeviceConfig.newBuilder()
            .setDeviceModelId(MyDevice.MODEL_ID)
            .setDeviceId(MyDevice.INSTANCE_ID)
            .build())
    val dialogStateInBuilder = DialogStateIn.newBuilder()
        .setLanguageCode(MyDevice.LANGUAGE_CODE)
    if (mConversationState != null) {
      dialogStateInBuilder.conversationState = mConversationState
    }
    converseConfigBuilder.dialogStateIn = dialogStateInBuilder.build()
    mAssistantRequestObserver!!.onNext(
        AssistRequest.newBuilder()
            .setConfig(converseConfigBuilder.build())
            .build())
    mAssistantHandler!!.post(mStreamAssistantRequest)
  }
  private val mStreamAssistantRequest = object : Runnable {
    override fun run() {
      val audioData = ByteBuffer.allocateDirect(SAMPLE_BLOCK_SIZE)
      if (mAudioInputDevice != null) {
        mAudioRecord!!.preferredDevice = mAudioInputDevice
      }
      val result = mAudioRecord!!.read(audioData, audioData.capacity(), AudioRecord.READ_BLOCKING)
      if (result < 0) {
        loge("error reading from audio stream:$result")
        return
      }
      logd("streaming ConverseRequest: $result")
      mAssistantRequestObserver!!.onNext(AssistRequest.newBuilder()
          .setAudioIn(ByteString.copyFrom(audioData))
          .build())
      mAssistantHandler!!.post(this)
    }
  }
  private val mStopAssistantRequest = Runnable {
    logi("ending assistant request")
    mAssistantHandler!!.removeCallbacks(mStreamAssistantRequest)
    if (mAssistantRequestObserver != null) {
      mAssistantRequestObserver!!.onCompleted()
      mAssistantRequestObserver = null
    }
    mAudioRecord!!.stop()
    mAudioTrack!!.play()
  }
  private var mMainHandler: Handler? = null

  // List & adapter to store and display the history of Assistant Requests.
  private val mAssistantRequests = ArrayList<String>()
  private var mAssistantRequestsAdapter: ArrayAdapter<String>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    logi("starting assistant demo")

    setContentView(R.layout.activity_main)
    val assistantRequestsListView = findViewById<ListView>(R.id.assistantRequestsListView)
    mAssistantRequestsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
        mAssistantRequests)
    assistantRequestsListView.adapter = mAssistantRequestsAdapter
    mMainHandler = Handler(mainLooper)

    mAssistantThread = HandlerThread("assistantThread")
    mAssistantThread!!.start()
    mAssistantHandler = Handler(mAssistantThread!!.looper)

    try {
      mButton = Button(BoardDefaults.gpioForButton,
          Button.LogicState.PRESSED_WHEN_LOW)
      mLed = PeripheralManager.getInstance().openGpio(BoardDefaults.gpioForLED)

      mButton!!.setDebounceDelay(BUTTON_DEBOUNCE_DELAY_MS.toLong())
      mButton!!.setOnButtonEventListener(this)

      mLed!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
      mLed!!.setActiveType(Gpio.ACTIVE_HIGH)
    } catch (e: IOException) {
      loge("error configuring peripherals:", e)
      return
    }

    val manager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    logi("setting volume to: $maxVolume")
    manager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
    mOutputBufferSize = AudioTrack.getMinBufferSize(AUDIO_FORMAT_OUT_MONO.sampleRate,
        AUDIO_FORMAT_OUT_MONO.channelMask,
        AUDIO_FORMAT_OUT_MONO.encoding)
    mAudioTrack = AudioTrack.Builder()
        .setAudioFormat(AUDIO_FORMAT_OUT_MONO)
        .setBufferSizeInBytes(mOutputBufferSize)
        .build()
    mAudioTrack!!.play()
    val inputBufferSize = AudioRecord.getMinBufferSize(AUDIO_FORMAT_STEREO.sampleRate,
        AUDIO_FORMAT_STEREO.channelMask,
        AUDIO_FORMAT_STEREO.encoding)
    mAudioRecord = AudioRecord.Builder()
        .setAudioSource(AudioSource.VOICE_RECOGNITION)
        .setAudioFormat(AUDIO_FORMAT_IN_MONO)
        .setBufferSizeInBytes(inputBufferSize)
        .build()

    val channel = ManagedChannelBuilder.forTarget(ASSISTANT_ENDPOINT).build()
    try {
      mAssistantService = EmbeddedAssistantGrpc.newStub(channel)
          .withCallCredentials(MoreCallCredentials.from(
              Credentials.fromResource(this, R.raw.credentials)
          ))
    } catch (e: IOException) {
      loge("error creating assistant service:", e)
    } catch (e: JSONException) {
      loge("error creating assistant service:", e)
    }

  }

  override fun onButtonEvent(button: Button, pressed: Boolean) {
    try {
      if (mLed != null) {
        mLed!!.value = pressed
      }
    } catch (e: IOException) {
      logd("error toggling LED:", e)
    }

    if (pressed) {
      mAssistantHandler!!.post(mStartAssistantRequest)
    } else {
      mAssistantHandler!!.post(mStopAssistantRequest)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    logi("destroying assistant demo")
    if (mAudioRecord != null) {
      mAudioRecord!!.stop()
      mAudioRecord = null
    }
    if (mAudioTrack != null) {
      mAudioTrack!!.stop()
      mAudioTrack = null
    }
    if (mLed != null) {
      try {
        mLed!!.close()
      } catch (e: IOException) {
        logw("error closing LED", e)
      }

      mLed = null
    }
    if (mButton != null) {
      try {
        mButton!!.close()
      } catch (e: IOException) {
        logw("error closing button", e)
      }

      mButton = null
    }

    mAssistantHandler!!.post { mAssistantHandler!!.removeCallbacks(mStreamAssistantRequest) }
    mAssistantThread!!.quitSafely()
  }

  companion object {
    // Peripheral and drivers constants.
    private const val BUTTON_DEBOUNCE_DELAY_MS = 20

    // Audio constants.
    private const val SAMPLE_RATE = 16000
    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private var mVolumePercentage = 100
    private val ENCODING_INPUT = AudioInConfig.Encoding.LINEAR16
    private val ENCODING_OUTPUT = AudioOutConfig.Encoding.LINEAR16
    private val ASSISTANT_AUDIO_REQUEST_CONFIG = AudioInConfig.newBuilder()
        .setEncoding(ENCODING_INPUT)
        .setSampleRateHertz(SAMPLE_RATE)
        .build()
    private val ASSISTANT_AUDIO_RESPONSE_CONFIG = AudioOutConfig.newBuilder()
        .setEncoding(ENCODING_OUTPUT)
        .setSampleRateHertz(SAMPLE_RATE)
        .setVolumePercentage(mVolumePercentage)
        .build()
    private val AUDIO_FORMAT_STEREO = AudioFormat.Builder()
        .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
        .setEncoding(ENCODING)
        .setSampleRate(SAMPLE_RATE)
        .build()
    private val AUDIO_FORMAT_OUT_MONO = AudioFormat.Builder()
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .setEncoding(ENCODING)
        .setSampleRate(SAMPLE_RATE)
        .build()
    private val AUDIO_FORMAT_IN_MONO = AudioFormat.Builder()
        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
        .setEncoding(ENCODING)
        .setSampleRate(SAMPLE_RATE)
        .build()
    private const val SAMPLE_BLOCK_SIZE = 1024

    // Google Assistant API constants.
    private const val ASSISTANT_ENDPOINT = "embeddedassistant.googleapis.com"
  }
}
