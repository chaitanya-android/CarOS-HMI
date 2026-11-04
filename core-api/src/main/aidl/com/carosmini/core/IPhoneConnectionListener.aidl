package com.carosmini.core;

import com.carosmini.core.PairedDevice;

oneway interface IPhoneConnectionListener {
    void onDeviceConnected(in PairedDevice device);
    void onDeviceDisconnected(in PairedDevice device);
}
