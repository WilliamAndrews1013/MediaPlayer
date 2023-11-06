package com.example.mediaplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import kotlin.concurrent.thread

private const val ONGOING_NOTIFICATION = 1
class MyMediaService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
AudioManager.OnAudioFocusChangeListener{

    companion object{
        var started = false
        lateinit var self: MyMediaService
    }

    var mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        setOnPreparedListener { start() }
    }

    private var isPlaying: Boolean = false
    private lateinit var musicList: ArrayList<Song>
    private lateinit var mediaSession: MediaSessionCompat
    private var position: Int = 0
    private lateinit var runnable: Runnable
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaMetadata: MediaMetadataCompat

    override fun onCreate() {
        super.onCreate()
        self = this
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mediaSession = MediaSessionCompat(applicationContext, "MEDIA_SESSION")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        position = intent.getIntExtra("position", 0)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel()
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        startForeground(ONGOING_NOTIFICATION, getNotification().build())

        thread {
            createMediaPlayer()
        }

        return START_STICKY
    }

    private fun createMediaPlayer():Boolean{

        musicList = MainActivity.musicList

        try {
            mediaPlayer!!.reset()
            mediaPlayer = MediaPlayer.create(this, musicList[position].id)
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mediaPlayer.start()
            isPlaying = true
            notificationManager.notify(ONGOING_NOTIFICATION, getNotification().build())
            mediaPlayer.setOnCompletionListener(this)
            return true
        }catch (e:Exception){

            return false
        }

        return false
    }

    private fun getNotification() : NotificationCompat.Builder {

        // Create an Intent for the activity you want to start
        val resultIntent = Intent(this, MainActivity::class.java)

        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0,
                PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = NotificationCompat.Builder(applicationContext, "music_id").apply {
            setContentTitle("song")
            setContentText("text")
            setContentIntent(resultPendingIntent)
            setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    applicationContext,
                    PlaybackStateCompat.ACTION_STOP
                ))
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.ic_music_note)


            addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_previous,
                    "Previous",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
                    )
                )
            )
            addAction(
                NotificationCompat.Action(
                    if(isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    "Pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext,
                        if (isPlaying) PlaybackStateCompat.ACTION_PLAY else PlaybackStateCompat.ACTION_PAUSE
                    )
                )
            )
            addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_next,
                    "Next",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            )
            setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(1)
            )
        }
        return builder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(){
        val importance = NotificationManager.IMPORTANCE_NONE
        val mChannel = NotificationChannel("music_id", getString(R.string.channel_name), importance)
        mChannel.description = getString(R.string.channel_description)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mediaPlayer.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(0.3f, 0.3f)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (mediaPlayer != null) {
                    if (!mediaPlayer.isPlaying()) {
                        mediaPlayer.start()
                    }
                    mediaPlayer.setVolume(1.0f, 1.0f)
                }
            }
        }
    }

    override fun onCompletion(p0: MediaPlayer?) {
        setPosition(true)
        createMediaPlayer()
        updateMediaMetadata()
        notificationManager.notify(ONGOING_NOTIFICATION, getNotification().build())
    }

    private fun setPosition(increment:Boolean){
        if(increment) {
            if(musicList.size - 1 == position){
                position = 0
            }else{
                position++
            }
        }else{
            if(position == 0){
                position = musicList.size - 1
            }else{
                position--
            }
        }
    }

    fun playPause():Boolean{
        if(isPlaying){
            mediaPlayer.pause()
            isPlaying = false
        }else{
            mediaPlayer.start()
            isPlaying = true
        }
        notificationManager.notify(ONGOING_NOTIFICATION, getNotification().build())
        return isPlaying
    }

    fun prevNexSong(increment:Boolean):Int{
        setPosition(increment)
        updateMediaMetadata()
        notificationManager.notify(ONGOING_NOTIFICATION, getNotification().build())
        return if(createMediaPlayer()) position
        else 0
    }

    private fun updateMediaMetadata(){

        //extract cover as bitmap
        val coverByteArray = musicList[position].cover
        var bitmap: Bitmap? = null
        if(coverByteArray != null) {
            bitmap = BitmapFactory.decodeByteArray(coverByteArray, 0, coverByteArray!!.size)
        }

        //update metadata
        mediaMetadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, musicList[position].title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, musicList[position].artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, musicList[position].album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, musicList[position].durationMS!!)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
            .build()

        mediaSession.setMetadata(mediaMetadata)
    }

    override fun onPrepared(mediaPlayer: MediaPlayer) {
        mediaPlayer.start()
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }


    inner class LocalBinder: Binder(){
        fun getService(): MyMediaService = this@MyMediaService
    }

}