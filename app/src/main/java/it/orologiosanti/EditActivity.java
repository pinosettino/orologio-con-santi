package it.orologiosanti;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

/** Finestra con tastiera per scrivere l'appunto di un giorno. */
public class EditActivity extends Activity {
    static final String EXTRA_KEY = "key";
    static final int MAX_CHARS = 300;

    private String key;
    private EditText input;

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
        title.setText("Appunto del " + Dates.label(day));
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        if (saint.length() > 0) {
            TextView s = new TextView(this);
            s.setText(saint);
            s.setTextSize(14);
            root.addView(s);
        }

        input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setHorizontallyScrolling(false);
        input.setMaxLines(4);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_CHARS)});
        input.setHint("Scrivi qui l'appunto (una riga)");
        input.setText(NotesDb.get(this).getNote(key));
        input.setSelection(input.getText().length());
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    save();
                    return true;
                }
                return false;
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (12 * d);
        root.addView(input, lp);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);

        Button delete = new Button(this);
        delete.setText("Elimina");
        delete.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                input.setText("");
                save();
            }
        });
        Button cancel = new Button(this);
        cancel.setText("Annulla");
        cancel.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                finish();
            }
        });
        Button ok = new Button(this);
        ok.setText("Salva");
        ok.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                save();
            }
        });
        buttons.addView(delete);
        buttons.addView(cancel);
        buttons.addView(ok);
        root.addView(buttons);

        setContentView(root);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        getWindow().setLayout((int) (dm.widthPixels * 0.94f), ViewGroup.LayoutParams.WRAP_CONTENT);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        input.requestFocus();
        input.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 250);
    }

    private void save() {
        // una sola riga: a capo e spazi multipli diventano uno spazio
        String text = input.getText().toString().replaceAll("\\s+", " ").trim();
        NotesDb.get(this).setNote(key, text);
        ClockWidget.updateAll(this);
        finish();
    }
}
