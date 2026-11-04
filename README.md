# CarOSMini

A multi-app Android Automotive infotainment system demonstrating real-world AAOS architecture: profile switching, Bluetooth phone detection, and per-user preferences — all running on a two-emulator development setup.

## Project Structure

```
CarOSMini/
├── core-service/          # Bound AIDL services (ProfileService, PhoneConnectionService)
│                          # Room database, shared Parcelable models
├── app-launcher/          # AAOS home launcher — profile display, app grid
├── app-media/             # Media player (stub → full in Phase 6)
├── app-nav/               # Navigation (stub → full in Phase 7)
├── app-dashboard/         # Driver dashboard (stub → full in Phase 8)
└── scripts/
    └── install-all.sh     # Targeted APK install to correct emulators
```

## Hardware Requirements

Running two emulators simultaneously requires:

- **RAM**: 16 GB minimum, 32 GB recommended
- **CPU**: Hardware virtualisation (Intel VT-x, AMD-V, or Apple Silicon)
- **Disk**: 50 GB free for system images + project artifacts
- **OS**: macOS, Linux, or Windows with WSL2 (Linux has best emulator performance)

## Prerequisites

1. **Android Studio** Hedgehog (2023.1) or newer
2. **AAOS system image**: `Automotive with Google APIs, API 34, x86_64`
3. **Phone system image**: `Google Play, API 34, x86_64`

Install both via **SDK Manager → SDK Platforms → Android 14 (API 34)**.

## Two-Emulator Setup

### 1. Create AVDs

Open **Device Manager** in Android Studio:

| Field | AAOS AVD | Phone AVD |
|---|---|---|
| Name | `CarOSMini_AAOS` | `CarOSMini_Phone` |
| Hardware | Automotive 1080p landscape | Pixel 7 |
| System image | API 34 Automotive + Google APIs | API 34 Google Play |
| RAM | 4096 MB | 2048 MB |
| Architecture | x86_64 | x86_64 |

> Keep both on x86_64. Mixing architectures breaks the Bluetooth bridge.

### 2. Launch Order

1. Start `CarOSMini_AAOS` first
2. Start `CarOSMini_Phone` second
3. Verify both appear in `adb devices`:

```
adb devices
# List of devices attached
# emulator-5554   device     ← AAOS
# emulator-5556   device     ← Phone
```

### 3. Build

```bash
./gradlew assembleDebug
```

### 4. Install

```bash
./scripts/install-all.sh
```

The script resolves each emulator by AVD name automatically. Override with environment variables if needed:

```bash
AAOS_AVD=MyCustomAAOS PHONE_AVD=MyPhone ./scripts/install-all.sh
```

### 5. Bluetooth Pairing (one-time setup)

1. On AAOS emulator: **Settings → Connected devices → Pair new device**
2. On Phone emulator: **Settings → Connected devices → Pair new device**
3. Each emulator's Bluetooth name appears on the other's scan list
4. Tap to pair → confirm the PIN on both sides
5. `ACTION_ACL_CONNECTED` fires on the AAOS side — `PhoneConnectionService` picks it up

### 6. Link Phone to Profile

1. In the CarOSMini launcher, open **Settings**
2. Under **Phone Profiles**, select the paired phone's MAC address
3. Assign it to a profile (e.g. "Dad")
4. Disconnect and reconnect Bluetooth — the profile switches automatically
5. A 30-second grace period applies before reverting on disconnect

### 7. AVD Snapshots (saves ~10 min per session)

After initial setup (all APKs installed, Bluetooth paired):

- AAOS: **Extended Controls (⋯) → Snapshots → Take Snapshot** → name it `caros-ready`
- Phone: same, name it `phone-ready`

Restore from snapshot at the start of each session instead of cold boot.

## Architecture

### AIDL Contracts (core-service)

```
IProfileService
  getProfiles()                     → List<Profile>
  getActiveProfile()                → Profile
  setActiveProfile(profileId)
  createProfile(profile)
  deleteProfile(profileId)
  registerProfileListener(listener)
  unregisterProfileListener(listener)

IPhoneConnectionService
  getPairedDevices()                     → List<PairedDevice>
  linkDeviceToProfile(macAddress, profileId)
  getCurrentlyConnectedDevice()          → PairedDevice
  registerConnectionListener(listener)
  unregisterConnectionListener(listener)
```

### Profile Switch Flow

```
BluetoothDevice.ACTION_ACL_CONNECTED
    └─► PhoneConnectionService.handleConnected()
            └─► DeviceLinkDao.getByMac(mac)
                    └─► if linkedProfileId != -1
                            └─► broadcast ACTION_SET_PROFILE
                                    └─► ProfileService.setActiveProfile()
                                            └─► notifyListeners() → all apps update UI
```

### Database (Room)

| Table | Purpose |
|---|---|
| `profiles` | Stored user profiles (name, avatar, isDefault) |
| `device_profile_links` | Maps Bluetooth MAC → profileId |

## Known Emulator Limitations

| Feature | Status |
|---|---|
| ACL connect/disconnect events | **Works** |
| Profile switching via MAC lookup | **Works** |
| A2DP audio streaming from phone | Limited — use local files on AAOS for audio demos |
| PBAP (contacts sync) | Not supported in emulator; code path exists, returns empty on emulator |
| MAP (messages) | Not supported in emulator; available on real hardware |
| Bluetooth name change live | Requires phone emulator reboot to take effect |

> The code path is identical to real hardware. These are emulator constraints, not architectural ones.

## Build Phases

| Phase | Status | Description |
|---|---|---|
| 1 | ✅ Done | Multi-module Gradle setup, four stub apps, core-service AIDL |
| 2 | Next | ProfileService wired end-to-end; launcher shows active profile name |
| 3 | Planned | Profile CRUD UI in launcher settings |
| 4 | Planned | Per-profile preferences persistence |
| 5 | Planned | Launcher polish — app grid, icons, animations |
| 5.5 | Planned | Phone connection — Bluetooth pairing UI, MAC→profile mapping |
| 6 | Planned | Media app — local playback, profile-aware last-track memory |
| 7 | Planned | Nav app — destination history per profile |
| 8 | Planned | Dashboard app — trip stats per profile |
| 9 | Planned | Integration polish + demo script |
| 10 | Future | Real hardware deployment (Raspberry Pi or car head unit) |
