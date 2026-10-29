package com.nemesis.chargeport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Triple-press MAP -> open the charge-port door (in Park).
 * Counts MAP_SHORTCUT broadcasts; 3 within WINDOW_MS of each other fires.
 */
public class ChargeReceiver extends BroadcastReceiver {
    static final String PREFS = "cfg";
    static final String NEM = "/system/xbin/nemesis";
    static final long WINDOW_MS = 1500;   // max gap between consecutive presses
    static final int NEED = 3;            // presses required

    @Override
    public void onReceive(final Context ctx, Intent intent) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!p.getBoolean("enabled", true)) return;

        long now = SystemClock.elapsedRealtime();
        long t1 = p.getLong("t1", 0);
        long t2 = p.getLong("t2", 0);
        boolean triple = (t1 > 0 && t2 > 0
                && (now - t2) <= WINDOW_MS
                && (t2 - t1) <= WINDOW_MS);

        if (triple) {
            p.edit().putLong("t1", 0).putLong("t2", 0).apply();   // reset
            final boolean requirePark = p.getBoolean("require_park", true);
            final PendingResult pr = goAsync();
            new Thread(new Runnable() {
                public void run() {
                    try { fire(ctx, requirePark); }
                    finally { pr.finish(); }
                }
            }).start();
        } else {
            p.edit().putLong("t1", t2).putLong("t2", now).apply();  // slide history
        }
    }

    void fire(Context ctx, boolean requirePark) {
        if (requirePark) {
            int gp = parcelValue(runRoot("service call com.hkmc.micom.system 36"));
            if (gp != 1) { toast(ctx, "Charge port: not in Park (" + gp + ")"); return; }
        }
        runRoot("service call com.hkmc.micom.bcm 84 i32 0");
        toast(ctx, "Opening charge port");
    }

    static String runRoot(String cmd) {
        StringBuilder out = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(new String[] { NEM, "root", "-c", cmd });
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) out.append(line).append("\n");
            p.waitFor();
        } catch (Exception e) {
            Log.e("CHARGEPORT", "runRoot: " + e.getMessage());
            return "ERR " + e.getMessage();
        }
        return out.toString();
    }

    /** Parse the value word out of `service call` output: Parcel(00000000 0000000X ...) -> X. */
    static int parcelValue(String out) {
        if (out == null) return -1;
        int i = out.indexOf("Parcel(");
        if (i < 0) return -1;
        int j = out.indexOf(')', i);
        String inside = out.substring(i + 7, j < 0 ? out.length() : j);
        String[] t = inside.trim().split("[^0-9a-fA-F]+");
        if (t.length >= 2) {
            try { return (int) Long.parseLong(t[1], 16); } catch (Exception e) {}
        }
        return -1;
    }

    static void toast(final Context ctx, final String msg) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() { Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show(); }
        });
    }
}
