package it.orologiosanti;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

/** Schermata informativa: l'app serve solo da contenitore dei widget. */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);

        TextView t = new TextView(this);
        t.setPadding(pad, pad, pad, pad);
        t.setTextSize(16);
        t.setText("Orologio Santi\n\n"
                + "Questa e' solo l'app che contiene i widget: non devi usarla.\n\n"
                + "Per aggiungere il widget:\n"
                + "1. tieni premuto su uno spazio vuoto della schermata Home;\n"
                + "2. scegli \"Widget\";\n"
                + "3. cerca \"Orologio Santi\" e trascina \"Orologio 4x2\" o \"Orologio 5x2\".\n\n"
                + "Uso:\n"
                + "- le frecce accanto alla data cambiano giorno;\n"
                + "- toccando la data si torna a oggi;\n"
                + "- toccando l'orologio il testo passa da bianco a nero (e viceversa);\n"
                + "- toccando la riga sotto il santo scrivi l'appunto del giorno;\n"
                + "- se l'appunto e' lungo compaiono due frecce per leggerlo a pezzi;\n"
                + "- gli appunti dei giorni passati vengono cancellati da soli;\n"
                + "- dopo 3 minuti il widget torna da solo alla data di oggi.");

        ScrollView sv = new ScrollView(this);
        sv.addView(t, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(sv);
    }
}
