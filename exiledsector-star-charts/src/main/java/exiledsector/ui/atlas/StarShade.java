package exiledsector.ui.atlas;

import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;
import exiledsector.ui.util.ColorUtil;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

final class StarShade {

    static final float AVAILABLE_DARKENING = 0.2f;
    static final float DULL_DARKENING = 0.45f;

    private final Map<Color, Color[]> shadesByLitColour = new HashMap<>();

    Color of(Color litColour, Lighting lighting) {
        if (lighting == Lighting.LIT || litColour == null) {
            return litColour;
        }
        Color[] shades = shadesByLitColour.computeIfAbsent(litColour, StarShade::shadesOf);
        return lighting == Lighting.AVAILABLE ? shades[0] : shades[1];
    }

    static Color[] shadesOf(Color litColour) {
        return new Color[] {ColorUtil.mix(litColour, Color.BLACK, AVAILABLE_DARKENING), ColorUtil.mix(litColour, Color.BLACK, DULL_DARKENING)};
    }
}
