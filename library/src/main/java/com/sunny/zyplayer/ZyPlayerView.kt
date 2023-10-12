package com.sunny.zyplayer

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.TimeBar
import com.google.common.collect.ImmutableList
import com.sunny.zyplayer.bean.ZySubtitleBean
import com.sunny.zyplayer.databinding.ZyLayoutPlayerViewBinding
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

    private val volumeView by lazy {
        VolumeViewPopupWindow(context).apply {
            height = (viewBinding.playerView.height *0.8).toInt()
            width = height / 4
        }
    }

    private var controllerShowTimeoutMs = 5000L

    private var isAnimating = false

    init {
        viewBinding.playerView.player = player

        player.addListener(this)

        viewBinding.playerView.setOnClickListener(this)

        viewBinding.playerControl.timeBar.addListener(this)

        viewBinding.playerControl.ivPlay.setOnClickListener(this)

        viewBinding.playerControl.btnForward.setOnClickListener(this)

        viewBinding.playerControl.ivFullscreen.setOnClickListener(this)

        viewBinding.playerControl.ivVolume.setOnClickListener(this)

        setControllerShowTimeoutMs(controllerShowTimeoutMs)
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

        val subtitleUri = subtitleBean?.uri
        if (subtitleUri != null) {
            MimeTypes.APPLICATION_AIT
            viewBinding.playerView.setShowSubtitleButton(true)
            val subtitleConfiguration = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                .setLanguage(subtitleBean.language)
                .setMimeType(subtitleBean.mimeType)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaItem.setSubtitleConfigurations(ImmutableList.of(subtitleConfiguration))
        }
        player.setMediaItem(mediaItem.build())
        player.prepare()
        player.playWhenReady = isAutoPlay


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
                    val y = (viewBinding.playerControl.root.height + volumeView.height)
                    val dp10 = resources.getDimension(com.sunny.zy.R.dimen.dp_10).toInt()
                    volumeView.showAsDropDown(viewBinding.playerControl.root, -dp10, -y, Gravity.END)
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

    interface ControllerVisibilityListener {
        fun onVisibilityChanged(isVisibility: Boolean)
    }


    interface OnFullScreenModeChangedListener {
        fun onFullScreenModeChanged(isFullScreen: Boolean)
    }

}