package it.orologiosanti;

import android.content.Context;
import android.content.SharedPreferences;

/** Messaggio fisso: resta ogni giorno finche' non viene modificato o cancellato. */
final class FixedNote {
    private FixedNote() {}

    private static SharedPreferences sp(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences("fisso", Context.MODE_PRIVATE);
    }

    static String get(Context ctx) {
        return sp(ctx).getString("testo", "");
    }

    /** Testo vuoto = messaggio cancellato. */
    static void set(Context ctx, String text) {
        SharedPreferences.Editor e = sp(ctx).edit();
        if (text == null || text.trim().length() == 0) {
            e.remove("testo");
        } else {
            e.putString("testo", text.trim());
        }
        e.apply();
    }
}
