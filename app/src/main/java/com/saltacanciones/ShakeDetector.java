package com.saltacanciones;

/** Reconoce dos sacudidas fuertes y seguidas. */
public final class ShakeDetector {
    public static final double THRESHOLD_G = 2.3;
    public static final long MIN_GAP_MS = 250;
    public static final long MAX_GAP_MS = 900;
    public static final long COOLDOWN_MS = 1500;

    private long first = -1;
    private long blockedUntil;
    private boolean above;

    /** g: aceleración total en unidades de gravedad. True al completar el gesto. */
    public boolean sample(double g, long now) {
        boolean strong = g >= THRESHOLD_G;
        boolean start = strong && !above;
        above = strong;
        if (!start || now < blockedUntil) return false;
        if (first >= 0 && now - first < MIN_GAP_MS) return false;
        if (first >= 0 && now - first <= MAX_GAP_MS) {
            first = -1;
            blockedUntil = now + COOLDOWN_MS;
            return true;
        }
        first = now;
        return false;
    }
}
