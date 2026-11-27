package com.saltacanciones;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("ajustes", MODE_PRIVATE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        addSwitch(layout, "volumen", "Cambiar canción con el volumen",
                "Con música: 2× subir = siguiente, 2× bajar = anterior");
        addSwitch(layout, "agitar", "Agitar para la linterna",
                "Agita 2 veces para encender la linterna y otras 2 para apagarla");

        setContentView(layout);

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private void addSwitch(LinearLayout layout, String key, String title, String help) {
        Switch option = new Switch(this);
        option.setText(title);
        option.setTextSize(18);
        option.setChecked(prefs.getBoolean(key, false));
        option.setOnCheckedChangeListener((v, on) -> {
            prefs.edit().putBoolean(key, on).apply();
            updateService();
        });
        layout.addView(option);
        TextView text = new TextView(this);
        text.setText(help + "\n");
        layout.addView(text);
    }

    private void updateService() {
        Intent service = new Intent(this, VolumeService.class);
        if (prefs.getBoolean("volumen", false) || prefs.getBoolean("agitar", false)) {
            startForegroundService(service);
        } else {
            stopService(service);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateService();
    }
}
