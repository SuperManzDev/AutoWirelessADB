# AutoWirelessAdb

Background utility for Android tablets used as in-car head units. Handles vehicle ignition power events, keeps ADB available over Wi-Fi, and updates outdated system root certificates on older Android versions.

## What it does

- Detects charger connection (`ACTION_POWER_CONNECTED`). Wakes the screen and launches Headunit Reloaded for Android Auto when the car starts.
- Puts the screen to sleep when power is disconnected to prevent draining the tablet battery while parked.
- Starts the ADB daemon on TCP port 5555 on boot via root, allowing wireless debugging without plugging into a computer.
- Unpacks updated Mozilla / Let's Encrypt root certificates (`cacerts.tar.gz`) into `/system/etc/security/cacerts` so Android 7 devices can connect to modern HTTPS endpoints.

## Building from source

Requirements:
- JDK 8 or higher
- Android SDK build tools
- Android API 24 platform

Run the build script:

```powershell
.\build.ps1
```

The compiled package is saved as `AutoWirelessADB.apk`.

Install it via ADB:

```powershell
adb install -r AutoWirelessADB.apk
adb shell "appops set com.car.autowirelessadb WRITE_SETTINGS allow"
```

## License

GPL-3.0.
Android Auto is a trademark of Google LLC.
