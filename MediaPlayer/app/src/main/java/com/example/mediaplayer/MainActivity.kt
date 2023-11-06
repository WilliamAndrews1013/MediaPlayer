package com.example.mediaplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.AssetFileDescriptor
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mediaplayer.ui.theme.MediaPlayerTheme
import java.security.Provider.Service

class MainActivity : ComponentActivity() {

    private lateinit var songIds: ArrayList<Int>

    companion object {
        lateinit var musicList: ArrayList<Song>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediaPlayerTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("Android")
                }
            }
        }

        songIds = arrayListOf(R.raw.song1, R.raw.song2, R.raw.song3, R.raw.song4, R.raw.song5, R.raw.song6,
            R.raw.song7, R.raw.song8, R.raw.song9, R.raw.song10, R.raw.song11, R.raw.song12, R.raw.song13,
            R.raw.song14)

        musicList = arrayListOf()

        var mmr: MediaMetadataRetriever = MediaMetadataRetriever()
        var afd: AssetFileDescriptor
        var count = 0

        for(music in songIds) {
            afd = this.resources.openRawResourceFd(music)
            mmr.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            musicList.add(
                Song(
                    count,
                    music,
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION),
                    mmr.embeddedPicture
                )
            )

            count++
            afd.close();
        }
    }

    override fun onStart() {
        super.onStart()

        if(MyMediaService.started){
            Intent(this, MyMediaService::class.java).also {
                intent -> bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        } else {
            val intent = Intent(this, MyMediaService::class.java)
            startForegroundService(intent)
        }
    }

    private lateinit var mService: MyMediaService
    private var mBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService() */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(calssName: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MyMediaService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
            text = "Hello $name!",
            modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MediaPlayerTheme {
        Greeting("Android")
    }
}