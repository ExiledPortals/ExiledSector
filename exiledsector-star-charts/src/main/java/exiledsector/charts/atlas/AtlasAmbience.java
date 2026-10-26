package exiledsector.charts.atlas;

import java.awt.Color;
import java.util.Map;

public record AtlasAmbience(float storms, Color cloudColour, Map<String, Float> decorations, float decorationDensity, boolean abyss) {

    public static final AtlasAmbience BARE = new AtlasAmbience(0f, null, Map.of(), 0f);

    public AtlasAmbience {
        storms = Math.max(0f, Math.min(1f, storms));
        decorations = Map.copyOf(decorations);
        decorationDensity = Math.max(0f, decorationDensity);
    }

    public AtlasAmbience(float storms, Color cloudColour, Map<String, Float> decorations, float decorationDensity) {
        this(storms, cloudColour, decorations, decorationDensity, false);
    }
}
