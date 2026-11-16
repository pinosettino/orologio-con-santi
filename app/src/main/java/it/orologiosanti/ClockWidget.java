package it.orologiosanti;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.provider.AlarmClock;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Logica comune dei widget (4x2, 5x2 e 5x3).
 * Le differenze (layout, dimensioni dei caratteri) stanno nelle sottoclassi.
 */
public abstract class ClockWidget extends AppWidgetProvider {

    static final String PREFIX = "it.orologiosanti.";
    static final String A_PREV = PREFIX + "PREV";        // giorno precedente
    static final String A_NEXT = PREFIX + "NEXT";        // giorno successivo
    static final String A_TODAY = PREFIX + "TODAY";      // torna a oggi
    static final String A_RESET = PREFIX + "RESET";      // ritorno automatico a oggi

    /** Dopo questo tempo senza tocchi, il widget torna da solo a oggi (3 minuti). */
    static final long RESET_MS = 3L * 60L * 1000L;
    static final int MAX_OFFSET = 3660;
    static final int MIN_SP = 11;
    static final int PAD_DP = 10;       // deve coincidere con il padding nei layout
    static final int MAX_PIECES = 30;   // massimo di pezzi dell'appunto

    /** App Orologio piu' comuni, usate se il telefono non risponde al comando standard. */
    private static final String[] CLOCK_PACKAGES = {
            "com.sec.android.app.clockpackage",
            "com.google.android.deskclock",
            "com.android.deskclock",
            "com.coloros.alarmclock",
            "com.oplus.alarmclock",
            "com.oneplus.deskclock",
            "com.android.BBKClock",
            "com.asus.deskclock",
            "com.lge.clock",
            "com.htc.android.worldclock",
            "com.motorola.blur.alarmclock"};

    abstract int layoutRes(boolean nero);
    abstract int defaultWidthDp();
    abstract int noteSp();
    abstract int saintMaxSp();

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
        int off = st.offset(id);
        long now = System.currentTimeMillis();
        boolean userTouch = true;

        if (A_PREV.equals(action)) {
            off = Math.max(-MAX_OFFSET, off - 1);
        } else if (A_NEXT.equals(action)) {
            off = Math.min(MAX_OFFSET, off + 1);
        } else if (A_TODAY.equals(action)) {
            off = 0;
        } else if (A_RESET.equals(action)) {
            userTouch = false;
            if (off != 0 && now - st.touch(id) >= RESET_MS - 5000L) {
                off = 0;
            } else {
                // allarme arrivato in anticipo o widget gia' a oggi: non cambia nulla
                update(ctx, AppWidgetManager.getInstance(ctx), id);
                return;
            }
        }
        st.save(id, off, userTouch ? now : st.touch(id));
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
        updateClass(app, mgr, new Widget5x3());
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
        if (off != 0 && now - st.touch(id) > RESET_MS + 60000L) {
            off = 0;                         // rete di sicurezza se l'allarme e' andato perso
            st.save(id, 0, now);
        }
        boolean nero = st.nero();

        Calendar shown = Dates.plusDays(today, off);
        String key = Dates.key(shown);
        String dateText = Dates.widgetLabel(shown);
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

        // appunto: pezzi che scorrono da soli
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
        // il messaggio fisso (valido ogni giorno) si accoda all'appunto del giorno
        String fixed = FixedNote.get(ctx);
        String all = note;
        if (fixed.length() > 0) {
            all = note.length() > 0 ? note + "#" + fixed : fixed;
        }
        List<String> pieces = TextPager.build(all, meter, fullPx, MAX_PIECES);

        // costruzione del widget
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), layoutRes(nero));
        rv.setTextViewText(R.id.date, dateText);
        rv.setTextViewText(R.id.saint, saint);
        rv.setTextViewTextSize(R.id.saint, TypedValue.COMPLEX_UNIT_SP, saintSp);

        rv.removeAllViews(R.id.note_flipper);
        for (String piece : pieces) {
            RemoteViews item = new RemoteViews(ctx.getPackageName(),
                    nero ? R.layout.note_piece_nero : R.layout.note_piece);
            item.setTextViewText(R.id.note_piece, piece);
            item.setTextViewTextSize(R.id.note_piece, TypedValue.COMPLEX_UNIT_SP, noteSp());
            rv.addView(R.id.note_flipper, item);
        }

        // prossima sveglia (sotto la data)
        String alarm = nextAlarmText(ctx);
        if (alarm != null) {
            rv.setTextViewText(R.id.alarm_text, alarm);
            rv.setViewVisibility(R.id.alarm_row, View.VISIBLE);
        } else {
            rv.setViewVisibility(R.id.alarm_row, View.GONE);
        }

        // tocchi: zone invisibili ai lati della data, data, orologio, riga dell'appunto
        rv.setOnClickPendingIntent(R.id.zone_prev, selfBroadcast(ctx, id, A_PREV));
        rv.setOnClickPendingIntent(R.id.zone_next, selfBroadcast(ctx, id, A_NEXT));
        rv.setOnClickPendingIntent(R.id.date, selfBroadcast(ctx, id, A_TODAY));

        Intent clock = clockIntent(ctx);
        if (clock != null) {
            clock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(ctx, id, clock, pendingFlags());
            rv.setOnClickPendingIntent(R.id.clock_time, pi);
            rv.setOnClickPendingIntent(R.id.alarm_row, pi);
        }

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

    // ------------------------------------------------------------------ sveglia e app Orologio

    /** Orario della prossima sveglia (letto dal sistema), oppure null se non ce ne sono. */
    private static String nextAlarmText(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return null;
        }
        AlarmManager.AlarmClockInfo info = am.getNextAlarmClock();
        if (info == null) {
            return null;
        }
        long trigger = info.getTriggerTime();
        Calendar t = new GregorianCalendar();
        t.setTimeInMillis(trigger);
        String hm = String.format(Locale.US, "%02d:%02d",
                t.get(Calendar.HOUR_OF_DAY), t.get(Calendar.MINUTE));
        boolean within24h = trigger - System.currentTimeMillis() < 24L * 60L * 60L * 1000L;
        if (within24h) {
            return hm;
        }
        return Dates.GIORNI[t.get(Calendar.DAY_OF_WEEK) - 1] + " " + hm;
    }

    /**
     * Comando standard "mostra le sveglie": lo gestisce l'app Orologio predefinita del telefono,
     * qualunque marca sia. Se non risponde, prova ad aprire una delle app Orologio note.
     */
    private static Intent clockIntent(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Intent show = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        if (pm.resolveActivity(show, 0) != null) {
            return show;
        }
        for (String p : CLOCK_PACKAGES) {
            Intent launch = pm.getLaunchIntentForPackage(p);
            if (launch != null) {
                return launch;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ allarmi interni

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
