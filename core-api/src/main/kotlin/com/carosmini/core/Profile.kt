package com.carosmini.core

import android.os.Parcel
import android.os.Parcelable

data class Profile(
    val id: Int = 0,
    val name: String,
    val avatarResId: Int = 0,
    val isDefault: Boolean = false,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        name = parcel.readString() ?: "",
        avatarResId = parcel.readInt(),
        isDefault = parcel.readByte() != 0.toByte(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeInt(avatarResId)
        parcel.writeByte(if (isDefault) 1 else 0)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Profile> {
        override fun createFromParcel(parcel: Parcel) = Profile(parcel)
        override fun newArray(size: Int): Array<Profile?> = arrayOfNulls(size)
    }
}
