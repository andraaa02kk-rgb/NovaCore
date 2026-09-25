package com.novacore.performance.audio

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class NovaAudioService: MediaSessionService(){companion object{const val PLAY="nova.play";const val PAUSE="nova.pause";const val STOP="nova.stop";const val SEEK="nova.seek"};private var player:ExoPlayer?=null;private var session:MediaSession?=null
 override fun onCreate(){super.onCreate();player=ExoPlayer.Builder(this).build();session=MediaSession.Builder(this,player!!).build()}
 override fun onStartCommand(i:Intent?,flags:Int,id:Int):Int{when(i?.action){PLAY->{i.data?.let{player?.setMediaItem(MediaItem.fromUri(it));player?.prepare()};player?.play()};PAUSE->player?.pause();STOP->{player?.stop();stopSelf()};SEEK->player?.seekTo(i.getLongExtra("ms",0))};return START_NOT_STICKY}
 override fun onGetSession(c:MediaSession.ControllerInfo)=session
 override fun onDestroy(){session?.release();player?.release();super.onDestroy()}
}
