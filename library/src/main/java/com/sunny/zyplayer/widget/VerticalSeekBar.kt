package com.sunny.zyplayer.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.sunny.zyplayer.ColorConfig
import com.sunny.zyplayer.R


class VerticalSeekBar : View {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val progressBar = RectF()
    private val rectF = RectF()
    private val thumb = RectF()
    private val thumbBorder = RectF()
    private var thumbSize = 0f
    private val thumbBorderSize by lazy {
        thumbSize / 6
    }
    private var progressSize = 0f
    private var progress = 0
    private var max = 0

    var onSeekBarChangeListener: OnSeekBarChangeListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.VerticalSeekBar)
        val thumbSizeId = typeArray.getResourceId(R.styleable.VerticalSeekBar_thumbSize, 0)

        thumbSize = if (thumbSizeId != 0) {
            resources.getDimension(thumbSizeId)
        } else {
            typeArray.getDimension(R.styleable.VerticalSeekBar_thumbSize, 0f)
        }

        val progressSizeId = typeArray.getResourceId(R.styleable.VerticalSeekBar_progressSize, 0)
        progressSize = if (progressSizeId != 0) {
            resources.getDimension(progressSizeId)
        } else {
            typeArray.getDimension(R.styleable.VerticalSeekBar_progressSize, 0f)
        }

        max = (attrs?.getAttributeValue("http://schemas.android.com/apk/res/android", "max") ?: "100").toInt()
        typeArray.recycle()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        if (thumbSize == 0f) {
            thumbSize = resources.getDimension(com.sunny.zy.R.dimen.dp_16)
        }

        if (progressSize == 0f) {
            progressSize = thumbSize / 3
        }

        if (widthMode == MeasureSpec.AT_MOST) {
            width = thumbSize.toInt() * 2
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            height = resources.getDimension(com.sunny.zy.R.dimen.dp_100).toInt()
        }

        setMeasuredDimension(width, height)


        val thumbLeft = (width - thumbSize) / 2
        thumbBorder.set(thumbLeft, height - thumbSize, thumbSize + thumbLeft, height.toFloat())
        thumb.set(thumbBorderSize, thumb.top + thumbBorderSize, thumb.right - thumbBorderSize, thumb.bottom - thumbBorderSize)
        val progressLeft = (width - progressSize) / 2
        progressBar.set(progressLeft, 0f, progressSize + progressLeft, height.toFloat())
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val radius = progressBar.right / 2f
        // 绘制进度条
        paint.color = getColorWithAlpha(0.1f, ColorConfig.colorTheme)
        canvas.drawRoundRect(progressBar, radius, radius, paint)

//        // 绘制进度
        paint.color = ColorConfig.colorTheme
        val progressY = progressBar.bottom - progress * progressBar.bottom / max
        rectF.left = progressBar.left
        rectF.right = progressBar.right
        rectF.top = progressY
        rectF.bottom = progressBar.bottom
        canvas.drawRoundRect(rectF, radius, radius, paint)


        paint.color = Color.WHITE
        val thumbScope = progressBar.bottom - thumbSize
        val thumbY = thumbScope - progress * thumbScope / max
        thumbBorder.bottom = thumbY + thumbSize
        thumbBorder.top = thumbY
        canvas.drawRoundRect(thumbBorder, radius, radius, paint)

        // 绘制滑块
        paint.color = ColorConfig.colorTheme
        thumb.left = thumbBorder.left + thumbBorderSize
        thumb.top = thumbBorder.top + thumbBorderSize
        thumb.right = thumbBorder.right - thumbBorderSize
        thumb.bottom = thumbBorder.bottom - thumbBorderSize
        canvas.drawRoundRect(thumb, radius, radius, paint)
//        LogUtil.i("值:$progressY")
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                var y = event.y
                if (y < progressBar.top) {
                    y = progressBar.top
                }
                if (y > progressBar.bottom) {
                    y = progressBar.bottom
                }

                val mProgress = max - (max * y / height).toInt()
                if (mProgress != progress) {
                    progress = mProgress
                    invalidate()
                    onSeekBarChangeListener?.onProgressChanged(this, progress, true)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                performClick()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun getProgress(): Int {
        return progress
    }

    fun setProgress(progress: Int) {
        this.progress = progress
        invalidate()
        onSeekBarChangeListener?.onProgressChanged(this, progress, true)
    }

    fun getMax(): Int {
        return max
    }

    fun setMax(max: Int) {
        this.max = max
        invalidate()
        onSeekBarChangeListener?.onProgressChanged(this, progress, true)
    }


    private fun getColorWithAlpha(alpha: Float, baseColor: Int): Int {
        val a = 255.coerceAtMost(0.coerceAtLeast((alpha * 255).toInt())) shl 24
        val rgb = 0x00ffffff and baseColor
        return a + rgb
    }


    interface OnSeekBarChangeListener {
        fun onProgressChanged(seekBar: VerticalSeekBar, progress: Int, fromUser: Boolean)
    }
}