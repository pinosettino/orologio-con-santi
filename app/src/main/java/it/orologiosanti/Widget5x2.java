package it.orologiosanti;

/** Widget largo 5x2. */
public class Widget5x2 extends ClockWidget {
    @Override int layoutRes(boolean nero) { return nero ? R.layout.widget_5x2_nero : R.layout.widget_5x2; }
    @Override int defaultWidthDp() { return 320; }
    @Override int noteSp() { return 18; }
    @Override int saintMaxSp() { return 17; }
    @Override int arrowDp() { return 28; }
}
