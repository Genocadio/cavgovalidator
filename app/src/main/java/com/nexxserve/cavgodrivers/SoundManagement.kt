package com.nexxserve.cavgodrivers

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.util.Log

@SuppressLint("ObsoleteSdkInt")
class SoundManagement private constructor(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()

    init {
        // Initialize SoundPool
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attributes)
                .build()
        } else {
            soundPool = SoundPool(5, android.media.AudioManager.STREAM_MUSIC, 0)
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: SoundManagement? = null

        // Singleton instance access
        fun getInstance(context: Context): SoundManagement {
            return INSTANCE ?: synchronized(this) {
                val instance = SoundManagement(context)
                INSTANCE = instance
                instance
            }
        }
    }

    // Load a sound into the SoundPool
    fun loadSound(resourceId: Int) {
        if (!soundMap.containsKey(resourceId)) {
            val soundId = soundPool?.load(context, resourceId, 1) ?: return
            soundMap[resourceId] = soundId
            Log.d("SoundManagement", "Loaded sound with resourceId: $resourceId, soundId: $soundId")
        } else {
            Log.d("SoundManagement", "Sound already loaded with resourceId: $resourceId")
        }
    }

    // Play the sound by its resource ID
    fun playSound(resourceId: Int) {
        soundMap[resourceId]?.let {
            val result = soundPool?.play(it, 1f, 1f, 0, 0, 1f)
            if (result == 0) {
                Log.e("SoundManagement", "Sound failed to play for resourceId: $resourceId")
            } else {
                Log.d("SoundManagement", "Playing sound with resourceId: $resourceId")
            }
        } ?: Log.e("SoundManagement", "Sound not loaded for resourceId: $resourceId")
    }

    // Release resources when done
    fun release() {
        soundPool?.release()
    }
}
