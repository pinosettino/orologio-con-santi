package it.orologiosanti;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Riceve cambio data/ora/fuso orario e l'allarme di mezzanotte: aggiorna i widget. */
public class TimeReceiver extends BroadcastReceiver {
    static final String A_MIDNIGHT = "it.orologiosanti.MIDNIGHT";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        ClockWidget.updateAll(ctx);
        ClockWidget.scheduleMidnight(ctx);
    }
}
