package it.orologiosanti;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Logica comune dei widget (4x2 e 5x2).
 * Le differenze (layout, dimensioni dei caratteri) stanno nelle sottoclassi.
 */
public abstract class ClockWidget extends AppWidgetProvider {

    static final String PREFIX = "it.orologiosanti.";
    static final String A_PREV = PREFIX + "PREV";        // giorno precedente
    static final String A_NEXT = PREFIX + "NEXT";        // giorno successivo
    static final String A_TODAY = PREFIX + "TODAY";      // torna a oggi
    static final String A_NOTE_PREV = PREFIX + "NPREV";  // pezzo precedente dell'appunto
    static final String A_NOTE_NEXT = PREFIX + "NNEXT";  // pezzo successivo dell'appunto
    static final String A_RESET = PREFIX + "RESET";      // ritorno automatico a oggi
    static final String A_THEME = PREFIX + "TEMA";       // alterna testo bianco / nero

    /** Dopo questo tempo senza tocchi, il widget torna da solo a oggi (3 minuti). */
    static final long RESET_MS = 3L * 60L * 1000L;
    static final int MAX_OFFSET = 3660;
    static final int MIN_SP = 11;
    static final int PAD_DP = 10;   // deve coincidere con il padding nei layout

    abstract int layoutRes(boolean nero);
    abstract int defaultWidthDp();
    abstract int noteSp();
    abstract int saintMaxSp();
    abstract int arrowDp();         // larghezza delle frecce dell'appunto (come nel layout)

    // ------------------------------------------------------------------ ciclo di vita

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            update(ctx, mgr, id);
        }
        scheduleMidnight(ctx);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager mgr, int id, Bundle opts) {
        update(ctx, mgr, id);
    }

    @Override
    public void onEnabled(Context ctx) {
        scheduleMidnight(ctx);
    }

    @Override
    public void onDeleted(Context ctx, int[] ids) {
        State st = new State(ctx);
        for (int id : ids) {
            st.clear(id);
        }
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String a = intent.getAction();
        if (a != null && a.startsWith(PREFIX)) {
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                handle(ctx, a, id);
            }
            return;
        }
        super.onReceive(ctx, intent);
    }

    private void handle(Context ctx, String action, int id) {
        State st = new State(ctx);
        if (A_THEME.equals(action)) {
            st.setNero(id, !st.nero(id));
            update(ctx, AppWidgetManager.getInstance(ctx), id);
            return;
        }
        int off = st.offset(id);
        int page = st.page(id);
        long now = System.currentTimeMillis();
        boolean userTouch = true;

        if (A_PREV.equals(action)) {
            off = Math.max(-MAX_OFFSET, off - 1);
            page = 0;
        } else if (A_NEXT.equals(action)) {
            off = Math.min(MAX_OFFSET, off + 1);
            page = 0;
        } else if (A_TODAY.equals(action)) {
            off = 0;
            page = 0;
        } else if (A_NOTE_PREV.equals(action)) {
            page--;
        } else if (A_NOTE_NEXT.equals(action)) {
            page++;
        } else if (A_RESET.equals(action)) {
            userTouch = false;
            if (off != 0 && now - st.touch(id) >= RESET_MS - 5000L) {
                off = 0;
                page = 0;
            } else {
                // allarme arrivato in anticipo o widget gia' a oggi: non cambia nulla
                update(ctx, AppWidgetManager.getInstance(ctx), id);
                return;
            }
        }
        st.save(id, off, page, userTouch ? now : st.touch(id));
        if (userTouch && off != 0) {
            scheduleReset(ctx, id);
        }
        update(ctx, AppWidgetManager.getInstance(ctx), id);
    }

    // ------------------------------------------------------------------ aggiornamento

    /** Aggiorna tutti i widget di tutti i tipi (dopo una modifica agli appunti, a mezzanotte, ecc.). */
    static void updateAll(Context ctx) {
        Context app = ctx.getApplicationContext();
        AppWidgetManager mgr = AppWidgetManager.getInstance(app);
        updateClass(app, mgr, new Widget4x2());
        updateClass(app, mgr, new Widget5x2());
    }

    private static void updateClass(Context ctx, AppWidgetManager mgr, ClockWidget w) {
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, w.getClass()));
        for (int id : ids) {
            w.update(ctx, mgr, id);
        }
    }

    void update(Context ctx, AppWidgetManager mgr, int id) {
        try {
            doUpdate(ctx, mgr, id);
        } catch (RuntimeException e) {
            // meglio lasciare il widget com'e' che farlo andare in crash
        }
    }

    private void doUpdate(Context ctx, AppWidgetManager mgr, int id) {
        Calendar today = Dates.today();
        NotesDb db = NotesDb.get(ctx);
        db.deleteBefore(Dates.key(today));   // appunti di ieri e prima: cancellati

        State st = new State(ctx);
        long now = System.currentTimeMillis();
        int off = st.offset(id);
        int page = st.page(id);
        if (off != 0 && now - st.touch(id) > RESET_MS + 60000L) {
            off = 0;                         // rete di sicurezza se l'allarme e' andato perso
            page = 0;
            st.save(id, 0, 0, now);
        }

        Calendar shown = Dates.plusDays(today, off);
        String key = Dates.key(shown);
        String dateText = Dates.label(shown);
        String saint = Saints.get(ctx, shown.get(Calendar.MONTH), shown.get(Calendar.DAY_OF_MONTH));
        String note = off < 0 ? "" : db.getNote(key);

        // larghezza disponibile
        final DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        int widthDp = defaultWidthDp();
        Bundle opts = mgr.getAppWidgetOptions(id);
        if (opts != null) {
            int w = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
            if (w > 0) {
                widthDp = w;
            }
        }
        float fullPx = dp(dm, widthDp - 2 * PAD_DP) * 0.95f;
        float pagedPx = dp(dm, widthDp - 2 * PAD_DP - 2 * arrowDp()) * 0.95f;

        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.DEFAULT);

        // santo: si rimpicciolisce finche' entra
        int saintSp = saintMaxSp();
        while (saintSp > MIN_SP) {
            paint.setTextSize(sp(dm, saintSp));
            if (paint.measureText(saint) <= fullPx) {
                break;
            }
            saintSp--;
        }

        // appunto: una riga, oppure piu' pezzi con le frecce
        paint.setTextSize(sp(dm, noteSp()));
        TextPager.Meter meter = new TextPager.Meter() {
            @Override
            public float width(String s) {
                return paint.measureText(s);
            }

            @Override
            public int fit(String s, float maxWidth) {
                return paint.breakText(s, true, maxWidth, null);
            }
        };
        List<String> pages;
        boolean multi = false;
        if (note.length() == 0) {
            pages = Collections.singletonList("");
        } else if (meter.width(note) <= fullPx) {
            pages = Collections.singletonList(note);
        } else {
            pages = TextPager.pages(note, meter, pagedPx);
            multi = pages.size() > 1;
            if (pages.isEmpty()) {
                pages = Collections.singletonList("");
            }
        }
        int n = pages.size();
        page = ((page % n) + n) % n;
        st.setPage(id, page);

        // costruzione del widget
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), layoutRes(st.nero(id)));
        rv.setTextViewText(R.id.date, dateText);
        rv.setTextViewText(R.id.saint, saint);
        rv.setTextViewTextSize(R.id.saint, TypedValue.COMPLEX_UNIT_SP, saintSp);
        rv.setTextViewText(R.id.note, pages.get(page));
        rv.setTextViewTextSize(R.id.note, TypedValue.COMPLEX_UNIT_SP, noteSp());
        int arrows = multi ? View.VISIBLE : View.GONE;
        rv.setViewVisibility(R.id.note_prev, arrows);
        rv.setViewVisibility(R.id.note_next, arrows);

        PendingIntent theme = selfBroadcast(ctx, id, A_THEME);   // tocco sull'orologio: cambia tema
        rv.setOnClickPendingIntent(R.id.clock_box, theme);
        rv.setOnClickPendingIntent(R.id.clock_time, theme);
        rv.setOnClickPendingIntent(R.id.clock_sec, theme);
        rv.setOnClickPendingIntent(R.id.date_prev, selfBroadcast(ctx, id, A_PREV));
        rv.setOnClickPendingIntent(R.id.date_next, selfBroadcast(ctx, id, A_NEXT));
        rv.setOnClickPendingIntent(R.id.date, selfBroadcast(ctx, id, A_TODAY));
        rv.setOnClickPendingIntent(R.id.note_prev, selfBroadcast(ctx, id, A_NOTE_PREV));
        rv.setOnClickPendingIntent(R.id.note_next, selfBroadcast(ctx, id, A_NOTE_NEXT));
        if (off >= 0) {
            // si possono scrivere appunti solo per oggi e per i giorni futuri
            Intent edit = new Intent(ctx, EditActivity.class);
            edit.putExtra(EditActivity.EXTRA_KEY, key);
            edit.setData(Uri.parse("orologiosanti://edit/" + key));
            edit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            rv.setOnClickPendingIntent(R.id.note_row,
                    PendingIntent.getActivity(ctx, id, edit, pendingFlags()));
        }

        mgr.updateAppWidget(id, rv);
    }

    // ------------------------------------------------------------------ allarmi

    /** Aggiorna i widget poco dopo la mezzanotte, per cambiare data e santo. */
    static void scheduleMidnight(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        Calendar t = Dates.today();
        t.add(Calendar.DAY_OF_MONTH, 1);
        t.set(Calendar.SECOND, 5);
        Intent i = new Intent(ctx, TimeReceiver.class);
        i.setAction(TimeReceiver.A_MIDNIGHT);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, i, pendingFlags());
        am.set(AlarmManager.RTC, t.getTimeInMillis(), pi);
    }

    private void scheduleReset(Context ctx, int id) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        am.set(AlarmManager.RTC, System.currentTimeMillis() + RESET_MS,
                selfBroadcast(ctx, id, A_RESET));
    }

    // ------------------------------------------------------------------ utilita'

    private PendingIntent selfBroadcast(Context ctx, int id, String action) {
        Intent i = new Intent(ctx, getClass());
        i.setAction(action);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        // l'URI rende ogni PendingIntent distinto (widget + azione)
        i.setData(Uri.parse("orologiosanti://" + getClass().getSimpleName() + "/" + id + "/" + action));
        return PendingIntent.getBroadcast(ctx, id, i, pendingFlags());
    }

    static int pendingFlags() {
        int f = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            f |= PendingIntent.FLAG_IMMUTABLE;
        }
        return f;
    }

    private static float dp(DisplayMetrics dm, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, dm);
    }

    private static float sp(DisplayMetrics dm, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, dm);
    }
}
