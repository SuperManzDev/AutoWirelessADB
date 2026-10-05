# AutoWirelessAdb · Android Auto & Wireless ADB Automation 🚗⚡

> **Automotive power management, wireless ADB automation, and Android Auto launcher for Android 7 tablets.**

## Overview

**AutoWirelessAdb (`com.car.autowirelessadb`)** is a dedicated background service and automotive controller for Android tablets mounted in vehicles (such as Benesse Challenge Pad 3 / MT8167B). It automates the tablet's lifecycle in sync with the vehicle's ignition and powers Android Auto (via Headunit Reloaded) with zero manual intervention.

---

## Features

1. **Auto Ignition Wake & Sleep**:
   - Listens for vehicle power events (`ACTION_POWER_CONNECTED` / `ACTION_POWER_DISCONNECTED`).
   - Wakes screen, sets full brightness, and launches Android Auto (Headunit Reloaded) on ignition turn-on.
   - Puts tablet to sleep and dims screen on ignition turn-off to conserve vehicle battery.
2. **Persistent Wireless ADB (Port 5555)**:
   - Uses embedded `mtk-su` root privileges to persist `setprop service.adb.tcp.port 5555` and restart `adbd`.
   - Allows completely wireless debugging, app deployment, and shell access inside the vehicle.
3. **Automated Root CA Certificate Updates**:
   - Injects modern system root certificates (`cacerts.tar.gz`) into `/system/etc/security/cacerts` on Android 7 devices, fixing expired Let's Encrypt / modern HTTPS connections across all apps.
4. **Automotive Dark UI & Quick Controls**:
   - Clean dark-mode dashboard with instant buttons for enabling Wireless ADB, updating certificates, boosting CPU governor, and toggling screen sleep.

---

## Building from Source

Prerequisites:
- JDK 8 or JDK 17/21
- Android Build-Tools (AAPT, D8, Zipalign, Apksigner)
- Android SDK Platform `android-24`

```powershell
.\build.ps1
```
The output APK is generated at `AutoWirelessADB.apk`.

To install to an attached device:
```powershell
adb install -r AutoWirelessADB.apk
adb shell "appops set com.car.autowirelessadb WRITE_SETTINGS allow"
```

---

## License

GNU General Public License v3.0 (GPL-3.0).
Android Auto is a trademark of Google LLC.
