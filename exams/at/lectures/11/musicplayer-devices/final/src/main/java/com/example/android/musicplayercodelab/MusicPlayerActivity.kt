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

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import androidx.core.content.ContextCompat

import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import java.util.ArrayList

/** An Activity to browse and play media.  */
class MusicPlayerActivity : AppCompatActivity() {

  private var mBrowserAdapter: BrowseAdapter? = null
  private var mPlayPause: ImageButton? = null
  private var mTitle: TextView? = null
  private var mSubtitle: TextView? = null
  private var mAlbumArt: ImageView? = null
  private var mPlaybackControls: ViewGroup? = null

  private var mCurrentMetadata: MediaMetadataCompat? = null
  private var mCurrentState: PlaybackStateCompat? = null

  private var mMediaBrowser: MediaBrowserCompat? = null

  private val mConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
    override fun onConnected() {
      mMediaBrowser!!.subscribe(mMediaBrowser!!.root, mSubscriptionCallback)
      try {
        val mediaController = MediaControllerCompat(
            this@MusicPlayerActivity, mMediaBrowser!!.sessionToken)
        updatePlaybackState(mediaController.playbackState)
        updateMetadata(mediaController.metadata)
        mediaController.registerCallback(mMediaControllerCallback)
        MediaControllerCompat.setMediaController(
            this@MusicPlayerActivity, mediaController)
      } catch (e: RemoteException) {
        throw RuntimeException(e)
      }

    }
  }

  // Receive callbacks from the MediaController. Here we update our state such as which queue
  // is being shown, the current title and description and the PlaybackState.
  private val mMediaControllerCallback = object : MediaControllerCompat.Callback() {
    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
      updateMetadata(metadata)
      mBrowserAdapter!!.notifyDataSetChanged()
    }

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
      updatePlaybackState(state)
      mBrowserAdapter!!.notifyDataSetChanged()
    }

    override fun onSessionDestroyed() {
      updatePlaybackState(null)
      mBrowserAdapter!!.notifyDataSetChanged()
    }
  }

  private val mSubscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
    override fun onChildrenLoaded(
        parentId: String, children: List<MediaBrowserCompat.MediaItem>) {
      onMediaLoaded(children)
    }
  }

  private val mPlaybackButtonListener = View.OnClickListener {
    val state = if (mCurrentState == null)
      PlaybackStateCompat.STATE_NONE
    else
      mCurrentState!!.state
    if (state == PlaybackStateCompat.STATE_PAUSED
        || state == PlaybackStateCompat.STATE_STOPPED
        || state == PlaybackStateCompat.STATE_NONE) {

      if (mCurrentMetadata == null) {
        mCurrentMetadata = MusicLibrary.getMetadata(
            this@MusicPlayerActivity,
            MusicLibrary.mediaItems.get(0).getMediaId()!!)
        updateMetadata(mCurrentMetadata)
      }

      MediaControllerCompat.getMediaController(this@MusicPlayerActivity)
          .transportControls
          .playFromMediaId(
              mCurrentMetadata!!.description.mediaId, null)
    } else {
      MediaControllerCompat.getMediaController(this@MusicPlayerActivity)
          .transportControls
          .pause()
    }
  }

  private fun onMediaLoaded(media: List<MediaBrowserCompat.MediaItem>) {
    mBrowserAdapter!!.clear()
    mBrowserAdapter!!.addAll(media)
    mBrowserAdapter!!.notifyDataSetChanged()
  }

  private fun onMediaItemSelected(item: MediaBrowserCompat.MediaItem) {
    if (item.isPlayable) {
      MediaControllerCompat.getMediaController(this)
          .transportControls
          .playFromMediaId(item.mediaId, null)
    }
  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_player)
    title = getString(R.string.app_name)
    setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)

    mBrowserAdapter = BrowseAdapter(this)

    val listView = findViewById<View>(R.id.list_view) as ListView
    listView.adapter = mBrowserAdapter
    listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
      val item = mBrowserAdapter!!.getItem(position)
      onMediaItemSelected(item!!)
    }

    // Playback controls configuration:
    mPlaybackControls = findViewById<View>(R.id.playback_controls) as ViewGroup
    mPlayPause = findViewById<View>(R.id.play_pause) as ImageButton
    mPlayPause!!.isEnabled = true
    mPlayPause!!.setOnClickListener(mPlaybackButtonListener)

    mTitle = findViewById<View>(R.id.title) as TextView
    mSubtitle = findViewById<View>(R.id.artist) as TextView
    mAlbumArt = findViewById<View>(R.id.album_art) as ImageView
  }

  public override fun onStart() {
    super.onStart()

    mMediaBrowser = MediaBrowserCompat(
        this,
        ComponentName(this, MusicService::class.java!!),
        mConnectionCallback, null)
    mMediaBrowser!!.connect()
  }

  public override fun onStop() {
    super.onStop()
    val controller = MediaControllerCompat.getMediaController(this)
    controller?.unregisterCallback(mMediaControllerCallback)
    if (mMediaBrowser != null && mMediaBrowser!!.isConnected) {
      if (mCurrentMetadata != null) {
        mMediaBrowser!!.unsubscribe(mCurrentMetadata!!.description.mediaId!!)
      }
      mMediaBrowser!!.disconnect()
    }
  }

  private fun updatePlaybackState(state: PlaybackStateCompat?) {
    mCurrentState = state
    if (state == null
        || state.state == PlaybackStateCompat.STATE_PAUSED
        || state.state == PlaybackStateCompat.STATE_STOPPED) {
      mPlayPause!!.setImageDrawable(
          ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_36dp))
    } else {
      mPlayPause!!.setImageDrawable(
          ContextCompat.getDrawable(this, R.drawable.ic_pause_black_36dp))
    }
    mPlaybackControls!!.visibility = if (state == null) View.GONE else View.VISIBLE
  }

  private fun updateMetadata(metadata: MediaMetadataCompat?) {
    mCurrentMetadata = metadata
    mTitle!!.text = if (metadata == null) "" else metadata.description.title
    mSubtitle!!.text = if (metadata == null) "" else metadata.description.subtitle
    mAlbumArt!!.setImageBitmap(
        if (metadata == null)
          null
        else
          MusicLibrary.getAlbumBitmap(
              this, metadata.description.mediaId!!))
    mBrowserAdapter!!.notifyDataSetChanged()
  }

  // An adapter for showing the list of browsed MediaItem's
  private inner class BrowseAdapter(context: Activity) : ArrayAdapter<MediaBrowserCompat.MediaItem>(context, R.layout.media_list_item, ArrayList()) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
      val item = getItem(position)
      var itemState = MediaItemViewHolder.STATE_NONE
      if (item!!.isPlayable) {
        val itemMediaId = item.description.mediaId
        var playbackState = PlaybackStateCompat.STATE_NONE
        if (mCurrentState != null) {
          playbackState = mCurrentState!!.state
        }
        if (mCurrentMetadata != null && itemMediaId == mCurrentMetadata!!.description.mediaId) {
          if (playbackState == PlaybackStateCompat.STATE_PLAYING || playbackState == PlaybackStateCompat.STATE_BUFFERING) {
            itemState = MediaItemViewHolder.STATE_PLAYING
          } else if (playbackState != PlaybackStateCompat.STATE_ERROR) {
            itemState = MediaItemViewHolder.STATE_PAUSED
          }
        }
      }
      return MediaItemViewHolder.setupView(
          context as Activity, convertView, parent, item.description, itemState)
    }
  }
}
