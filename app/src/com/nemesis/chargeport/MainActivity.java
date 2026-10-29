package com.nemesis.chargeport;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Settings + status for the MAP triple-press charge-port opener. */
public class MainActivity extends Activity {
    SharedPreferences cfg;
    boolean enabled, requirePark;
    Button enBtn, parkBtn;
    TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        cfg = getSharedPreferences(ChargeReceiver.PREFS, MODE_PRIVATE);
        enabled = cfg.getBoolean("enabled", true);
        requirePark = cfg.getBoolean("require_park", true);

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        root.setPadding(p, p, p, p);
        root.setBackgroundColor(Color.rgb(18, 20, 24));
        sv.addView(root);

        root.addView(tv("Charge Port Button", 24, Color.WHITE, 8));
        root.addView(tv("Triple-press the MAP button to open the charge-port door. "
                + "A single press still opens the map as normal.", 13, Color.rgb(150, 160, 170), 12));

        enBtn = new Button(this);
        enBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { enabled = !enabled; save(); refresh(); }
        });
        root.addView(enBtn);

        parkBtn = new Button(this);
        parkBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { requirePark = !requirePark; save(); refresh(); }
        });
        root.addView(parkBtn);

        root.addView(tv("Test", 16, Color.rgb(120, 180, 255), 8));

        Button open = new Button(this);
        open.setText("Open charge port now");
        open.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread(new Runnable() { public void run() {
                    ChargeReceiver.runRoot("service call com.hkmc.micom.bcm 84 i32 0");
                    post("Charge port: open command sent");
                }}).start();
            }
        });
        root.addView(open);

        Button park = new Button(this);
        park.setText("Check Park status");
        park.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread(new Runnable() { public void run() {
                    int gp = ChargeReceiver.parcelValue(
                            ChargeReceiver.runRoot("service call com.hkmc.micom.system 36"));
                    post("GearP status = " + gp + (gp == 1 ? "  (in Park)" : "  (not Park)"));
                }}).start();
            }
        });
        root.addView(park);

        status = new TextView(this);
        status.setTextColor(Color.rgb(140, 160, 180));
        status.setPadding(0, dp(16), 0, 0);
        root.addView(status);

        setContentView(sv);
        refresh();
    }

    void refresh() {
        enBtn.setText(enabled ? "Automation: ON" : "Automation: OFF");
        parkBtn.setText(requirePark ? "Only in Park: YES" : "Only in Park: NO");
    }

    void save() {
        cfg.edit().putBoolean("enabled", enabled).putBoolean("require_park", requirePark).apply();
    }

    void post(final String s) {
        runOnUiThread(new Runnable() {
            public void run() { status.setText(s); Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show(); }
        });
    }

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    TextView tv(String s, int size, int color, int padDp) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextColor(color); t.setTextSize(size);
        t.setPadding(0, dp(padDp), 0, dp(padDp));
        return t;
    }
}
