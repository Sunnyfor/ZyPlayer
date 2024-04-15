package com.sunny.zyplayer.widget

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import com.sunny.zyplayer.R
import com.sunny.zyplayer.databinding.ZyLayoutVolumeViewBinding


class VolumeViewPopupWindow(context: Context) : PopupWindow(context) {

    companion object {
        const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    }


    private val audioManager by lazy {
        context.getSystemService(Service.AUDIO_SERVICE) as AudioManager
    }

    private val viewBinding by lazy {
        ZyLayoutVolumeViewBinding.inflate(LayoutInflater.from(context))
    }

    private val autoDismissHandler: Handler = Handler(Looper.getMainLooper())

    private val volumeBroadcastReceiver by lazy {
        VolumeBroadcastReceiver()
    }

    private val autoDismissRunnable by lazy {
        Runnable {
            dismiss()
        }
    }


    private var isRegister = false

    private var isUpdate = false

    init {
        setBackgroundDrawable(null)
        width = context.resources.getDimension(com.sunny.zy.R.dimen.dp_35).toInt()
        height = context.resources.getDimension(com.sunny.zy.R.dimen.dp_120).toInt()
        contentView = viewBinding.root

        viewBinding.seekBar.onSeekBarChangeListener = object : VerticalSeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: VerticalSeekBar, progress: Int, fromUser: Boolean) {
                startAutoDismiss()
                val value = ((progress.toFloat() / seekBar.getMax()) * 100).toInt()
                viewBinding.tvNumber.text = value.toString()
                if (isUpdate) {
                    return
                }
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,  //音量类型
                    progress, AudioManager.FLAG_PLAY_SOUND
                )
            }
        }

        audioManager.requestAudioFocus(
            {},
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        animationStyle = R.style.VolumeViewAnimation
        updateValue()
    }

    private fun updateValue() {
        isUpdate = true
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        viewBinding.seekBar.setMax(maxVolume)
        viewBinding.seekBar.setProgress(currentVolume)
        isUpdate = false
    }

    override fun dismiss() {
        unrRegisterReceiver(contentView.context)
        updateValue()
        super.dismiss()
    }

    override fun showAtLocation(parent: View, gravity: Int, x: Int, y: Int) {
        super.showAtLocation(parent, gravity, x, y)
        startAutoDismiss()
        registerReceiver(parent.context)
    }

    override fun showAsDropDown(anchor: View, xoff: Int, yoff: Int, gravity: Int) {
        super.showAsDropDown(anchor, xoff, yoff, gravity)
        startAutoDismiss()
        registerReceiver(anchor.context)
    }

    private fun startAutoDismiss() {
        autoDismissHandler.removeCallbacks(autoDismissRunnable)
        autoDismissHandler.postDelayed(autoDismissRunnable, 4 * 1000L)
    }


    private fun registerReceiver(context: Context) {
        isRegister = true
        val filter = IntentFilter()
        filter.addAction(VOLUME_CHANGED_ACTION)
        context.registerReceiver(volumeBroadcastReceiver, filter)
    }

    private fun unrRegisterReceiver(context: Context) {
        if (isRegister) {
            isRegister = false
            context.unregisterReceiver(volumeBroadcastReceiver)
        }
    }


    inner class VolumeBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "android.media.VOLUME_CHANGED_ACTION" &&
                (intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) == AudioManager.STREAM_MUSIC)
            ) {
                updateValue()
            }
        }

    }
}