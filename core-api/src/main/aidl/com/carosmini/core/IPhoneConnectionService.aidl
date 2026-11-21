package com.carosmini.core;

import com.carosmini.core.PairedDevice;
import com.carosmini.core.IPhoneConnectionListener;

interface IPhoneConnectionService {
    List<PairedDevice> getPairedDevices();
    void linkDeviceToProfile(String macAddress, String displayName, int profileId);
    PairedDevice getCurrentlyConnectedDevice();
    void registerConnectionListener(IPhoneConnectionListener listener);
    void unregisterConnectionListener(IPhoneConnectionListener listener);
}
