package com.sunny.zyplayer.bean

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

/**
 * Desc
 * Author ZY
 * Date 2023/10/22
 */
class ZyVideoBean(var uri: Uri) : Parcelable {

    var title: String = ""

    var subtitle: ZySubtitleBean? = null

    constructor(parcel: Parcel) : this(parcel.readParcelable(Uri::class.java.classLoader) ?: Uri.EMPTY) {
        title = parcel.readString() ?: ""
        subtitle = parcel.readParcelable(ZySubtitleBean::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeString(title)
        parcel.writeParcelable(subtitle, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ZyVideoBean> {
        override fun createFromParcel(parcel: Parcel): ZyVideoBean {
            return ZyVideoBean(parcel)
        }

        override fun newArray(size: Int): Array<ZyVideoBean?> {
            return arrayOfNulls(size)
        }
    }


}