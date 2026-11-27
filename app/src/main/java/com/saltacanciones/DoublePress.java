package com.saltacanciones;

/**
 * Reconoce una doble pulsación. Mantener el botón apretado genera 3 o más
 * cambios seguidos, así que la acción solo se confirma si no llega un tercero.
 */
public final class DoublePress {
    public static final long MIN_GAP_MS = 60;
    public static final long MAX_GAP_MS = 400;

    private int count;
    private int direction;
    private long last;
    private boolean valid;

    /** Registra una pulsación. dir: +1 subir, -1 bajar. */
    public void press(int dir, long now) {
        long gap = now - last;
        if (count > 0 && dir == direction && gap <= MAX_GAP_MS) {
            count++;
            valid = valid && gap >= MIN_GAP_MS;
        } else {
            count = 1;
            direction = dir;
            valid = true;
        }
        last = now;
    }

    /** Llamar MAX_GAP_MS después de la última pulsación. Devuelve +1, -1 o 0. */
    public int resolve() {
        int result = (count == 2 && valid) ? direction : 0;
        count = 0;
        return result;
    }

    /** Cantidad de pulsaciones de la secuencia actual. */
    public int count() { return count; }
}
