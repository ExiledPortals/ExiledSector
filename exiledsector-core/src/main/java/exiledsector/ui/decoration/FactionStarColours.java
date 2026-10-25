package exiledsector.ui.decoration;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class FactionStarColours {

    private static final AtomicReference<Map<String, Color>> COLOUR_BY_FACTION = new AtomicReference<>(Map.of());

    private FactionStarColours() {
    }

    public static void register(Map<String, Color> colourByFaction) {
        COLOUR_BY_FACTION.set(Map.copyOf(colourByFaction));
    }

    public static Color colourOf(String factionId) {
        return factionId == null ? null : COLOUR_BY_FACTION.get().get(factionId);
    }

    public static void clear() {
        COLOUR_BY_FACTION.set(Map.of());
    }
}
