package com.car.autowirelessadb;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements View.OnClickListener {
    private TextView statusHeader;
    private TextView logView;
    private Button btnFixConnect;
    private Button btnBoost;
    private Button btnEnableAdb;
    private Button btnGoHome;
    private Button btnSleep;
    private Button btnRefresh;
    private Handler mainHandler;

    static class ActionRunner implements Runnable {
        private final MainActivity act;
        private final int actionType;

        ActionRunner(MainActivity act, int actionType) {
            this.act = act;
            this.actionType = actionType;
        }

        @Override
        public void run() {
            switch (actionType) {
                case 1:
                    act.doFixAndConnect();
                    break;
                case 2:
                    act.doEnableAdb();
                    break;
                case 3:
                    act.doReturnHome();
                    break;
                case 4:
                    act.doSleep();
                    break;
                case 5:
                    act.refreshStatus();
                    break;
                case 6:
                    act.doTurboBoost();
                    break;
                case 7:
                    act.doUpdateCerts();
                    break;
                case 8:
                    act.doEnsureCerts();
                    break;
            }
        }
    }

    static class LogPost implements Runnable {
        private final MainActivity act;
        private final String message;

        LogPost(MainActivity act, String message) {
            this.act = act;
            this.message = message;
        }

        @Override
        public void run() {
            if (act.logView != null) {
                String ts = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
                act.logView.append("[" + ts + "] " + message + "\n");
            }
        }
    }

    static class ToastPost implements Runnable {
        private final MainActivity act;
        private final String text;

        ToastPost(MainActivity act, String text) {
            this.act = act;
            this.text = text;
        }

        @Override
        public void run() {
            Toast.makeText(act, text, Toast.LENGTH_SHORT).show();
        }
    }

    static class StatusPost implements Runnable {
        private final MainActivity act;
        private final String statusText;

        StatusPost(MainActivity act, String statusText) {
            this.act = act;
            this.statusText = statusText;
        }

        @Override
        public void run() {
            if (act.statusHeader != null) {
                act.statusHeader.setText(statusText);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainHandler = new Handler(Looper.getMainLooper());
        try {
            startService(new Intent(this, PowerMonitorService.class));
        } catch (Exception ignored) {}

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#101010"));
        root.setPadding(20, 10, 20, 10);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, 8);

        TextView title = new TextView(this);
        title.setText("🚗 Car Headunit Rescue");
        title.setTextSize(20);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        title.setLayoutParams(titleLp);
        header.addView(title);

        btnRefresh = new Button(this);
        btnRefresh.setId(5);
        btnRefresh.setText("🔄 REFRESH");
        btnRefresh.setTextSize(11);
        btnRefresh.setTextColor(Color.parseColor("#CCCCCC"));
        btnRefresh.setBackgroundColor(Color.parseColor("#252525"));
        btnRefresh.setPadding(12, 6, 12, 6);
        btnRefresh.setOnClickListener(this);
        header.addView(btnRefresh);
        root.addView(header);

        // 2-Column Body (Landscape Optimized)
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        body.setLayoutParams(bodyLp);

        // LEFT COLUMN (Info + Log)
        LinearLayout leftCol = new LinearLayout(this);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.05f);
        leftLp.setMargins(0, 0, 10, 0);
        leftCol.setLayoutParams(leftLp);

        // Status Card
        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setBackgroundColor(Color.parseColor("#1E1E1E"));
        statusCard.setPadding(10, 6, 10, 6);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, 6);
        statusCard.setLayoutParams(cardLp);

        statusHeader = new TextView(this);
        statusHeader.setTextSize(11);
        statusHeader.setTextColor(Color.parseColor("#81C784"));
        statusHeader.setTypeface(Typeface.MONOSPACE);
        statusHeader.setText("Detecting network status...");
        statusCard.addView(statusHeader);
        leftCol.addView(statusCard);

        // Log Title
        TextView logTitle = new TextView(this);
        logTitle.setText("Live Diagnostic Log:");
        logTitle.setTextSize(11);
        logTitle.setTextColor(Color.parseColor("#777777"));
        leftCol.addView(logTitle);

        // Log Console
        ScrollView sv = new ScrollView(this);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        sv.setLayoutParams(svLp);
        sv.setBackgroundColor(Color.parseColor("#181818"));
        sv.setPadding(10, 8, 10, 8);

        logView = new TextView(this);
        logView.setTextSize(10);
        logView.setTextColor(Color.parseColor("#00E676"));
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setText("System ready. Tap any button to execute.\n");
        sv.addView(logView);
        leftCol.addView(sv);
        body.addView(leftCol);

        // RIGHT COLUMN (Action Buttons)
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.25f);
        rightLp.setMargins(10, 0, 0, 0);
        rightCol.setLayoutParams(rightLp);

        // Button 1: Big Fix & Launch Android Auto
        btnFixConnect = createCarButton("⚡  FIX & LAUNCH ANDROID AUTO", "#2E7D32", 1, 1.25f, 13);
        rightCol.addView(btnFixConnect);

        // Button 6: Turbo Boost Tablet
        btnBoost = createCarButton("🚀  TURBO BOOST (SPEED UP TABLET)", "#6A1B9A", 6, 0.95f, 12);
        rightCol.addView(btnBoost);

        // Button 7: Update Root CA Certs
        Button btnCerts = createCarButton("🔒  UPDATE ROOT CA CERTS (FIX SSL)", "#00695C", 7, 0.95f, 11);
        rightCol.addView(btnCerts);

        // Button 2: Re-Enable Port 5555
        btnEnableAdb = createCarButton("📶  RE-ENABLE PORT 5555 (WIRELESS ADB)", "#1565C0", 2, 0.95f, 11);
        rightCol.addView(btnEnableAdb);

        // Row for Home & Sleep
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.95f);
        rowParams.setMargins(0, 0, 0, 4);
        row.setLayoutParams(rowParams);

        btnGoHome = createHalfButton("🏠 HOME SCREEN", "#E65100", 3, 11);
        btnSleep = createHalfButton("🌙 SLEEP TABLET", "#37474F", 4, 11);
        row.addView(btnGoHome);
        row.addView(btnSleep);
        rightCol.addView(row);

        body.addView(rightCol);
        root.addView(body);

        setContentView(root);

        new Thread(new ActionRunner(this, 5)).start();

        File certFlag = new File("/data/local/cacerts/6187b673.0");
        if (!certFlag.exists()) {
            new Thread(new ActionRunner(this, 7)).start();
        } else {
            new Thread(new ActionRunner(this, 8)).start();
        }
    }

    private Button createCarButton(String text, String hexColor, int id, float weight, int textSize) {
        Button b = new Button(this);
        b.setId(id);
        b.setText(text);
        b.setTextSize(textSize);
        b.setTextColor(Color.WHITE);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackgroundColor(Color.parseColor(hexColor));
        b.setPadding(4, 0, 4, 0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, weight);
        lp.setMargins(0, 0, 0, 6);
        b.setLayoutParams(lp);
        b.setOnClickListener(this);
        return b;
    }

    private Button createHalfButton(String text, String hexColor, int id, int textSize) {
        Button b = new Button(this);
        b.setId(id);
        b.setText(text);
        b.setTextSize(textSize);
        b.setTextColor(Color.WHITE);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackgroundColor(Color.parseColor(hexColor));
        b.setPadding(2, 0, 2, 0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        lp.setMargins(3, 0, 3, 0);
        b.setLayoutParams(lp);
        b.setOnClickListener(this);
        return b;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        new Thread(new ActionRunner(this, id)).start();
    }

    private void logMsg(String msg) {
        mainHandler.post(new LogPost(this, msg));
    }

    private void showToast(String msg) {
        mainHandler.post(new ToastPost(this, msg));
    }

    private void refreshStatus() {
        String ssid = AdbHelper.getWifiSsid(this);
        String tabIp = AdbHelper.getDeviceIp(this);
        String phoneIp = AdbHelper.getGatewayIp(this);
        boolean adbActive = AdbHelper.isPort5555Active();
        String cpuStatus = AdbHelper.getCpuStatus();

        StringBuilder sb = new StringBuilder();
        sb.append("Wi-Fi SSID  : ").append(ssid).append("\n");
        sb.append("Tablet IP   : ").append(tabIp).append("\n");
        sb.append("Phone (GW)  : ").append(phoneIp != null ? phoneIp : "Not Detected").append("\n");
        sb.append("Wireless ADB: ").append(adbActive ? "PORT 5555 ACTIVE (OK)" : "PORT 5555 INACTIVE").append("\n");
        sb.append("CPU / Therm : ").append(cpuStatus);

        mainHandler.post(new StatusPost(this, sb.toString()));
    }

    private void doFixAndConnect() {
        logMsg("Starting Fix & Connect sequence...");

        logMsg("Checking Wireless ADB port 5555...");
        if (!AdbHelper.isPort5555Active()) {
            logMsg("Enabling Wireless ADB via root...");
            AdbHelper.enableWirelessAdb(this);
        } else {
            logMsg("Wireless ADB is already active.");
        }

        String phoneIp = AdbHelper.getGatewayIp(this);
        logMsg("Phone Hotspot Gateway: " + (phoneIp != null ? phoneIp : "None"));

        if (phoneIp == null) {
            logMsg("ERROR: Tablet is not connected to Phone Hotspot!");
            showToast("Error: Hotspot not connected!");
            refreshStatus();
            return;
        }

        logMsg("Running Turbo Boost pre-launch cleanup...");
        AdbHelper.runTurboBoost(this);
        AdbHelper.applyPowerConnected(this);

        logMsg("Restarting Open Headunit...");
        AdbHelper.stopHeadunit(this);

        try {
            Thread.sleep(600);
        } catch (InterruptedException ignored) {}

        logMsg("Launching projection to " + phoneIp + ":5277...");
        AdbHelper.launchHeadunit(this, phoneIp);
        logMsg("Connection intent dispatched!");
        showToast("Connecting to Android Auto...");

        refreshStatus();
    }

    private void doTurboBoost() {
        logMsg("🚀 Starting Tablet Turbo Boost...");
        showToast("Boosting tablet performance...");
        String res = AdbHelper.runTurboBoost(this);
        AdbHelper.applyPowerConnected(this);
        if (res != null && !res.isEmpty()) {
            for (String line : res.split("\n")) {
                if (!line.trim().isEmpty()) {
                    logMsg("  " + line.trim());
                }
            }
        }
        logMsg("✅ Boost sequence completed!");
        showToast("🚀 Turbo Boost Complete!");
        refreshStatus();
    }

    private void doEnableAdb() {
        logMsg("Re-enabling Wireless ADB (Port 5555)...");
        String res = AdbHelper.enableWirelessAdb(this);
        logMsg("Result: " + res);
        boolean active = AdbHelper.isPort5555Active();
        logMsg("Port 5555 status: " + (active ? "ACTIVE" : "FAILED"));
        showToast(active ? "Port 5555 Active!" : "Failed to activate port 5555");
        refreshStatus();
    }

    private void doReturnHome() {
        logMsg("Exiting Open Headunit and returning to Home...");
        AdbHelper.stopHeadunit(this);
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
        showToast("Returned to Home Screen");
    }

    private void doSleep() {
        logMsg("Putting tablet to sleep...");
        showToast("Putting tablet to sleep...");
        AdbHelper.sleepTablet(this);
    }

    void doUpdateCerts() {
        logMsg("🔒 Updating modern System CA certificates...");
        showToast("Installing modern CA certs...");
        String res = AdbHelper.installModernCertificates(this);
        if (res != null && !res.isEmpty()) {
            for (String line : res.split("\n")) {
                if (!line.trim().isEmpty()) {
                    logMsg("  " + line.trim());
                }
            }
        }
        logMsg("✅ Root certificates updated successfully!");
        showToast("🔒 CA Certs Updated! Modern SSL Active.");
        refreshStatus();
    }

    void doEnsureCerts() {
        AdbHelper.ensureCertificatesMounted(this);
    }
}
