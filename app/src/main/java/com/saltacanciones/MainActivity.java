package com.saltacanciones;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private Button button;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        TextView text = new TextView(this);
        text.setText("Con música sonando:\n\n"
                + "Subir volumen 2 veces → siguiente canción\n\n"
                + "Bajar volumen 2 veces → canción anterior\n");
        text.setTextSize(18);
        text.setGravity(Gravity.CENTER);
        layout.addView(text);

        button = new Button(this);
        button.setOnClickListener(v -> {
            Intent service = new Intent(this, VolumeService.class);
            if (VolumeService.running) {
                stopService(service);
                button.setText("Activar");
            } else {
                startForegroundService(service);
                button.setText("Desactivar");
            }
        });
        layout.addView(button);
        setContentView(layout);

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        button.setText(VolumeService.running ? "Desactivar" : "Activar");
    }
}
