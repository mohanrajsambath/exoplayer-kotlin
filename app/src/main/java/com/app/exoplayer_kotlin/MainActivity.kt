package com.app.exoplayer_kotlin

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.exo_playback_control_view.*

const val HLS_STATIC_URL = "https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8"
const val STATE_RESUME_WINDOW = "resumeWindow"
const val STATE_RESUME_POSITION = "resumePosition"
const val STATE_PLAYER_FULLSCREEN = "playerFullscreen"
const val STATE_PLAYER_PLAYING = "playerOnPlay"

class MainActivity : AppCompatActivity() {

    private lateinit var exoPlayer: SimpleExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory

    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var isFullscreen = false
    private var isPlayerPlaying = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dataSourceFactory = DefaultDataSourceFactory(this,
            Util.getUserAgent(this, "testapp"))

        initFullScreenButton()

        if (savedInstanceState != null) {
            currentWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW)
            playbackPosition = savedInstanceState.getLong(STATE_RESUME_POSITION)
            isFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN)
            isPlayerPlaying = savedInstanceState.getBoolean(STATE_PLAYER_PLAYING)
        }
    }

    private fun initPlayer(){
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        val videoSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(HLS_STATIC_URL))

        with(exoPlayer) {
            playWhenReady = isPlayerPlaying
            seekTo(currentWindow, playbackPosition)
            prepare(videoSource, false, false)
        }
        player_view.player = exoPlayer

        if (isFullscreen) {
            openFullscreen()
        }
    }

    private fun releasePlayer(){
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        currentWindow = exoPlayer.currentWindowIndex
        exoPlayer.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_RESUME_WINDOW, exoPlayer.currentWindowIndex)
        outState.putLong(STATE_RESUME_POSITION, exoPlayer.currentPosition)
        outState.putBoolean(STATE_PLAYER_FULLSCREEN, isFullscreen)
        outState.putBoolean(STATE_PLAYER_PLAYING, isPlayerPlaying)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initPlayer()
            if (player_view != null) player_view.onResume()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23) {
            initPlayer()
            if (player_view != null) player_view.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            if (player_view != null) player_view.onPause()
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            if (player_view != null) player_view.onPause()
            releasePlayer()
        }
    }

    override fun onBackPressed() {
        if(isFullscreen){
            closeFullscreen()
            return
        }
        super.onBackPressed()
    }

    // FULLSCREEN PART

    private fun initFullScreenButton(){
        exo_fullscreen_button.setOnClickListener {
            if (!isFullscreen) {
                openFullscreen()
            } else {
                closeFullscreen()
            }
        }
    }

    private fun openFullscreen(){
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        exo_fullscreen_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_fullscreen_shrink))
        player_view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorBlack))
        val params: LinearLayout.LayoutParams = player_view.layoutParams as LinearLayout.LayoutParams
        params.width = LinearLayout.LayoutParams.MATCH_PARENT
        params.height = LinearLayout.LayoutParams.MATCH_PARENT
        player_view.layoutParams = params
        supportActionBar?.hide()
        hideSystemUi()
        isFullscreen = true
    }

    private fun closeFullscreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        exo_fullscreen_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_fullscreen_expand))
        player_view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorWhite))
        val params: LinearLayout.LayoutParams = player_view.layoutParams as LinearLayout.LayoutParams
        params.width = LinearLayout.LayoutParams.MATCH_PARENT
        params.height = 0
        player_view.layoutParams = params
        supportActionBar?.show()
        player_view.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        isFullscreen = false
    }

    private fun hideSystemUi() {
        player_view?.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}
