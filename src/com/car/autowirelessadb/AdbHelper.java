package com.car.autowirelessadb;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

public class AdbHelper {
    public static synchronized String enableWirelessAdb(Context context) {
        StringBuilder log = new StringBuilder();
        File logFile = new File(context.getFilesDir(), "app.log");

        try {
            appendLog(logFile, "=== Start enableWirelessAdb at " + new Date() + " ===");

            File localSu = new File(context.getFilesDir(), "mtk-su");
            if (!localSu.exists() || localSu.length() == 0) {
                try {
                    InputStream in = context.getAssets().open("mtk-su64");
                    FileOutputStream out = new FileOutputStream(localSu);
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                } catch (Exception ignored) {}
            }

            if (localSu.exists()) {
                localSu.setExecutable(true, false);
                localSu.setReadable(true, false);
            }

            String binaryPath = null;
            if (localSu.exists() && localSu.canExecute()) {
                binaryPath = localSu.getAbsolutePath();
            } else {
                File tmpSu = new File("/data/local/tmp/mtk-su");
                if (tmpSu.exists()) {
                    binaryPath = tmpSu.getAbsolutePath();
                }
            }

            if (binaryPath == null) {
                return "Error: no executable mtk-su found!";
            }

            File scriptFile = new File(context.getFilesDir(), "run_su.sh");
            FileOutputStream fos = new FileOutputStream(scriptFile);
            String shContent = "#!/system/bin/sh\n" +
                               "setprop service.adb.tcp.port 5555\n" +
                               "for p in $(pidof adbd); do\n" +
                               "    kill -9 $p\n" +
                               "done\n" +
                               "echo SCRIPT_DONE\n";
            fos.write(shContent.getBytes("UTF-8"));
            fos.flush();
            fos.close();
            scriptFile.setExecutable(true, false);
            scriptFile.setReadable(true, false);

            String cmd = "cat " + scriptFile.getAbsolutePath() + " | " + binaryPath + " -Z u:r:shell:s0";
            ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", "-c", cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line).append("\n");
            }

            int exitCode = p.waitFor();
            log.append("Exit: ").append(exitCode);

        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            log.append("Exception: ").append(t.getMessage());
        }

        return log.toString();
    }

    public static synchronized String runTurboBoost(Context context) {
        StringBuilder sb = new StringBuilder();
        try {
            File localSu = new File(context.getFilesDir(), "mtk-su");
            String binaryPath = (localSu.exists() && localSu.canExecute()) ? localSu.getAbsolutePath() : "/data/local/tmp/mtk-su";

            File boostScript = new File(context.getFilesDir(), "boost.sh");
            FileOutputStream fos = new FileOutputStream(boostScript);
            String shContent = "#!/system/bin/sh\n" +
                "echo 'Terminating background bloat...'\n" +
                "for p in com.aurora.store com.teslacoilsw.launcher com.finalwire.aida64 com.topjohnwu.magisk com.android.gallery3d io.github.visnkmr.quicklaunch be.ppareit.swiftp_free org.fdroid.fdroid pl.solidexplorer2 com.android.settings com.svox.pico; do\n" +
                "  pids=$(pidof $p)\n" +
                "  if [ -n \"$pids\" ]; then\n" +
                "    for pid in $pids; do kill -9 $pid 2>/dev/null; done\n" +
                "    echo \"Stopped $p\"\n" +
                "  fi\n" +
                "done\n" +
                "for p in $(pidof com.andrerinas.headunitrevived); do renice -n -10 -p $p 2>/dev/null; echo \"Prioritized HUR ($p)\"; done\n" +
                "for p in $(pidof media.codec); do renice -n -10 -p $p 2>/dev/null; echo \"Prioritized Codec ($p)\"; done\n" +
                "for p in $(pidof surfaceflinger); do renice -n -8 -p $p 2>/dev/null; done\n" +
                "if [ -f /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load ]; then echo 80 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load 2>/dev/null; fi\n" +
                "for i in 1 2 3; do echo 1 > /sys/devices/system/cpu/cpu$i/online 2>/dev/null; done\n" +
                "sync; echo 3 > /proc/sys/vm/drop_caches\n" +
                "echo \"Cores: $(cat /sys/devices/system/cpu/online 2>/dev/null) / 4\"\n" +
                "echo \"Temp: $(( $(cat /sys/class/thermal/thermal_zone2/temp 2>/dev/null) / 1000 ))°C\"\n" +
                "FREE_KB=$(grep MemAvailable /proc/meminfo 2>/dev/null | tr -s ' ' | cut -d ' ' -f 2)\n" +
                "if [ -n \"$FREE_KB\" ]; then echo \"Free RAM: $(( FREE_KB / 1024 )) MB\"; fi\n" +
                "echo 'BOOST_DONE'\n";
            fos.write(shContent.getBytes("UTF-8"));
            fos.flush();
            fos.close();
            boostScript.setExecutable(true, false);

            String cmd = "cat " + boostScript.getAbsolutePath() + " | " + binaryPath + " -Z u:r:shell:s0";
            ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", "-c", cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("UID:") && !line.startsWith("selinux:")) {
                    sb.append(line).append("\n");
                }
            }
            p.waitFor();
        } catch (Throwable t) {
            sb.append("Boost error: ").append(t.getMessage());
        }
        return sb.toString().trim();
    }

    public static String getCpuStatus() {
        String cores = "0";
        try {
            File f = new File("/sys/devices/system/cpu/online");
            if (f.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                String l = br.readLine();
                br.close();
                if (l != null) cores = l.trim();
            }
        } catch (Exception ignored) {}

        String tempStr = "?";
        try {
            File f = new File("/sys/class/thermal/thermal_zone2/temp");
            if (f.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                String l = br.readLine();
                br.close();
                if (l != null) {
                    int mc = Integer.parseInt(l.trim());
                    tempStr = (mc / 1000) + "°C";
                }
            }
        } catch (Exception ignored) {}

        String pwrStr = "Batt";
        try {
            File f = new File("/sys/class/power_supply/battery/status");
            if (f.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                String l = br.readLine();
                br.close();
                if (l != null) pwrStr = l.trim();
            }
        } catch (Exception ignored) {}

        return "Cores: " + cores + " | Temp: " + tempStr + " | Pwr: " + pwrStr;
    }

    public static String getGatewayIp(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                DhcpInfo dhcp = wm.getDhcpInfo();
                if (dhcp != null && dhcp.gateway != 0) {
                    int gw = dhcp.gateway;
                    return (gw & 0xFF) + "." + ((gw >> 8) & 0xFF) + "." + ((gw >> 16) & 0xFF) + "." + ((gw >> 24) & 0xFF);
                }
            }
        } catch (Exception ignored) {}

        try {
            Process p = Runtime.getRuntime().exec(new String[]{"getprop", "net.dns1"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.waitFor();
            if (line != null && !line.trim().isEmpty() && !line.startsWith("127.")) {
                return line.trim();
            }
        } catch (Exception ignored) {}

        return null;
    }

    public static String getDeviceIp(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                WifiInfo info = wm.getConnectionInfo();
                if (info != null) {
                    int ip = info.getIpAddress();
                    if (ip != 0) {
                        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
                    }
                }
            }
        } catch (Exception ignored) {}

        try {
            Process p = Runtime.getRuntime().exec(new String[]{"getprop", "dhcp.wlan0.ipaddress"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.waitFor();
            if (line != null && !line.trim().isEmpty()) {
                return line.trim();
            }
        } catch (Exception ignored) {}

        return "Not Connected";
    }

    public static String getWifiSsid(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                WifiInfo info = wm.getConnectionInfo();
                if (info != null && info.getSSID() != null) {
                    String ssid = info.getSSID();
                    if (ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length() >= 2) {
                        return ssid.substring(1, ssid.length() - 1);
                    }
                    if (!"<unknown ssid>".equals(ssid)) {
                        return ssid;
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Disconnected";
    }

    public static boolean isPort5555Active() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"getprop", "service.adb.tcp.port"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.waitFor();
            return "5555".equals(line != null ? line.trim() : "");
        } catch (Exception e) {
            return false;
        }
    }

    public static void stopHeadunit(Context context) {
        runRootCommand(context, "am force-stop com.andrerinas.headunitrevived");
    }

    public static void returnHome(Context context) {
        stopHeadunit(context);
        runRootCommand(context, "input keyevent 3");
    }

    public static void sleepTablet(Context context) {
        runRootCommand(context, "am force-stop com.andrerinas.headunitrevived; input keyevent 26");
    }

    public static void launchHeadunit(Context context, String phoneIp) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("headunit://connect?ip=" + phoneIp));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage("com.andrerinas.headunitrevived");
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
            }
        }
    }

    public static boolean isPowerConnected(Context context) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int plugged = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1);
                if (plugged == android.os.BatteryManager.BATTERY_PLUGGED_AC ||
                    plugged == android.os.BatteryManager.BATTERY_PLUGGED_USB ||
                    plugged == android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        try {
            File ac = new File("/sys/class/power_supply/ac/online");
            if (ac.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ac)));
                String l = br.readLine();
                br.close();
                if ("1".equals(l != null ? l.trim() : "")) return true;
            }
        } catch (Exception ignored) {}

        try {
            File usb = new File("/sys/class/power_supply/usb/online");
            if (usb.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(usb)));
                String l = br.readLine();
                br.close();
                if ("1".equals(l != null ? l.trim() : "")) return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    public static void applyPowerConnected(Context context) {
        String cmd = "settings put global stay_on_while_plugged_in 7; " +
                     "settings put system screen_brightness 255; " +
                     "settings put system screen_brightness_mode 0; " +
                     "settings put system screen_off_timeout 2147483647; " +
                     "input keyevent 224; " +
                     "input keyevent 82; " +
                     "if [ -f /proc/hps/enabled ]; then echo 0 > /proc/hps/enabled; fi; " +
                     "echo 1 > /sys/devices/system/cpu/cpu1/online 2>/dev/null; " +
                     "echo 1 > /sys/devices/system/cpu/cpu2/online 2>/dev/null; " +
                     "echo 1 > /sys/devices/system/cpu/cpu3/online 2>/dev/null; " +
                     "echo interactive > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null; " +
                     "setprop service.adb.tcp.port 5555";
        runRootCommand(context, cmd);
    }

    public static void applyDeepSleep(Context context) {
        String cmd = "am force-stop com.andrerinas.headunitrevived; " +
                     "settings put system screen_brightness 20; " +
                     "settings put system screen_off_timeout 15000; " +
                     "input keyevent 26; " +
                     "echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null; " +
                     "echo 0 > /sys/devices/system/cpu/cpu1/online 2>/dev/null; " +
                     "echo 0 > /sys/devices/system/cpu/cpu2/online 2>/dev/null; " +
                     "echo 0 > /sys/devices/system/cpu/cpu3/online 2>/dev/null; " +
                     "sync; echo 3 > /proc/sys/vm/drop_caches";
        runRootCommand(context, cmd);
    }

    public static synchronized String installModernCertificates(Context context) {
        StringBuilder sb = new StringBuilder();
        try {
            File tarFile = new File(context.getFilesDir(), "cacerts.tar.gz");
            InputStream in = context.getAssets().open("cacerts.tar.gz");
            FileOutputStream out = new FileOutputStream(tarFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();

            File localSu = new File(context.getFilesDir(), "mtk-su");
            String suPath = (localSu.exists() && localSu.canExecute()) ? localSu.getAbsolutePath() : "/data/local/tmp/mtk-su";

            File script = new File(context.getFilesDir(), "install_certs.sh");
            FileOutputStream fos = new FileOutputStream(script);
            String sh = "#!/system/bin/sh\n" +
                "echo 'Preparing modern CA certificates store...'\n" +
                "mkdir -p /data/local/cacerts\n" +
                "cp -n /system/etc/security/cacerts/* /data/local/cacerts/ 2>/dev/null\n" +
                "tar -xzf " + tarFile.getAbsolutePath() + " -C /data/local/cacerts/\n" +
                "chmod 644 /data/local/cacerts/*\n" +
                "chown 0:0 /data/local/cacerts/*\n" +
                "chcon u:object_r:system_file:s0 /data/local/cacerts/* 2>/dev/null\n" +
                "umount /system/etc/security/cacerts 2>/dev/null\n" +
                "mount -o bind /data/local/cacerts /system/etc/security/cacerts\n" +
                "echo \"Mounted $(ls /system/etc/security/cacerts | wc -l) system CA certs!\"\n" +
                "echo 'CERTS_INSTALLED_OK'\n";
            fos.write(sh.getBytes("UTF-8"));
            fos.flush();
            fos.close();
            script.setExecutable(true, false);

            String cmd = "cat " + script.getAbsolutePath() + " | " + suPath + " -Z u:r:shell:s0";
            ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", "-c", cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("UID:") && !line.startsWith("selinux:")) {
                    sb.append(line).append("\n");
                }
            }
            p.waitFor();
        } catch (Throwable t) {
            sb.append("Cert error: ").append(t.getMessage());
        }
        return sb.toString().trim();
    }

    public static void ensureCertificatesMounted(Context context) {
        String cmd = "if [ -d /data/local/cacerts ] && [ ! -f /system/etc/security/cacerts/6187b673.0 ]; then " +
                     "mount -o bind /data/local/cacerts /system/etc/security/cacerts; fi";
        runRootCommand(context, cmd);
    }

    public static void runRootCommand(Context context, String cmd) {
        try {
            File suFile = new File(context.getFilesDir(), "mtk-su");
            String suPath = suFile.exists() ? suFile.getAbsolutePath() : "/data/local/tmp/mtk-su";
            String fullCmd = "echo '" + cmd + "' | " + suPath + " -Z u:r:shell:s0";
            Process p = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", fullCmd});
            p.waitFor();
        } catch (Exception ignored) {}
    }

    private static void appendLog(File file, String text) {
        try {
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write((text + "\n").getBytes("UTF-8"));
            fos.flush();
            fos.close();
        } catch (Exception ignored) {}
    }
}
