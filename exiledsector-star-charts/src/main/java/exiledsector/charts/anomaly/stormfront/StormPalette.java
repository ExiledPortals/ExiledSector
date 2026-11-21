package exiledsector.charts.anomaly.stormfront;

import java.awt.Color;

record StormPalette(Color cloud, Color flash, Color arcFringe, Color arcCore) {

    static final StormPalette ION = new StormPalette(new Color(90, 110, 255), new Color(170, 200, 255), new Color(110, 130, 255, 255),
            new Color(235, 240, 255, 255));
    static final StormPalette SOLAR = new StormPalette(new Color(255, 100, 40), new Color(255, 190, 110), new Color(255, 120, 50, 255),
            new Color(255, 240, 220, 255));

    static StormPalette of(boolean solar) {
        return solar ? SOLAR : ION;
    }
}
