package com.saltacanciones;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.PowerManager;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;

/** Escucha los cambios del volumen multimedia, también con la pantalla apagada. */
public class VolumeService extends Service {
    private static final String VOLUME_ACTION = "android.media.VOLUME_CHANGED_ACTION";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DoublePress detector = new DoublePress();
    private AudioManager audio;
    private int volumeBefore;      // volumen antes de la primera pulsación
    private SharedPreferences prefs;
    private PowerManager power;
    private SensorManager sensors;
    private PowerManager.WakeLock cpuLock;  // mantiene el sensor activo con la pantalla apagada
    private final ShakeDetector shake = new ShakeDetector();
    private boolean listeningShake;

    private final SensorEventListener shakeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent e) {
            float x = e.values[0], y = e.values[1], z = e.values[2];
            double g = Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
            if (shake.sample(g, SystemClock.elapsedRealtime())) setTorch(!torchOn);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    };

    private CameraManager camera;
    private String torchId;        // cámara que tiene flash
    private boolean torchOn;

    private final CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeChanged(String id, boolean enabled) {
            if (id.equals(torchId)) torchOn = enabled;
        }
    };

    private String findTorch() {
        try {
            for (String id : camera.getCameraIdList()) {
                Boolean flash = camera.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Boolean.TRUE.equals(flash)) return id;
            }
        } catch (CameraAccessException e) {
            return null;
        }
        return null;
    }

    private void setTorch(boolean on) {
        if (torchId == null) return;
        try {
            camera.setTorchMode(torchId, on);
            torchOn = on;
        } catch (CameraAccessException | IllegalArgumentException e) {
            // La cámara está en uso por otra app.
        }
    }

    private void setShake(boolean on) {
        if (on == listeningShake) return;
        if (on) {
            Sensor accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer == null) return;
            sensors.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        } else {
            sensors.unregisterListener(shakeListener);
            if (torchOn) setTorch(false);
        }
        listeningShake = on;
    }

    private final Runnable resolve = () -> {
        int dir = detector.resolve();
        if (!prefs.getBoolean("volumen", false)) return;
        if (dir == 0) {            // pulsación normal: el volumen queda como está
            keepAwayFromLimits();
            return;
        }
        setVolume(volumeBefore);   // la doble pulsación no altera el volumen
        sendKey(dir > 0 ? KeyEvent.KEYCODE_MEDIA_NEXT : KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        keepAwayFromLimits();
    };

    private static final long POLL_MS = 40;
    private int lastVolume;
    private boolean polling;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkVolume();
        }
    };

    /** Revisa el volumen sin depender de los avisos, que pueden fallar con la pantalla apagada. */
    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            checkVolume();
            handler.postDelayed(this, POLL_MS);
        }
    };

    private void checkVolume() {
        int now = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int old = lastVolume;
        if (now == old) return;
        lastVolume = now;
        if (!prefs.getBoolean("volumen", false) || !audio.isMusicActive()) return;

        int dir = now > old ? 1 : -1;
        long time = SystemClock.elapsedRealtime();
        detector.press(dir, time);
        if (detector.count() == 1) volumeBefore = old;
        handler.removeCallbacks(resolve);
        handler.postDelayed(resolve, DoublePress.MAX_GAP_MS);
    }

    private void setVolumeGesture(boolean on) {
        if (on == polling) return;
        if (on) {
            lastVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            handler.post(poll);
        } else {
            handler.removeCallbacks(poll);
            handler.removeCallbacks(resolve);
            detector.resolve();
        }
        polling = on;
    }

    /** El procesador debe seguir despierto para detectar gestos con la pantalla apagada. */
    private void updateWakeLock() {
        boolean needed = polling || listeningShake;
        if (needed && !cpuLock.isHeld()) cpuLock.acquire();
        else if (!needed && cpuLock.isHeld()) cpuLock.release();
    }

    private void setVolume(int value) {
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == value) return;
        try {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
            // Cambio propio: no cuenta como pulsación.
            lastVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        } catch (SecurityException e) {
            // p. ej. con No molestar activo
        }
    }

    /** En el máximo o en cero el botón no cambia nada y no se podría detectar. */
    private void keepAwayFromLimits() {
        if (!prefs.getBoolean("volumen", false) || !audio.isMusicActive()) return;
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int v = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (v >= max) setVolume(max - 1);
        else if (v <= 0) setVolume(1);
    }

    private void sendKey(int code) {
        audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        prefs = getSharedPreferences("ajustes", MODE_PRIVATE);
        power = getSystemService(PowerManager.class);
        sensors = getSystemService(SensorManager.class);
        cpuLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "saltacanciones:sensor");
        camera = getSystemService(CameraManager.class);
        torchId = findTorch();
        camera.registerTorchCallback(torchCallback, handler);
        NotificationChannel channel = new NotificationChannel(
                "servicio", "Servicio activo", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification notification = new Notification.Builder(this, "servicio")
                .setSmallIcon(R.drawable.ic_skip)
                .setContentTitle("Salta Canciones activo")
                .setContentText("Gestos activados")
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notification);
        }
        IntentFilter filter = new IntentFilter(VOLUME_ACTION);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setVolumeGesture(prefs.getBoolean("volumen", false));
        setShake(prefs.getBoolean("agitar", false));
        updateWakeLock();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        setShake(false);
        setVolumeGesture(false);
        updateWakeLock();
        camera.unregisterTorchCallback(torchCallback);
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
