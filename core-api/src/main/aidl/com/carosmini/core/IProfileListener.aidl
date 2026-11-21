package com.carosmini.core;

import com.carosmini.core.Profile;

oneway interface IProfileListener {
    void onProfileChanged(in Profile profile);
}
