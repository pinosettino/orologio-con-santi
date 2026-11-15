package it.orologiosanti;

import android.content.Context;
import android.content.SharedPreferences;

/** Stato dei widget: di quanti giorni e' spostata la data, ultimo tocco, colore del testo. */
final class State {
    private final SharedPreferences sp;

    State(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences("stato", Context.MODE_PRIVATE);
    }

    int offset(int id) {
        return sp.getInt("off_" + id, 0);
    }

    long touch(int id) {
        return sp.getLong("tocco_" + id, 0L);
    }

    void save(int id, int offset, long touch) {
        sp.edit().putInt("off_" + id, offset).putLong("tocco_" + id, touch).apply();
    }

    void clear(int id) {
        sp.edit().remove("off_" + id).remove("tocco_" + id).apply();
    }

    /** Colore del testo, uguale per tutti i widget: true = nero, false = bianco (predefinito). */
    boolean nero() {
        return sp.getBoolean("tema_nero", false);
    }

    void setNero(boolean nero) {
        sp.edit().putBoolean("tema_nero", nero).apply();
    }
}
