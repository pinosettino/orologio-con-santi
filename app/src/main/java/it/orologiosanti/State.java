package it.orologiosanti;

import android.content.Context;
import android.content.SharedPreferences;

/** Stato di ogni widget: di quanti giorni e' spostata la data, pagina dell'appunto, ultimo tocco. */
final class State {
    private final SharedPreferences sp;

    State(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences("stato", Context.MODE_PRIVATE);
    }

    int offset(int id) {
        return sp.getInt("off_" + id, 0);
    }

    int page(int id) {
        return sp.getInt("pag_" + id, 0);
    }

    long touch(int id) {
        return sp.getLong("tocco_" + id, 0L);
    }

    /** true = testo nero, false = testo bianco (predefinito). */
    boolean nero(int id) {
        return sp.getBoolean("nero_" + id, false);
    }

    void setNero(int id, boolean nero) {
        sp.edit().putBoolean("nero_" + id, nero).apply();
    }

    void save(int id, int offset, int page, long touch) {
        sp.edit().putInt("off_" + id, offset).putInt("pag_" + id, page)
                .putLong("tocco_" + id, touch).apply();
    }

    void setPage(int id, int page) {
        sp.edit().putInt("pag_" + id, page).apply();
    }

    void clear(int id) {
        sp.edit().remove("off_" + id).remove("pag_" + id).remove("tocco_" + id)
                .remove("nero_" + id).apply();
    }
}
