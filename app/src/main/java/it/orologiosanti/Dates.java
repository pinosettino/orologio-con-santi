package it.orologiosanti;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/** Utilita' per le date. Il 29 febbraio esiste solo negli anni bisestili, gestito da Calendar. */
final class Dates {
    private Dates() {}

    /** Oggi alle 00:00. */
    static Calendar today() {
        Calendar c = new GregorianCalendar();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    static Calendar plusDays(Calendar base, int days) {
        Calendar c = (Calendar) base.clone();
        c.add(Calendar.DAY_OF_MONTH, days);
        return c;
    }

    /** Chiave per il database, es. 2026-11-14 (si ordina correttamente come testo). */
    static String key(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    static Calendar fromKey(String key) {
        Calendar c = today();
        try {
            String[] p = key.split("-");
            c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 0, 0, 0);
        } catch (Exception e) {
            // se la chiave e' malformata restituisce oggi
        }
        return c;
    }

    /** Testo mostrato nel widget, es. "14 11 2026". */
    static String label(Calendar c) {
        return String.format(Locale.US, "%02d %02d %04d",
                c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
    }
}
