package it.orologiosanti;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Schermata dell'app: scelta del colore del testo e istruzioni d'uso. */
public class MainActivity extends Activity {

    private LinearLayout previewBox;
    private TextView previewText;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final float d = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Orologio Santi");
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView intro = new TextView(this);
        intro.setText("Questa e' solo l'app che contiene i widget: non devi usarla, "
                + "se non per scegliere il colore del testo.");
        intro.setPadding(0, (int) (8 * d), 0, (int) (16 * d));
        root.addView(intro);

        // ---- colore del testo
        TextView h1 = new TextView(this);
        h1.setText("Colore del testo");
        h1.setTextSize(18);
        h1.setTypeface(null, Typeface.BOLD);
        root.addView(h1);

        status = new TextView(this);
        status.setPadding(0, (int) (4 * d), 0, (int) (8 * d));
        root.addView(status);

        previewBox = new LinearLayout(this);
        previewBox.setOrientation(LinearLayout.VERTICAL);
        previewBox.setPadding((int) (14 * d), (int) (10 * d), (int) (14 * d), (int) (10 * d));
        previewText = new TextView(this);
        previewText.setTextSize(18);
        previewText.setText("16:45                    DOM 15 NOV\nS. Alberto Magno\nAppuntamento dentista 15:30");
        previewBox.addView(previewText);
        root.addView(previewBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.START);
        Button white = new Button(this);
        white.setText("Testo bianco");
        white.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choose(false);
            }
        });
        Button black = new Button(this);
        black.setText("Testo nero");
        black.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choose(true);
            }
        });
        buttons.addView(white);
        buttons.addView(black);
        root.addView(buttons);

        // ---- istruzioni
        TextView h2 = new TextView(this);
        h2.setText("Come si usa");
        h2.setTextSize(18);
        h2.setTypeface(null, Typeface.BOLD);
        h2.setPadding(0, (int) (20 * d), 0, (int) (6 * d));
        root.addView(h2);

        TextView help = new TextView(this);
        help.setText(
                "Aggiungere il widget: tieni premuto su uno spazio vuoto della Home, scegli "
                + "\"Widget\", cerca \"Orologio Santi\" e trascina \"Orologio 4x2\" o \"Orologio 5x2\".\n\n"
                + "ORA: toccando l'orologio si apre l'app Orologio del telefono "
                + "(sveglie, timer, cronometro).\n\n"
                + "DATA: tocca a sinistra della data per il giorno precedente, a destra per il "
                + "successivo. Toccando la data si torna a oggi. Dopo 3 minuti il widget torna "
                + "da solo a oggi. Le zone da toccare non si vedono.\n\n"
                + "SVEGLIA: sotto la data compare l'orario della prossima sveglia, se ce n'e' una. "
                + "Toccala per aprire l'elenco delle sveglie.\n\n"
                + "APPUNTO: tocca la riga sotto il santo per scrivere. L'appunto scorre da solo, "
                + "un pezzo ogni 3 secondi. Per mostrare piu' messaggi separali con la barra "
                + "verticale |, per esempio:\n"
                + "Dentista 15:30 | Chiamare Marco\n"
                + "Ogni messaggio viene mostrato da solo; se e' lungo, a pezzi, e solo dopo "
                + "passa al successivo. Si puo' scrivere solo da oggi in avanti. Gli appunti dei "
                + "giorni passati vengono cancellati da soli.\n\n"
                + "Il 29 febbraio compare solo negli anni bisestili.");
        root.addView(help);

        ScrollView sv = new ScrollView(this);
        sv.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(sv);

        refresh();
    }

    private void choose(boolean nero) {
        new State(this).setNero(nero);
        ClockWidget.updateAll(this);   // i widget gia' sulla Home cambiano subito
        refresh();
    }

    private void refresh() {
        boolean nero = new State(this).nero();
        status.setText(nero ? "Colore attuale: nero" : "Colore attuale: bianco");
        // anteprima: sfondo scuro per il testo bianco, chiaro per il testo nero
        previewBox.setBackgroundColor(nero ? Color.parseColor("#DCE9F7") : Color.parseColor("#0B3A6E"));
        previewText.setTextColor(nero ? Color.BLACK : Color.WHITE);
    }
}
