package it.orologiosanti;

/** Widget alto 5x3 (griglia della Home a 5 colonne). */
public class Widget5x3 extends ClockWidget {
    @Override int layoutRes(boolean nero) { return nero ? R.layout.widget_5x3_nero : R.layout.widget_5x3; }
    @Override int defaultWidthDp() { return 320; }
    @Override int noteSp() { return 22; }
    @Override int saintMaxSp() { return 20; }
}
