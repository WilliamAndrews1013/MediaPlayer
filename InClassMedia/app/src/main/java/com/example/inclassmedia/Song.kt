package com.example.inclassmedia

import java.io.Serializable
import java.util.concurrent.TimeUnit

class Song : Serializable {
    var number:Int
    var id:Int
    var artist: String? = null
    var title: String? = null
    var album: String? = null
    var duration: String? = null
    var durationMS: Long? = null
    var cover: ByteArray? = null

    constructor(number:Int, id:Int, artist:String?, title:String?, album:String?,
                durationMS:String?, cover:ByteArray?){
        this.number=number
        this.id=id
        this.artist=artist
        this.title=title
        this.album=album
        this.duration=formatDuration(durationMS)
        if (durationMS != null) {
            this.durationMS=durationMS.toLong()
        }
        this.cover=cover
    }

    public fun formatDuration(duration:String?):String{
        if(duration != null){
            val durationLong:Long = duration.toLong()
            val minutes = TimeUnit.MINUTES.convert(durationLong, TimeUnit.MILLISECONDS)
            val seconds = TimeUnit.SECONDS.convert(durationLong, TimeUnit.MILLISECONDS) -
                    minutes*TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES)
            return String.format("%2d:%02d", minutes, seconds)
        }else{
            return "0:00"
        }
    }
}