package it.orologiosanti;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Un solo appunto (una riga di testo) per giorno. */
final class NotesDb extends SQLiteOpenHelper {
    private static NotesDb instance;

    static synchronized NotesDb get(Context ctx) {
        if (instance == null) {
            instance = new NotesDb(ctx.getApplicationContext());
        }
        return instance;
    }

    private NotesDb(Context ctx) {
        super(ctx, "appunti.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE note (giorno TEXT PRIMARY KEY, testo TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // niente da migrare per ora
    }

    synchronized String getNote(String key) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT testo FROM note WHERE giorno = ?", new String[]{key});
        try {
            return c.moveToFirst() ? c.getString(0) : "";
        } finally {
            c.close();
        }
    }

    /** Testo vuoto = appunto eliminato. */
    synchronized void setNote(String key, String text) {
        SQLiteDatabase db = getWritableDatabase();
        if (text == null || text.trim().length() == 0) {
            db.delete("note", "giorno = ?", new String[]{key});
        } else {
            ContentValues v = new ContentValues();
            v.put("giorno", key);
            v.put("testo", text.trim());
            db.insertWithOnConflict("note", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    /** Cancella tutti gli appunti con data precedente a quella indicata (di solito oggi). */
    synchronized void deleteBefore(String key) {
        getWritableDatabase().delete("note", "giorno < ?", new String[]{key});
    }
}
