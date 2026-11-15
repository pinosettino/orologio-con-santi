package it.orologiosanti;

/** Widget compatto 4x2. */
public class Widget4x2 extends ClockWidget {
    @Override int layoutRes(boolean nero) { return nero ? R.layout.widget_4x2_nero : R.layout.widget_4x2; }
    @Override int defaultWidthDp() { return 250; }
    @Override int noteSp() { return 16; }
    @Override int saintMaxSp() { return 15; }
}
