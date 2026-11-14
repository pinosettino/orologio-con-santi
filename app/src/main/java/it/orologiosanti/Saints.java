package it.orologiosanti;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Legge calendario-santi.json (cartella assets) e restituisce il santo di un giorno. */
final class Saints {
    private Saints() {}

    private static final String[] MESI = {
            "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
            "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre"};

    private static Map<Integer, String> cache;

    /** month: 0-11 (come Calendar.MONTH), day: 1-31. */
    static synchronized String get(Context ctx, int month, int day) {
        if (cache == null) {
            cache = load(ctx.getApplicationContext());
        }
        String s = cache.get(month * 100 + day);
        return s == null ? "" : s;
    }

    private static Map<Integer, String> load(Context ctx) {
        Map<Integer, String> map = new HashMap<Integer, String>();
        try {
            InputStream in = ctx.getAssets().open("calendario-santi.json");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            in.close();
            String json = out.toString("UTF-8");
            if (json.length() > 0 && json.charAt(0) == '\uFEFF') {
                json = json.substring(1);
            }
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                int m = indexOfMonth(o.getString("mese"));
                if (m < 0) {
                    continue;
                }
                map.put(m * 100 + o.getInt("giorno"), o.getString("santo").trim());
            }
        } catch (Exception e) {
            // file mancante o malformato: nessun santo mostrato
        }
        return map;
    }

    private static int indexOfMonth(String name) {
        String n = name.trim().toLowerCase(Locale.ITALIAN);
        for (int i = 0; i < MESI.length; i++) {
            if (MESI[i].equals(n)) {
                return i;
            }
        }
        return -1;
    }
}
