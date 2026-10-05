package com.car.autowirelessadb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Date;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "AutoWirelessADB";

    static class Runner implements Runnable {
        private final Context ctx;
        private final PendingResult pr;
        private final String action;

        Runner(Context context, PendingResult pendingResult, String action) {
            this.ctx = context;
            this.pr = pendingResult;
            this.action = action;
        }

        @Override
        public void run() {
            File logFile = new File(ctx.getFilesDir(), "app.log");
            appendLog(logFile, ">>> Runner for action: " + action + " at " + new Date());

            try {
                if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                    "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action) ||
                    "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

                    for (int i = 0; i < 45; i++) {
                        String status = readProp("sys.boot_completed");
                        if ("1".equals(status)) {
                            appendLog(logFile, "sys.boot_completed=1 reached!");
                            break;
                        }
                        Thread.sleep(1000);
                    }
                    // Wait for UsbDeviceManager and framework boot animations to finish
                    Thread.sleep(5000);
                }

                // Enable Wireless ADB
                AdbHelper.enableWirelessAdb(ctx);

                // Mount modern CA certificates store
                AdbHelper.ensureCertificatesMounted(ctx);

                // Ensure PowerMonitorService is running to manage the 15-second disconnected deep sleep
                try {
                    Intent serviceIntent = new Intent(ctx, PowerMonitorService.class);
                    if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                        serviceIntent.setAction("ACTION_POWER_DISCONNECTED");
                    } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                        serviceIntent.setAction("ACTION_POWER_CONNECTED");
                    }
                    ctx.startService(serviceIntent);
                } catch (Exception ignored) {}

                // If power connected or booted, crank brightness to max and keep screen awake
                if (Intent.ACTION_POWER_CONNECTED.equals(action) ||
                    Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                    "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action) ||
                    "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
                    AdbHelper.applyPowerConnected(ctx);
                }

                // For boot actions, run a second pass after 10s to guarantee it survives late USB resets
                if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                    Thread.sleep(10000);
                    AdbHelper.enableWirelessAdb(ctx);
                    AdbHelper.applyPowerConnected(ctx);
                }

            } catch (Exception e) {
                appendLog(logFile, "Runner exception: " + e);
            } finally {
                if (pr != null) {
                    pr.finish();
                }
                appendLog(logFile, "<<< Runner finished for action: " + action);
            }
        }

        private String readProp(String propName) {
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"getprop", propName});
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = r.readLine();
                p.waitFor();
                return line != null ? line.trim() : "";
            } catch (Exception e) {
                return "";
            }
        }

        private void appendLog(File file, String text) {
            try {
                FileOutputStream fos = new FileOutputStream(file, true);
                fos.write((text + "\n").getBytes("UTF-8"));
                fos.flush();
                fos.close();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : "UNKNOWN";
        PendingResult pr = goAsync();
        new Thread(new Runner(context, pr, action)).start();
    }
}
