package exiledsector.ui.node;

import java.awt.Color;
import java.util.Map;

final class RootColours {

    static final Color LOW_TECH = new Color(255, 125, 62);
    static final Color MIDLINE = new Color(255, 212, 112);
    static final Color HIGH_TECH = new Color(89, 157, 255);
    private static final Map<String, Color> BY_ROOT_TYPE = Map.of(
            "root_low_tech", LOW_TECH,
            "root_midline", MIDLINE,
            "root_high_tech", HIGH_TECH);

    private RootColours() {
    }

    static Color of(String rootTypeId) {
        return BY_ROOT_TYPE.getOrDefault(rootTypeId, Color.WHITE);
    }
}
