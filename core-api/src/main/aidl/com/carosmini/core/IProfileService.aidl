package com.carosmini.core;

import com.carosmini.core.Profile;
import com.carosmini.core.IProfileListener;

interface IProfileService {
    List<Profile> getProfiles();
    Profile getActiveProfile();
    void setActiveProfile(int profileId);
    void createProfile(in Profile profile);
    void updateProfile(in Profile profile);
    void deleteProfile(int profileId);
    void registerProfileListener(IProfileListener listener);
    void unregisterProfileListener(IProfileListener listener);
}
