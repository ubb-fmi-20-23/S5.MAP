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

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class MusicService : MediaBrowserServiceCompat() {

  private var mSession: MediaSessionCompat? = null
  private var mPlayback: PlaybackManager? = null

  internal val mCallback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
      mSession!!.isActive = true
      val metadata = MusicLibrary.getMetadata(this@MusicService, mediaId!!)
      mSession!!.setMetadata(metadata)
      mPlayback!!.play(metadata)
    }

    override fun onPlay() {
      if (mPlayback!!.currentMediaId != null) {
        onPlayFromMediaId(mPlayback!!.currentMediaId, null)
      }
    }

    override fun onPause() {
      mPlayback!!.pause()
    }

    override fun onStop() {
      stopSelf()
    }

    override fun onSkipToNext() {
      onPlayFromMediaId(
          MusicLibrary.getNextSong(mPlayback!!.currentMediaId!!), null)
    }

    override fun onSkipToPrevious() {
      onPlayFromMediaId(
          MusicLibrary.getPreviousSong(mPlayback!!.currentMediaId!!), null)
    }
  }

  override fun onCreate() {
    super.onCreate()

    // Start a new MediaSession
    mSession = MediaSessionCompat(this, "MusicService")
    mSession!!.setCallback(mCallback)
    mSession!!.setFlags(
        MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
    sessionToken = mSession!!.sessionToken

    val mediaNotificationManager = MediaNotificationManager(this)

    mPlayback = PlaybackManager(
        this,
        object :PlaybackManager.Callback {
          override fun onPlaybackStatusChanged(state: PlaybackStateCompat) {
            mSession!!.setPlaybackState(state)
            mediaNotificationManager.update(
                mPlayback!!.currentMedia, state, sessionToken!!)
          }
        })
  }

  override fun onDestroy() {
    mPlayback!!.stop()
    mSession!!.release()
  }

  override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): MediaBrowserServiceCompat.BrowserRoot? {
    return MediaBrowserServiceCompat.BrowserRoot(MusicLibrary.root, null)
  }

  override fun onLoadChildren(
      parentMediaId: String, result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>) {
    result.sendResult(MusicLibrary.mediaItems)
  }
}
