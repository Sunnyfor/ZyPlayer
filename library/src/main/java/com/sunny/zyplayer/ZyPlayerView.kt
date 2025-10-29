package com.sunny.zyplayer

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import com.google.common.collect.ImmutableList
import com.sunny.zyplayer.bean.ZyVideoBean
import com.sunny.zyplayer.databinding.ZyLayoutPlayerViewBinding
import com.sunny.zyplayer.widget.ListViewPopupWindow
import com.sunny.zyplayer.widget.VolumeViewPopupWindow
import java.util.Formatter
import java.util.Locale


class ZyPlayerView : ConstraintLayout, Player.Listener, TimeBar.OnScrubListener, OnClickListener {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var isFullScreen = false

    private var scrubbing = false

    private val formatBuilder by lazy {
        StringBuilder()
    }
    private val formatter by lazy {
        Formatter(formatBuilder, Locale.getDefault())
    }

    private var currentWindowOffset: Long = 0

    private var timeBarMinUpdateIntervalMs = 200

    private val player by lazy { ExoPlayer.Builder(context).build() }

    private val updateProgressAction by lazy {
        Runnable { updateProgress() }
    }

    private val hideControlViewAction by lazy {
        Runnable {
            hideControlView()
        }
    }

    private val viewBinding: ZyLayoutPlayerViewBinding by lazy {
        ZyLayoutPlayerViewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    private var controllerVisibilityListener: ControllerVisibilityListener? = null
    private var onFullScreenModeChangedListener: OnFullScreenModeChangedListener? = null

    private val playList = arrayListOf<ZyVideoBean>()
    private val mediaItemList = arrayListOf<MediaItem>()

    private val volumeView by lazy {
        VolumeViewPopupWindow(context)
    }

    private val listView by lazy {
        ListViewPopupWindow(context, player, playList)
    }

    private var controllerShowTimeoutMs = 5000L

    private var isAnimating = false


    init {
        viewBinding.playerView.player = player

        player.addListener(this)

        viewBinding.playerView.setOnClickListener(this)

        val drawable = ContextCompat.getDrawable(context, R.drawable.zy_player_controls_thumb)

        val layerDrawable = drawable as LayerDrawable

        val innerShape = layerDrawable.findDrawableByLayerId(R.id.inner_circle_layer) as? GradientDrawable

        innerShape?.setColor(ColorConfig.colorTheme)

        try {
            val scrubberDrawable = layerDrawable
            val field = DefaultTimeBar::class.java.getDeclaredField("scrubberDrawable")
            field.isAccessible = true
            field.set(viewBinding.playerControl.timeBar, scrubberDrawable)
            viewBinding.playerControl.timeBar.invalidate() // 强制重绘
        } catch (e: Exception) {
            e.printStackTrace()
        }


        viewBinding.playerControl.timeBar.setPlayedColor(ColorConfig.colorTheme)
        viewBinding.playerControl.timeBar.addListener(this)

        viewBinding.playerControl.ivPlay.setOnClickListener(this)

        viewBinding.playerControl.btnForward.setOnClickListener(this)

        viewBinding.playerControl.ivFullscreen.setOnClickListener(this)

        viewBinding.playerControl.ivVolume.setOnClickListener(this)

        viewBinding.playerControl.ivList.setOnClickListener(this)

        setControllerShowTimeoutMs(controllerShowTimeoutMs)
    }

    /**
     * 加载视频
     */
    fun setVideoData(videoBean: ZyVideoBean, isAutoPlay: Boolean) {
        setVideoData(arrayListOf(videoBean), isAutoPlay)
    }


    fun setVideoData(data: List<ZyVideoBean>, isAutoPlay: Boolean) {
        playList.clear()
        mediaItemList.clear()
        playList.addAll(data)

        viewBinding.playerControl.ivList.visibility = if (data.size > 1) {
            View.VISIBLE
        } else {
            View.GONE
        }

        data.forEach {
            val mediaItem = MediaItem.Builder()
            mediaItem.setUri(it.uri)
            it.subtitle?.let { subtitle ->
                val subtitleConfiguration = MediaItem.SubtitleConfiguration.Builder(subtitle.uri)
                    .setLanguage(subtitle.language)
                    .setMimeType(subtitle.mimeType)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItem.setSubtitleConfigurations(ImmutableList.of(subtitleConfiguration))
            }
            mediaItemList.add(mediaItem.build())
        }
        try {
            player.setMediaItems(mediaItemList)
            player.prepare()
            player.playWhenReady = isAutoPlay
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }


    fun release() {
        player.release()
    }


    fun setControllerVisibilityListener(listener: ControllerVisibilityListener) {
        this.controllerVisibilityListener = listener
    }

    fun setFullScreenModeChangedListener(listener: OnFullScreenModeChangedListener) {
        this.onFullScreenModeChangedListener = listener
    }


    fun setControllerShowTimeoutMs(controllerShowTimeoutMs: Long) {
        showControlView()
        this.controllerShowTimeoutMs = controllerShowTimeoutMs
        val layoutParams = viewBinding.playerView.layoutParams as LayoutParams
        if (controllerShowTimeoutMs > 0) {
            layoutParams.bottomToTop = LayoutParams.UNSET
            layoutParams.bottomToBottom = LayoutParams.PARENT_ID
        } else {
            layoutParams.bottomToBottom = LayoutParams.UNSET
            layoutParams.bottomToTop = R.id.playerControl
        }
        updatePlayPauseButton()
    }


    fun setFullScreen(isFullScreen: Boolean) {
        this@ZyPlayerView.isFullScreen = isFullScreen
        val res = if (isFullScreen) {
            R.drawable.zy_player_controls_fullscreen_exit

        } else {
            R.drawable.zy_player_controls_fullscreen_enter
        }
        viewBinding.playerControl.ivFullscreen.setImageResource(res)
        hidePopupWindow()
        onFullScreenModeChangedListener?.onFullScreenModeChanged(isFullScreen)
    }


    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_AVAILABLE_COMMANDS_CHANGED
            )
        ) {
            updatePlayPauseButton()

        }

        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_AVAILABLE_COMMANDS_CHANGED
            )
        ) {
            updateProgress()
        }

        if (events.containsAny(
                Player.EVENT_POSITION_DISCONTINUITY,
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_AVAILABLE_COMMANDS_CHANGED
            )
        ) {
            updateTimeline()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        if (playbackState == Player.STATE_BUFFERING) {
            viewBinding.progressBar.visibility = View.VISIBLE
            viewBinding.tvPlayerError.visibility = View.GONE
        } else {
            viewBinding.progressBar.visibility = View.GONE
        }
    }


    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        viewBinding.tvPlayerError.visibility = View.VISIBLE
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        listView.updateIndex(player.currentMediaItemIndex)
    }


    private fun updatePlayPauseButton() {
        removeCallbacks(hideControlViewAction)
        val shouldShowPlayButton = Util.shouldShowPlayButton(player)
        val drawableRes: Int = if (shouldShowPlayButton) {
            if (controllerShowTimeoutMs > 0) {
                showControlView()
            }
            R.drawable.zy_player_controls_play
        } else {
            if (controllerShowTimeoutMs > 0) {
                postDelayed(hideControlViewAction, controllerShowTimeoutMs)
            }
            R.drawable.zy_player_controls_pause
        }
        viewBinding.playerControl.ivPlay.setImageResource(drawableRes)
    }

    override fun onClick(v: View?) {
        when (v) {
            viewBinding.playerView -> {
                if (listView.isShowing) {
                    listView.dismiss()
                    return
                }
                val shouldShowPlayButton = Util.shouldShowPlayButton(player)
                if (!shouldShowPlayButton) {
                    player.pause()
                } else {
                    player.play()
                }
            }

            viewBinding.playerControl.ivPlay -> {
                Util.handlePlayPauseButtonAction(player)
            }

            viewBinding.playerControl.btnForward -> {
                if (player.playbackState != Player.STATE_ENDED && player.isCommandAvailable(Player.COMMAND_SEEK_FORWARD)) {
                    player.seekForward()
                }
            }

            viewBinding.playerControl.ivFullscreen -> {
                setFullScreen(!isFullScreen)
            }

            viewBinding.playerControl.ivVolume -> {
                if (volumeView.isShowing) {
                    volumeView.dismiss()
                } else {
                    hidePopupWindow()
                    val y = (viewBinding.playerControl.root.height + volumeView.height)
                    val dp10 = resources.getDimension(com.sunny.zy.R.dimen.dp_10).toInt()
                    volumeView.showAsDropDown(viewBinding.playerControl.root, width - volumeView.width - dp10, -y)
                }
            }

            viewBinding.playerControl.ivList -> {
                if (listView.isShowing) {
                    listView.dismiss()
                } else {
                    hidePopupWindow()
                    val y = (viewBinding.playerControl.root.height + listView.height)
                    val dp10 = resources.getDimension(com.sunny.zy.R.dimen.dp_10).toInt()
                    listView.showAsDropDown(viewBinding.playerControl.root, width - listView.width - dp10, -y)
                }
            }
        }
    }


    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        scrubbing = true
        viewBinding.playerControl.tvPosition.text = Util.getStringForTime(formatBuilder, formatter, position)
    }

    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        viewBinding.playerControl.tvPosition.text = Util.getStringForTime(formatBuilder, formatter, position)
    }

    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        scrubbing = false
        if (!canceled) {
            seekToTimeBarPosition(player, position)
        }
    }


    private fun seekToTimeBarPosition(player: Player, positionMs: Long) {
        if (player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
            player.seekTo(positionMs)
        }
        updateProgress()
    }


    private fun updateProgress() {
        if (visibility != View.VISIBLE) {
            return
        }
        var position: Long = 0
        var bufferedPosition: Long = 0
        if (player.isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)) {
            position = currentWindowOffset + player.contentPosition
            bufferedPosition = currentWindowOffset + player.contentBufferedPosition
        }
        if (!scrubbing) {
            viewBinding.playerControl.tvPosition.text = Util.getStringForTime(formatBuilder, formatter, position)
        }

        viewBinding.playerControl.timeBar.setPosition(position)
        viewBinding.playerControl.timeBar.setBufferedPosition(bufferedPosition)

        // Cancel any pending updates and schedule a new one if necessary.
        removeCallbacks(updateProgressAction)
        val playbackState = player.playbackState
        if (player.isPlaying) {
            var mediaTimeDelayMs = viewBinding.playerControl.timeBar.preferredUpdateDelay
            // Limit delay to the start of the next full second to ensure position display is smooth.
            val mediaTimeUntilNextFullSecondMs = 1000 - position % 1000
            mediaTimeDelayMs = mediaTimeDelayMs.coerceAtMost(mediaTimeUntilNextFullSecondMs)

            // Calculate the delay until the next update in real time, taking playback speed into account.
            val playbackSpeed = player.playbackParameters.speed
            var delayMs = if (playbackSpeed > 0) (mediaTimeDelayMs / playbackSpeed).toLong() else 1000L

            // Constrain the delay to avoid too frequent / infrequent updates.
            delayMs = Util.constrainValue(delayMs, timeBarMinUpdateIntervalMs.toLong(), 1000L)
            postDelayed(updateProgressAction, delayMs)
        } else if (playbackState != Player.STATE_ENDED && playbackState != Player.STATE_IDLE) {
            postDelayed(updateProgressAction, 1000L)
        }
    }


    private fun updateTimeline() {
        currentWindowOffset = 0
        var durationUs: Long = 0
        if (player.isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)) {
            val playerDurationMs = player.contentDuration
            if (playerDurationMs != C.TIME_UNSET) {
                durationUs = Util.msToUs(playerDurationMs)
            }
        }
        val durationMs = Util.usToMs(durationUs)
        viewBinding.playerControl.tvDuration.text = Util.getStringForTime(formatBuilder, formatter, durationMs)
        viewBinding.playerControl.timeBar.setDuration(durationMs)
        updateProgress()
    }

    private fun showControlView() {
        val bottomView = viewBinding.playerControl.root
        if (bottomView.translationY != 0f && !isAnimating) {
            isAnimating = true
            val animate = bottomView.animate()
                .translationYBy((-bottomView.height).toFloat())
                .setDuration(500) // 设置动画持续时间
                .withEndAction {
                    isAnimating = false
                }
            animate.start()
            controllerVisibilityListener?.onVisibilityChanged(true)
        }
    }

    private fun hideControlView() {
        val bottomView = viewBinding.playerControl.root
        if (bottomView.translationY.toInt() != bottomView.height && !isAnimating) {
            hidePopupWindow()
            isAnimating = true
            val animate = bottomView.animate()
                .translationYBy(bottomView.height.toFloat())
                .setDuration(300) // 设置动画持续时间
                .withEndAction {
                    isAnimating = false
                    if (controllerShowTimeoutMs.toInt() == 0) {
                        showControlView()
                    }
                }
            animate.start()
            controllerVisibilityListener?.onVisibilityChanged(false)
        }
    }

    private fun hidePopupWindow() {
        if (volumeView.isShowing) {
            volumeView.dismiss()
        }
        if (listView.isShowing) {
            listView.dismiss()
        }
    }

    interface ControllerVisibilityListener {
        fun onVisibilityChanged(isVisibility: Boolean)
    }


    interface OnFullScreenModeChangedListener {
        fun onFullScreenModeChanged(isFullScreen: Boolean)
    }

}