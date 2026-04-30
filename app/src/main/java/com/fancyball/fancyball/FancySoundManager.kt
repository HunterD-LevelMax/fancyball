package com.fancyball.fancyball

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool

class FancySoundManager(context: Context) {
    private val appContext = context.applicationContext
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .build()

    private val knockSound = soundPool.load(appContext, R.raw.knock, 1)
    private val getMoneySound = soundPool.load(appContext, R.raw.get_money, 1)
    private val selectValueSound = soundPool.load(appContext, R.raw.select_value, 1)

    private var musicPlayer: MediaPlayer? = MediaPlayer.create(appContext, R.raw.background_music)?.apply {
        isLooping = true
        setVolume(0.32f, 0.32f)
    }

    fun startMusic() {
        musicPlayer?.takeIf { !it.isPlaying }?.start()
    }

    fun pauseMusic() {
        musicPlayer?.takeIf { it.isPlaying }?.pause()
    }

    fun playKnock() {
        play(knockSound, 0.42f)
    }

    fun playGetMoney() {
        play(getMoneySound, 0.72f)
    }

    fun playSelectValue() {
        play(selectValueSound, 0.52f)
    }

    fun release() {
        soundPool.release()
        musicPlayer?.release()
        musicPlayer = null
    }

    private fun play(soundId: Int, volume: Float) {
        if (soundId != 0) {
            soundPool.play(soundId, volume, volume, 1, 0, 1f)
        }
    }
}
