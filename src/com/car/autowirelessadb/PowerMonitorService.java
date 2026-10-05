package com.car.autowirelessadb;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

public class PowerMonitorService extends Service {
    private static final String TAG = "PowerMonitorService";
    private static final long SLEEP_DEBOUNCE_DELAY_MS = 15000; // 15 seconds

    private Handler handler;
    private PowerBroadcastReceiver powerReceiver;
    private SleepRunnable sleepRunnable;

    static class PowerBroadcastReceiver extends BroadcastReceiver {
        private final PowerMonitorService service;

        PowerBroadcastReceiver(PowerMonitorService service) {
            this.service = service;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || service == null) return;
            String action = intent.getAction();
            service.handlePowerEvent(action);
        }
    }

    static class SleepRunnable implements Runnable {
        private final PowerMonitorService service;

        SleepRunnable(PowerMonitorService service) {
            this.service = service;
        }

        @Override
        public void run() {
            if (service == null) return;
            service.executeDeepSleepCheck();
        }
    }

    static class WakeupRunnable implements Runnable {
        private final Context ctx;

        WakeupRunnable(Context context) {
            this.ctx = context;
        }

        @Override
        public void run() {
            AdbHelper.applyPowerConnected(ctx);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        sleepRunnable = new SleepRunnable(this);
        logToFile("PowerMonitorService created at " + new Date());

        powerReceiver = new PowerBroadcastReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(powerReceiver, filter);

        if (AdbHelper.isPowerConnected(this)) {
            logToFile("Initial state: Power connected. Applying max brightness & awake.");
            new Thread(new WakeupRunnable(this)).start();
        }
    }

    public void handlePowerEvent(String action) {
        logToFile("Power event received: " + action);

        if (Intent.ACTION_POWER_DISCONNECTED.equals(action) || "ACTION_POWER_DISCONNECTED".equals(action)) {
            logToFile("Power DISCONNECTED. Scheduling deep sleep in 15 seconds...");
            handler.removeCallbacks(sleepRunnable);
            handler.postDelayed(sleepRunnable, SLEEP_DEBOUNCE_DELAY_MS);
        } else if (Intent.ACTION_POWER_CONNECTED.equals(action) || "ACTION_POWER_CONNECTED".equals(action)) {
            logToFile("Power CONNECTED. Cancelling pending sleep and restoring full performance!");
            handler.removeCallbacks(sleepRunnable);
            new Thread(new WakeupRunnable(this)).start();
        }
    }

    public void executeDeepSleepCheck() {
        logToFile("15-second disconnected timer expired. Verifying power state...");
        boolean stillConnected = AdbHelper.isPowerConnected(this);
        logToFile("Current power connected state: " + stillConnected);

        if (!stillConnected) {
            logToFile("Confirmed disconnected for 15s. Triggering Ultra-Low Power Deep Sleep Mode!");
            AdbHelper.applyDeepSleep(this);
        } else {
            logToFile("Power reconnected before sleep executed. Deep sleep aborted.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handlePowerEvent(intent.getAction());
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        logToFile("PowerMonitorService destroying.");
        if (powerReceiver != null) {
            try {
                unregisterReceiver(powerReceiver);
            } catch (Exception ignored) {}
        }
        if (handler != null && sleepRunnable != null) {
            handler.removeCallbacks(sleepRunnable);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void logToFile(String msg) {
        Log.i(TAG, msg);
        try {
            File logFile = new File(getFilesDir(), "power.log");
            FileOutputStream fos = new FileOutputStream(logFile, true);
            String line = "[" + new Date() + "] " + msg + "\n";
            fos.write(line.getBytes("UTF-8"));
            fos.flush();
            fos.close();
        } catch (Exception ignored) {}
    }
}
