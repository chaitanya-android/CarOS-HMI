package com.carosmini.core

import android.os.Parcel
import android.os.Parcelable

data class PairedDevice(
    val macAddress: String,
    val displayName: String,
    val linkedProfileId: Int = -1,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        macAddress = parcel.readString() ?: "",
        displayName = parcel.readString() ?: "",
        linkedProfileId = parcel.readInt(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(macAddress)
        parcel.writeString(displayName)
        parcel.writeInt(linkedProfileId)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<PairedDevice> {
        override fun createFromParcel(parcel: Parcel) = PairedDevice(parcel)
        override fun newArray(size: Int): Array<PairedDevice?> = arrayOfNulls(size)
    }
}
