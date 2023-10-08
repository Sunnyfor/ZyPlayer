package com.sunny.zyplayer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.sunny.kit.utils.LogUtil


class VerticalSeekBar : View {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.RED
    }

    private val progressBar = RectF()
    private val rectF = RectF()
    private val thumb = RectF()
    private val thumbBorder = RectF()
    private var progress = 0
    private var max = 100
    private var min = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        thumb.set(0f, measuredHeight - measuredWidth.toFloat(), measuredWidth.toFloat(), measuredHeight.toFloat())
        thumbBorder.set(5f, thumb.top + 5, thumb.right + 5, thumb.bottom + 5)
        val progressBarWidth = measuredWidth.toFloat() / 3
        progressBar.set(progressBarWidth, 0f, progressBarWidth * 2, measuredHeight.toFloat())
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val radius = width / 2f
        // 绘制进度条
        paint.color = getColorWithAlpha(0.1f, ContextCompat.getColor(context, com.sunny.zy.R.color.colorTheme))
        canvas.drawRoundRect(progressBar, radius, radius, paint)

//        // 绘制进度
        paint.color = ContextCompat.getColor(context, com.sunny.zy.R.color.colorTheme)
        val progressY = progressBar.bottom - progress * progressBar.bottom / max
        rectF.left = progressBar.left
        rectF.right = progressBar.right
        rectF.top = progressY
        LogUtil.i("高度：$progressY")
        rectF.bottom = progressBar.bottom
        canvas.drawRoundRect(rectF, radius, radius, paint)

        // 绘制滑块
        val thumbScope = progressBar.bottom - width
        val thumbY = thumbScope - progress * thumbScope / max
        thumb.bottom = thumbY + width
        thumb.top = thumbY
        canvas.drawRoundRect(thumb, radius, radius, paint)

//        paint.color = Color.WHITE
//        thumbBorder.left = thumb.left + 5
//        thumbBorder.top = thumb.top + 5
//        thumbBorder.right = thumb.right + 5
//        thumbBorder.bottom = thumb.bottom + 5
//        canvas.drawRoundRect(thumbBorder, radius, radius, paint)
//        LogUtil.i("值:$progressY")
    }

    @SuppressLint("ClickableViewAccessibility")
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
//                if (y >= progressBar.top && y <= height) {
                progress = max - (max * y / height).toInt()
//                    LogUtil.i("百分比:$progress -----------Y = $y  progressBar.bottom = ${height}  progressabr= ${progressBar.bottom}")
//                    thumb.bottom += progress
//                    thumb.top -= progress
                invalidate()
                return true
//                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun getProgress(): Int {
        return progress
    }

    fun setProgress(progress: Int) {
        this.progress = progress
        invalidate()
    }

    fun getMax(): Int {
        return max
    }

    fun setMax(max: Int) {
        this.max = max
        invalidate()
    }

    fun getMin(): Int {
        return min
    }

    fun setMin(min: Int) {
        this.min = min
        invalidate()
    }

    fun getColorWithAlpha(alpha: Float, baseColor: Int): Int {
        val a = 255.coerceAtMost(0.coerceAtLeast((alpha * 255).toInt())) shl 24
        val rgb = 0x00ffffff and baseColor
        return a + rgb
    }
}