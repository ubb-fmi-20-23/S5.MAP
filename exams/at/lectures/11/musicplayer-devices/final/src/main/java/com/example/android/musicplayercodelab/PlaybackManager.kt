/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.example.android.musicplayercodelab

import android.media.MediaPlayer.OnCompletionListener

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import java.io.IOException

/** Handles media playback using a [MediaPlayer].  */
internal class PlaybackManager(private val mContext: Context, private val mCallback: Callback?) : AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener {
  private var mState: Int = 0
  private var mPlayOnFocusGain: Boolean = false
  @Volatile
  var currentMedia: MediaMetadataCompat? = null
    private set

  private var mMediaPlayer: MediaPlayer? = null
  private val mAudioManager: AudioManager

  val isPlaying: Boolean
    get() = mPlayOnFocusGain || mMediaPlayer != null && mMediaPlayer!!.isPlaying

  val currentMediaId: String?
    get() = if (currentMedia == null) null else currentMedia!!.description.mediaId

  val currentStreamPosition: Int
    get() = if (mMediaPlayer != null) mMediaPlayer!!.currentPosition else 0

  private val availableActions: Long
    @PlaybackStateCompat.Actions
    get() {
      var actions = (PlaybackStateCompat.ACTION_PLAY
          or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
          or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
          or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
          or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
      if (isPlaying) {
        actions = actions or PlaybackStateCompat.ACTION_PAUSE
      }
      return actions
    }

  init {
    this.mAudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  }

  fun play(metadata: MediaMetadataCompat) {
    val mediaId = metadata.description.mediaId
    val mediaChanged = currentMedia == null || currentMediaId != mediaId

    if (mMediaPlayer == null) {
      mMediaPlayer = MediaPlayer()
      mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
      mMediaPlayer!!.setWakeMode(
          mContext.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
      mMediaPlayer!!.setOnCompletionListener(this)
    } else {
      if (mediaChanged) {
        mMediaPlayer!!.reset()
      }
    }

    if (mediaChanged) {
      currentMedia = metadata
      try {
        mMediaPlayer!!.setDataSource(
            mContext.applicationContext,
            Uri.parse(MusicLibrary.getSongUri(mediaId!!)))
        mMediaPlayer!!.prepare()
      } catch (e: IOException) {
        throw RuntimeException(e)
      }

    }

    if (tryToGetAudioFocus()) {
      mPlayOnFocusGain = false
      mMediaPlayer!!.start()
      mState = PlaybackStateCompat.STATE_PLAYING
      updatePlaybackState()
    } else {
      mPlayOnFocusGain = true
    }
  }

  fun pause() {
    if (isPlaying) {
      mMediaPlayer!!.pause()
      mAudioManager.abandonAudioFocus(this)
    }
    mState = PlaybackStateCompat.STATE_PAUSED
    updatePlaybackState()
  }

  fun stop() {
    mState = PlaybackStateCompat.STATE_STOPPED
    updatePlaybackState()
    // Give up Audio focus
    mAudioManager.abandonAudioFocus(this)
    // Relax all resources
    releaseMediaPlayer()
  }

  /** Try to get the system audio focus.  */
  private fun tryToGetAudioFocus(): Boolean {
    val result = mAudioManager.requestAudioFocus(
        this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
  }

  /**
   * Called by AudioManager on audio focus changes. Implementation of [ ].
   */
  override fun onAudioFocusChange(focusChange: Int) {
    var gotFullFocus = false
    var canDuck = false
    if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
      gotFullFocus = true

    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS
        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
      // We have lost focus. If we can duck (low playback volume), we can keep playing.
      // Otherwise, we need to pause the playback.
      canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
    }

    if (gotFullFocus || canDuck) {
      if (mMediaPlayer != null) {
        if (mPlayOnFocusGain) {
          mPlayOnFocusGain = false
          mMediaPlayer!!.start()
          mState = PlaybackStateCompat.STATE_PLAYING
          updatePlaybackState()
        }
        val volume = if (canDuck) 0.2f else 1.0f
        mMediaPlayer!!.setVolume(volume, volume)
      }
    } else if (mState == PlaybackStateCompat.STATE_PLAYING) {
      mMediaPlayer!!.pause()
      mState = PlaybackStateCompat.STATE_PAUSED
      updatePlaybackState()
    }
  }

  /**
   * Called when media player is done playing current song.
   *
   * @see OnCompletionListener
   */
  override fun onCompletion(player: MediaPlayer) {
    stop()
  }

  /** Releases resources used by the service for playback.  */
  private fun releaseMediaPlayer() {
    if (mMediaPlayer != null) {
      mMediaPlayer!!.reset()
      mMediaPlayer!!.release()
      mMediaPlayer = null
    }
  }

  private fun updatePlaybackState() {
    if (mCallback == null) {
      return
    }
    val stateBuilder = PlaybackStateCompat.Builder().setActions(availableActions)

    stateBuilder.setState(
        mState, currentStreamPosition.toLong(), 1.0f, SystemClock.elapsedRealtime())
    mCallback.onPlaybackStatusChanged(stateBuilder.build())
  }

  interface Callback {
    fun onPlaybackStatusChanged(state: PlaybackStateCompat)
  }
}
