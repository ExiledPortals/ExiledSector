package exiledsector.charts.atlas;

import java.awt.Color;
import java.util.List;
import java.util.Map;

public record AtlasRegionLook(List<String> backgrounds, List<String> nebulaBackgrounds, float nebulaChance, Color lightTint,
                              float lightTintStrength, Map<String, Float> starWeights, Map<String, Float> planetWeights, boolean keepRuins,
                              String musicSetId) {

    public static final AtlasRegionLook PLAIN = new AtlasRegionLook(List.of(), List.of(), Float.NaN, null, 0f, Map.of(), Map.of(), false, "");

    public AtlasRegionLook {
        backgrounds = List.copyOf(backgrounds);
        nebulaBackgrounds = List.copyOf(nebulaBackgrounds);
        starWeights = Map.copyOf(starWeights);
        planetWeights = Map.copyOf(planetWeights);
        musicSetId = musicSetId == null ? "" : musicSetId;
    }
}
