package com.sunny.zyplayer

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.FullscreenButtonClickListener
import com.google.common.collect.ImmutableList
import com.sunny.zyplayer.bean.ZySubtitleBean


class ZyPlayerView : RelativeLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var player: ExoPlayer? = null

    private val playerView by lazy { PlayerView(context) }

    init {
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(playerView, layoutParams)
    }

    /**
     * 加载视频
     */
    fun setVideoUrl(videoUrl: String, isAutoPlay: Boolean) {
        setVideoUrl(videoUrl, null, isAutoPlay)
    }

    /**
     * 加载视频和字幕
     */
    fun setVideoUrl(videoUrl: String, subtitleBean: ZySubtitleBean?, isAutoPlay: Boolean) {
        val mediaItem = MediaItem.Builder()
        mediaItem.setUri(videoUrl)
        playerView.setShowRewindButton(false)
        playerView.setShowFastForwardButton(false)
        playerView.setShowPreviousButton(false)
        playerView.setFullscreenButtonClickListener {}
        playerView.setShowNextButton(false)
        val subtitleUri = subtitleBean?.uri
        if (subtitleUri != null) {
            MimeTypes.APPLICATION_AIT
            playerView.setShowSubtitleButton(true)
            val subtitleConfiguration = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                .setLanguage(subtitleBean.language)
                .setMimeType(subtitleBean.mimeType)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaItem.setSubtitleConfigurations(ImmutableList.of(subtitleConfiguration))
        }
        player?.setMediaItem(mediaItem.build())
        player?.prepare()
        player?.playWhenReady = isAutoPlay


    }

    fun setPlayer(player: ExoPlayer) {
        this.player = player
        playerView.player = player
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun setControllerVisibilityListener(listener: PlayerView.ControllerVisibilityListener) {
        playerView.setControllerVisibilityListener(listener)
    }

    fun setFullscreenButtonClickListener(listener: FullscreenButtonClickListener) {
        playerView.setFullscreenButtonClickListener(listener)
    }
}