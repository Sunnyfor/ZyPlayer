package com.sunny.zyplayer.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.view.View
import android.view.WindowManager
import androidx.media3.exoplayer.ExoPlayer
import com.sunny.zy.base.BaseActivity
import com.sunny.zyplayer.bean.ZySubtitleBean
import com.sunny.zyplayer.databinding.ActivityVideoBinding

class VideoActivity : BaseActivity() {

    companion object {
        fun intent(
            context: Context,
            title: String,
            videoUrl: String,
            subtitleBean: ZySubtitleBean? = null
        ) {
            val intent = Intent(context, VideoActivity::class.java)
            intent.putExtra("videoUrl", videoUrl)
            intent.putExtra("title", title)
            intent.putExtra("subtitle", subtitleBean)
            context.startActivity(intent)
        }

        fun intent(
            context: Context,
            videoUrl: String
        ) {
            intent(context, "", videoUrl)
        }
    }

    private val viewBinding by lazy { ActivityVideoBinding.inflate(layoutInflater) }

    private val player by lazy { ExoPlayer.Builder(this).build() }

    private val title: String by lazy { intent.getStringExtra("title") ?: "" }

    private val videoUrl: String by lazy { intent.getStringExtra("videoUrl") ?: "" }

    private val subtitleBean: ZySubtitleBean? by lazy { intent.getParcelableExtra("subtitle") }

    override fun initLayout() = viewBinding.root

    override fun initView() {
        statusBar.setBackgroundColor(Color.BLACK)
        if (title.isNotEmpty()) {
            viewBinding.tvTitle.text = title
        }
        viewBinding.videoView.setPlayer(player)

        viewBinding.videoView.setVideoUrl(videoUrl, subtitleBean, true)

        viewBinding.videoView.setControllerVisibilityListener {
            viewBinding.clTitle.visibility = it
        }
        viewBinding.videoView.setFullscreenButtonClickListener {
            if (it) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                supportActionBar?.hide()
                hideStatusBar()

            } else {
                requestedOrientation = screenOrientation
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                supportActionBar?.show()
                showStatusBar()

            }
        }
        setOnClickListener(viewBinding.ibBack)
    }

    override fun loadData() {

    }

    override fun onClickEvent(view: View) {
        when (view.id) {
            viewBinding.ibBack.id -> {
                finish()
            }
        }
    }

    override fun onClose() {
        player.release()
    }

}