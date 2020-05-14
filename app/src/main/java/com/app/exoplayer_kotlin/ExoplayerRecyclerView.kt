package com.app.exoplayer_kotlin

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class ExoplayerRecyclerView : RecyclerView {

    // ui
    private var thumbnail: ImageView? = null
    private var progressBar: ProgressBar? = null
    private var viewHolderParent: View? = null
    private var frameLayout: FrameLayout? = null
    private lateinit var videoSurfaceView: PlayerView
    private var videoPlayer: SimpleExoPlayer? = null

    // vars
    private var isVideoViewAdded = false

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    private fun init(context: Context) {
        videoSurfaceView = PlayerView(context)
        videoSurfaceView.videoSurfaceView
        videoSurfaceView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

        // 2. Create the player
        videoPlayer = SimpleExoPlayer.Builder(context).build()
        // Bind the player to the view.
        videoSurfaceView.useController = false
        videoSurfaceView.player = videoPlayer
        videoPlayer?.volume = 0f
        videoPlayer?.repeatMode = Player.REPEAT_MODE_ONE

        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) {
                    if (thumbnail != null) { // show the old thumbnail
                        thumbnail?.visibility = View.VISIBLE
                    }
                }
            }

        })
        addOnChildAttachStateChangeListener(object : OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.setOnLongClickListener { v ->
                    playVideo(v)
                    true
                }
            }
            override fun onChildViewDetachedFromWindow(view: View) {
                if (viewHolderParent != null && viewHolderParent == view) {
                    resetVideoView()
                }
            }
        })
        videoPlayer?.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        progressBar?.visibility = View.VISIBLE
                    }
                    Player.STATE_READY -> {
                        progressBar?.visibility = View.GONE
                        if (!isVideoViewAdded) {
                            addVideoView()
                        }
                    }
                    else -> {}
                }
            }
        })
    }

    fun playVideo(view: View) {
        // video is already playing so return
        if (viewHolderParent != null && viewHolderParent == view) {
            return
        } else {
            // remove any old surface views from previously playing videos
            progressBar?.visibility = View.GONE
            resetVideoView()
        }

        // set the position of the list-item that is to be played
        if (!::videoSurfaceView.isInitialized) {
            return
        }

        val holder = view.tag as ExoplayerVideoAdapter.VideoViewHolder? ?: return
        thumbnail = holder.imageView
        progressBar = holder.videoProgressbar
        viewHolderParent = holder.itemView
        frameLayout = holder.videoContainer
        videoSurfaceView.player = videoPlayer
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory( context,
            Util.getUserAgent(context, "RecyclerView VideoPlayer")
        )

        val mediaUrl: String? = holder.videoPreview
        if (mediaUrl != null) {
            val videoSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(mediaUrl))
            videoPlayer?.prepare(videoSource)
            videoPlayer?.playWhenReady = true
        }
    }

    // Remove the old player
    private fun removeVideoView(videoView: PlayerView?) {
        val parent = videoView?.parent as ViewGroup?
        val index = parent?.indexOfChild(videoView)
        if (index != null && index >= 0) {
            parent.removeViewAt(index)
            isVideoViewAdded = false
        }
    }

    private fun addVideoView() {
        frameLayout!!.addView(videoSurfaceView)
        isVideoViewAdded = true
        videoSurfaceView.requestFocus()
        videoSurfaceView.visibility = View.VISIBLE
        videoSurfaceView.alpha = 1f
        thumbnail?.visibility = View.GONE
    }

    private fun resetVideoView() {
        if (isVideoViewAdded) {
            removeVideoView(videoSurfaceView)
            progressBar?.visibility = View.INVISIBLE
            videoSurfaceView.visibility = View.INVISIBLE
            thumbnail?.visibility = View.VISIBLE
        }
    }

    fun releasePlayer() {
        if (videoPlayer != null) {
            videoPlayer?.release()
            videoPlayer = null
        }
        resetVideoView()
        viewHolderParent = null
    }

    fun createPlayer() {
        if(videoPlayer == null) {
            init(context)
        }
    }
}
