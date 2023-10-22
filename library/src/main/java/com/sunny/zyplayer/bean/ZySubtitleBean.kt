package com.sunny.zyplayer.bean

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class ZySubtitleBean(var uri: Uri) : Parcelable {
    var language = ""
    var mimeType = ""

    constructor(parcel: Parcel) : this(parcel.readParcelable(Uri::class.java.classLoader) ?: Uri.EMPTY) {
        language = parcel.readString() ?: ""
        mimeType = parcel.readString() ?: ""
    }


    override fun toString(): String {
        return "ZySubtitleBean(uri=$uri, language='$language', mimeType='$mimeType')"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeString(language)
        parcel.writeString(mimeType)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ZySubtitleBean> {
        override fun createFromParcel(parcel: Parcel): ZySubtitleBean {
            return ZySubtitleBean(parcel)
        }

        override fun newArray(size: Int): Array<ZySubtitleBean?> {
            return arrayOfNulls(size)
        }
    }

}