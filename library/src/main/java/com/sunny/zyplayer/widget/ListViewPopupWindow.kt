package com.sunny.zyplayer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.sunny.zy.base.BaseRecycleAdapter
import com.sunny.zy.base.BaseRecycleViewHolder
import com.sunny.zyplayer.ColorConfig
import com.sunny.zyplayer.R
import com.sunny.zyplayer.bean.ZyVideoBean
import com.sunny.zyplayer.databinding.ZyLayoutListViewBinding


class ListViewPopupWindow(context: Context, val player: ExoPlayer, val videoList: ArrayList<ZyVideoBean>) : PopupWindow(context) {


    private val viewBinding by lazy {
        ZyLayoutListViewBinding.inflate(LayoutInflater.from(context))
    }

    private var index = 0

    init {
        setBackgroundDrawable(null)
        width = context.resources.getDimension(com.sunny.zy.R.dimen.dp_120).toInt()
        height = context.resources.getDimension(com.sunny.zy.R.dimen.dp_160).toInt()
        contentView = viewBinding.root
        animationStyle = R.style.VolumeViewAnimation

        viewBinding.recyclerView.layoutManager = LinearLayoutManager(context)
        viewBinding.recyclerView.adapter = Adapter().apply {
            setOnItemClickListener { _, position ->
                val lastIndex = index
                if (lastIndex != position) {
                    index = position
                    notifyItemChanged(lastIndex)
                    notifyItemChanged(index)
                    player.seekTo(position, 0)
                    dismiss()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateIndex(index: Int) {
        this.index = index
        viewBinding.recyclerView.adapter?.notifyDataSetChanged()
    }


    inner class Adapter : BaseRecycleAdapter<ZyVideoBean>(videoList) {
        override fun initLayout(parent: ViewGroup, viewType: Int): View {
            return TextView(context).apply {
                val dp5 = context.resources.getDimension(com.sunny.zy.R.dimen.dp_5).toInt()
                val dp10 = context.resources.getDimension(com.sunny.zy.R.dimen.dp_10).toInt()
                setPadding(dp10, dp5, dp10, dp5)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(com.sunny.zy.R.dimen.dp_10))
            }
        }

        override fun onBindViewHolder(holder: BaseRecycleViewHolder, position: Int) {
            val textView = holder.itemView as TextView
            val data = getData(position)
            val title = data.title.ifEmpty {
                val names = getData(position).uri.lastPathSegment?.split(".")
                names?.get(0) ?: ""
            }
            textView.text = title

            if (position == index) {
                textView.setTextColor(ColorConfig.colorTheme)
            } else {
                textView.setTextColor(Color.BLACK)
            }
        }

    }
}