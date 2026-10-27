package exiledsector.charts.layout;

import exiledsector.charts.layout.abyss.AbyssPalette;

import java.awt.Color;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ChartLook(List<String> backgrounds, List<String> nebulaBackgrounds, float nebulaChance, Color lightTint, float lightTintStrength,
                        Map<String, Float> starWeights, Map<String, Float> planetWeights, boolean keepRuins, String musicSetId,
                        Map<AbyssPalette, Float> abyssPalettes) {

    public static final ChartLook PLAIN = new ChartLook(List.of(), List.of(), Float.NaN, null, 0f, Map.of(), Map.of(), false, "");

    public ChartLook {
        backgrounds = List.copyOf(backgrounds);
        nebulaBackgrounds = List.copyOf(nebulaBackgrounds);
        lightTintStrength = Math.max(0f, Math.min(1f, lightTintStrength));
        starWeights = Map.copyOf(starWeights);
        planetWeights = Map.copyOf(planetWeights);
        musicSetId = musicSetId == null ? "" : musicSetId;
        abyssPalettes = abyssPalettes == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(abyssPalettes));
    }

    public ChartLook(List<String> backgrounds, List<String> nebulaBackgrounds, float nebulaChance, Color lightTint, float lightTintStrength,
                     Map<String, Float> starWeights, Map<String, Float> planetWeights, boolean keepRuins, String musicSetId) {
        this(backgrounds, nebulaBackgrounds, nebulaChance, lightTint, lightTintStrength, starWeights, planetWeights, keepRuins, musicSetId, Map.of());
    }

    public ChartLook withAbyssPalette(AbyssPalette palette) {
        return new ChartLook(backgrounds, nebulaBackgrounds, nebulaChance, lightTint, lightTintStrength, starWeights, planetWeights, keepRuins,
                musicSetId, palette == null ? Map.of() : Map.of(palette, 1f));
    }

    public boolean abyssal() {
        return !abyssPalettes.isEmpty();
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
