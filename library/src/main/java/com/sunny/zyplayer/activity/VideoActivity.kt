package com.sunny.zyplayer.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Parcelable
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import com.sunny.zy.base.BaseActivity
import com.sunny.zyplayer.R
import com.sunny.zyplayer.ZyPlayerView
import com.sunny.zyplayer.bean.ZyVideoBean
import com.sunny.zyplayer.databinding.ZyActivityVideoBinding

class VideoActivity : BaseActivity() {

    companion object {
        fun intent(
            context: Context,
            title: String,
            videoBeans: ArrayList<ZyVideoBean>,
            isAutoPlay: Boolean = true
        ) {
            val intent = Intent(context, VideoActivity::class.java)
            intent.putExtra("mTitle", title)
            intent.putExtra("videoList", videoBeans)
            intent.putExtra("isAutoPlay", isAutoPlay)
            context.startActivity(intent)
        }

        fun intent(
            context: Context,
            title: String,
            videoBean: ZyVideoBean,
            isAutoPlay: Boolean = true
        ) {
            intent(context, title, arrayListOf(videoBean), isAutoPlay)
        }
    }

    private val viewBinding by lazy { ZyActivityVideoBinding.inflate(layoutInflater) }


    private val videoData by lazy { intent.getParcelableArrayListExtra<Parcelable>("videoList") ?: arrayListOf() }

    private val isAutoPlay by lazy { intent.getBooleanExtra("isAutoPlay", true) }

    private var isFullScreen = false

    override fun initLayout() = viewBinding.root

    override fun initView() {
        statusBar.setBackgroundColor(Color.BLACK)
        viewBinding.tvTitle.text = intent.getStringExtra("mTitle") ?: ""
        viewBinding.videoView.setControllerShowTimeoutMs(0) //不隐藏操作拦
        val playList = arrayListOf<ZyVideoBean>()
        videoData.forEach {
            if (it is ZyVideoBean) {
                playList.add(it)
            }
        }
        viewBinding.videoView.setVideoData(playList, isAutoPlay)

        viewBinding.videoView.setControllerVisibilityListener(object : ZyPlayerView.ControllerVisibilityListener {
            override fun onVisibilityChanged(isVisibility: Boolean) {
                if (isVisibility) {
                    showTitleView()
                } else {
                    hideTitleView()
                }
            }
        })

        viewBinding.videoView.setFullScreenModeChangedListener(object : ZyPlayerView.OnFullScreenModeChangedListener {
            override fun onFullScreenModeChanged(isFullScreen: Boolean) {
                this@VideoActivity.isFullScreen = isFullScreen
                fullScreenModeChanged(isFullScreen)
            }

        })
        setOnClickListener(viewBinding.ibBack)
    }


    fun fullScreenModeChanged(isFullScreen: Boolean) {

        val layoutParams = viewBinding.videoView.layoutParams as ConstraintLayout.LayoutParams

        if (isFullScreen) {
            layoutParams.topToBottom = ConstraintLayout.LayoutParams.UNSET
            layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            viewBinding.videoView.setControllerShowTimeoutMs(5000)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            supportActionBar?.hide()
            hideStatusBar()
        } else {
            layoutParams.topToBottom = R.id.clTitle
            layoutParams.topToTop = ConstraintLayout.LayoutParams.UNSET
            viewBinding.videoView.setControllerShowTimeoutMs(0)
            requestedOrientation = screenOrientation
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            supportActionBar?.show()
            showStatusBar()

        }
    }


    override fun loadData() {

    }

    override fun onClickEvent(view: View) {
        when (view.id) {
            viewBinding.ibBack.id -> {
                if (isFullScreen) {
                    viewBinding.videoView.setFullScreen(false)
                } else {
                    finish()
                }
            }
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isFullScreen) {
            viewBinding.videoView.setFullScreen(false)
        } else {
            super.onBackPressed()
        }
    }


    private fun showTitleView() {
        val titleView = viewBinding.clTitle
        if (titleView.translationY.toInt() == -titleView.height) {
            val animator: ViewPropertyAnimator = titleView.animate()
                .translationYBy(titleView.height.toFloat())
                .setDuration(500) // 设置动画持续时间
            animator.start()
        }
    }

    private fun hideTitleView() {
        val titleView = viewBinding.clTitle
        if (titleView.translationY.toInt() == 0) {
            val animator: ViewPropertyAnimator = titleView.animate()
                .translationYBy(-titleView.height.toFloat())
                .setDuration(500) // 设置动画持续时间
                .withEndAction {
                    if (!isFullScreen) {
                        showTitleView()
                    }
                }
            animator.start()
        }
    }

    override fun onClose() {
        viewBinding.videoView.release()
    }

}