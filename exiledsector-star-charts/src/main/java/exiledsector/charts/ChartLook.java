package exiledsector.charts;

import java.awt.Color;
import java.util.List;
import java.util.Map;

public record ChartLook(List<String> backgrounds, List<String> nebulaBackgrounds, float nebulaChance, Color lightTint, float lightTintStrength,
                        Map<String, Float> starWeights, Map<String, Float> planetWeights, boolean keepRuins, String musicSetId) {

    public static final ChartLook PLAIN = new ChartLook(List.of(), List.of(), Float.NaN, null, 0f, Map.of(), Map.of(), false, "");

    public ChartLook {
        backgrounds = List.copyOf(backgrounds);
        nebulaBackgrounds = List.copyOf(nebulaBackgrounds);
        lightTintStrength = Math.max(0f, Math.min(1f, lightTintStrength));
        starWeights = Map.copyOf(starWeights);
        planetWeights = Map.copyOf(planetWeights);
        musicSetId = musicSetId == null ? "" : musicSetId;
    }

    public Color tinted(Color light) {
        if (lightTint == null || light == null || lightTintStrength <= 0f) {
            return light;
        }
        return new Color(blend(light.getRed(), lightTint.getRed()), blend(light.getGreen(), lightTint.getGreen()),
                blend(light.getBlue(), lightTint.getBlue()), light.getAlpha());
    }

    private int blend(int from, int to) {
        return Math.round(from + (to - from) * lightTintStrength);
    }
}
