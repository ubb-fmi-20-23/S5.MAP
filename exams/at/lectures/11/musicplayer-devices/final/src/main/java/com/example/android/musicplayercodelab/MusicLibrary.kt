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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import java.util.ArrayList
import java.util.HashMap
import java.util.TreeMap

internal object MusicLibrary {

  private val music = TreeMap<String, MediaMetadataCompat>()
  private val albumRes = HashMap<String, Int>()
  private val musicRes = HashMap<String, Int>()

  val root: String
    get() = "root"

  val mediaItems: List<MediaBrowserCompat.MediaItem>
    get() {
      val result = ArrayList<MediaBrowserCompat.MediaItem>()
      for (metadata in music.values) {
        result.add(
            MediaBrowserCompat.MediaItem(
                metadata.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
      }
      return result
    }

  init {
    createMediaMetadataCompat(
        "Jazz_In_Paris",
        "Jazz in Paris",
        "Media Right Productions",
        "Jazz & Blues",
        "Jazz",
        103,
        R.raw.jazz_in_paris,
        R.drawable.album_jazz_blues,
        "album_jazz_blues")
    createMediaMetadataCompat(
        "The_Coldest_Shoulder",
        "The Coldest Shoulder",
        "The 126ers",
        "Youtube Audio Library Rock 2",
        "Rock",
        160,
        R.raw.the_coldest_shoulder,
        R.drawable.album_youtube_audio_library_rock_2,
        "album_youtube_audio_library_rock_2")
  }

  fun getSongUri(mediaId: String): String {
    return "android.resource://" + BuildConfig.APPLICATION_ID + "/" + getMusicRes(mediaId)
  }

  private fun getAlbumArtUri(albumArtResName: String): String {
    return "android.resource://" + BuildConfig.APPLICATION_ID + "/drawable/" + albumArtResName
  }

  private fun getMusicRes(mediaId: String): Int {
    return if (musicRes.containsKey(mediaId)) musicRes[mediaId]!! else 0
  }

  private fun getAlbumRes(mediaId: String): Int {
    return if (albumRes.containsKey(mediaId)) albumRes[mediaId]!! else 0
  }

  fun getAlbumBitmap(ctx: Context, mediaId: String): Bitmap {
    return BitmapFactory.decodeResource(ctx.resources, MusicLibrary.getAlbumRes(mediaId))
  }

  fun getPreviousSong(currentMediaId: String): String? {
    var prevMediaId: String? = music.lowerKey(currentMediaId)
    if (prevMediaId == null) {
      prevMediaId = music.firstKey()
    }
    return prevMediaId
  }

  fun getNextSong(currentMediaId: String): String? {
    var nextMediaId: String? = music.higherKey(currentMediaId)
    if (nextMediaId == null) {
      nextMediaId = music.firstKey()
    }
    return nextMediaId
  }

  fun getMetadata(ctx: Context, mediaId: String): MediaMetadataCompat {
    val metadataWithoutBitmap = music[mediaId]
    val albumArt = getAlbumBitmap(ctx, mediaId)

    // Since MediaMetadataCompat is immutable, we need to create a copy to set the album art.
    // We don't set it initially on all items so that they don't take unnecessary memory.
    val builder = MediaMetadataCompat.Builder()
    for (key in arrayOf(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, MediaMetadataCompat.METADATA_KEY_ALBUM, MediaMetadataCompat.METADATA_KEY_ARTIST, MediaMetadataCompat.METADATA_KEY_GENRE, MediaMetadataCompat.METADATA_KEY_TITLE)) {
      builder.putString(key, metadataWithoutBitmap!!.getString(key))
    }
    builder.putLong(
        MediaMetadataCompat.METADATA_KEY_DURATION,
        metadataWithoutBitmap!!.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
    builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
    return builder.build()
  }

  private fun createMediaMetadataCompat(
      mediaId: String,
      title: String,
      artist: String,
      album: String,
      genre: String,
      duration: Long,
      musicResId: Int,
      albumArtResId: Int,
      albumArtResName: String) {
    music[mediaId] = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration * 1000)
        .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
        .putString(
            MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
            getAlbumArtUri(albumArtResName))
        .putString(
            MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
            getAlbumArtUri(albumArtResName))
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        .build()
    albumRes[mediaId] = albumArtResId
    musicRes[mediaId] = musicResId
  }
}
