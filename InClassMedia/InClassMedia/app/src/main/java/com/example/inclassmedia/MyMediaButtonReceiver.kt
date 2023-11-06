package com.example.inclassmedia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.view.KeyEvent
import androidx.media.session.MediaButtonReceiver

class MyMediaButtonReceiver: MediaButtonReceiver() {

    override fun onReceive(cxt: Context, intent: Intent) {

        val event: KeyEvent?

        if (Intent.ACTION_MEDIA_BUTTON != intent.action) {
            return
        }

        event = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_KEY_EVENT) as KeyEvent

        if (event == null) {
            return
        }

        when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PAUSE -> MyMediaService.self.playPause()
            KeyEvent.KEYCODE_MEDIA_PLAY -> MyMediaService.self.playPause()
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> MyMediaService.self.playPause()
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MyMediaService.self.prevNexSong(false)
            KeyEvent.KEYCODE_MEDIA_NEXT -> MyMediaService.self.prevNexSong(true)
        }
    }
}