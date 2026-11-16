package it.orologiosanti;

import java.util.ArrayList;
import java.util.List;

/** Divide un testo in "pagine" di una riga, andando a capo solo tra le parole quando possibile. */
final class TextPager {
    private TextPager() {}

    interface Meter {
        /** Larghezza del testo. */
        float width(String s);

        /** Quanti caratteri di s entrano in maxWidth. */
        int fit(String s, float maxWidth);
    }

    static List<String> pages(String text, Meter m, float maxWidth) {
        List<String> pages = new ArrayList<String>();
        String t = text == null ? "" : text.trim();
        if (t.length() == 0) {
            return pages;
        }
        String cur = "";
        for (String word : t.split("\\s+")) {
            // parola piu' larga di una riga: viene spezzata
            while (m.width(word) > maxWidth) {
                int n = m.fit(word, maxWidth);
                if (n <= 0) {
                    n = 1;
                }
                if (n >= word.length()) {
                    break;
                }
                if (cur.length() > 0) {
                    pages.add(cur);
                    cur = "";
                }
                pages.add(word.substring(0, n));
                word = word.substring(n);
            }
            if (word.length() == 0) {
                continue;
            }
            String trial = cur.length() == 0 ? word : cur + " " + word;
            if (m.width(trial) <= maxWidth) {
                cur = trial;
            } else {
                if (cur.length() > 0) {
                    pages.add(cur);
                }
                cur = word;
            }
        }
        if (cur.length() > 0) {
            pages.add(cur);
        }
        return pages;
    }

    /**
     * Trasforma l'appunto nella sequenza di pezzi da mostrare.
     * Il marcatore # separa i messaggi (accettato anche il vecchio |): ognuno viene mostrato da solo e, se e' piu' lungo
     * della riga, a pezzi; solo dopo l'ultimo pezzo passa al messaggio successivo.
     * Con un solo pezzo lo ripete, cosi' scorre comunque.
     */
    static List<String> build(String note, Meter m, float width, int maxPieces) {
        List<String> out = new ArrayList<String>();
        if (note == null) {
            return out;
        }
        for (String part : note.split("[#|]")) {
            String msg = part.trim().replaceAll("\\s+", " ");
            if (msg.length() == 0) {
                continue;
            }
            if (m.width(msg) <= width) {
                out.add(msg);
            } else {
                out.addAll(pages(msg, m, width));
            }
            if (out.size() >= maxPieces) {
                break;
            }
        }
        if (out.size() > maxPieces) {
            out = new ArrayList<String>(out.subList(0, maxPieces));
        }
        if (out.size() == 1) {
            out.add(out.get(0));
        }
        return out;
    }
}
