package it.orologiosanti;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;

/**
 * Finestra con tastiera: appunto del giorno + messaggio fisso (valido ogni giorno).
 * Per separare piu' messaggi si usa il simbolo #.
 */
public class EditActivity extends Activity {
    static final String EXTRA_KEY = "key";
    static final int MAX_CHARS = 300;

    private String key;
    private EditText dayInput;
    private EditText fixedInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        key = getIntent().getStringExtra(EXTRA_KEY);
        if (key == null || key.compareTo(Dates.key(Dates.today())) < 0) {
            finish();   // giorni passati: non modificabili
            return;
        }
        Calendar day = Dates.fromKey(key);
        String saint = Saints.get(this, day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH));

        final float d = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * d);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Appunti del " + Dates.label(day));
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        if (saint.length() > 0) {
            TextView s = new TextView(this);
            s.setText(saint);
            s.setTextSize(14);
            root.addView(s);
        }

        // ---- appunto del giorno
        root.addView(label("Appunto di questo giorno", d, 14));
        dayInput = makeInput("Scrivi qui l'appunto del giorno", NotesDb.get(this).getNote(key));
        root.addView(dayInput, fullWidth(d, 4));
        root.addView(clearButton(dayInput));

        // ---- messaggio fisso
        root.addView(label("Messaggio fisso (si ripete ogni giorno)", d, 6));
        fixedInput = makeInput("Resta finche' non lo cambi o lo cancelli", FixedNote.get(this));
        root.addView(fixedInput, fullWidth(d, 4));
        root.addView(clearButton(fixedInput));

        TextView tip = new TextView(this);
        tip.setText("Per piu' messaggi nella stessa casella usa #  (es.: Dentista 15:30 # Chiamare Marco). "
                + "Il messaggio fisso viene dopo l'appunto del giorno.");
        tip.setTextSize(12);
        tip.setTextColor(Color.DKGRAY);
        tip.setPadding(0, (int) (4 * d), 0, (int) (8 * d));
        root.addView(tip);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);
        Button cancel = new Button(this);
        cancel.setText("Annulla");
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        Button ok = new Button(this);
        ok.setText("Salva");
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        buttons.addView(cancel);
        buttons.addView(ok);
        root.addView(buttons);

        ScrollView sv = new ScrollView(this);
        sv.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(sv);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        getWindow().setLayout((int) (dm.widthPixels * 0.94f), ViewGroup.LayoutParams.WRAP_CONTENT);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        dayInput.requestFocus();
        dayInput.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(dayInput, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 250);
    }

    private TextView label(String text, float d, int topDp) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setTypeface(null, Typeface.BOLD);
        t.setPadding(0, (int) (topDp * d) + (int) (8 * d), 0, 0);
        return t;
    }

    private LinearLayout.LayoutParams fullWidth(float d, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (topDp * d);
        return lp;
    }

    private EditText makeInput(String hint, String text) {
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.setImeOptions(EditorInfo.IME_ACTION_DONE);
        et.setHorizontallyScrolling(false);
        et.setMaxLines(4);
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_CHARS)});
        et.setHint(hint);
        et.setText(text);
        et.setSelection(et.getText().length());
        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    save();
                    return true;
                }
                return false;
            }
        });
        return et;
    }

    /** Pulsante piccolo a destra che svuota la casella (la modifica vale dopo "Salva"). */
    private LinearLayout clearButton(final EditText target) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        Button b = new Button(this);
        b.setText("Cancella");
        b.setTextSize(12);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                target.setText("");
                target.requestFocus();
            }
        });
        row.addView(b);
        return row;
    }

    private static String clean(EditText et) {
        // una sola riga: a capo e spazi multipli diventano uno spazio
        return et.getText().toString().replaceAll("\\s+", " ").trim();
    }

    private void save() {
        NotesDb.get(this).setNote(key, clean(dayInput));
        FixedNote.set(this, clean(fixedInput));
        ClockWidget.updateAll(this);
        finish();
    }
}
