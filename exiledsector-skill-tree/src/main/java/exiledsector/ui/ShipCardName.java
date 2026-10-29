package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;
import java.util.function.ToDoubleFunction;

final class ShipCardName {

    static final String FONT = Fonts.ORBITRON_12;
    static final Color COLOR = new Color(255, 255, 255, 100);
    static final float TOP_INSET = 4f;
    static final float SIDE_PADDING = 6f;
    static final String ELLIPSIS = "...";

    private ShipCardName() {
    }

    static void addTo(TooltipMakerAPI card, FleetMemberAPI member, float cardWidth) {
        String name = member.getShipName();
        if (name == null || name.isBlank()) {
            return;
        }
        String shownName = fit(name.strip(), cardWidth - SIDE_PADDING * 2f,
                text -> Global.getSettings().computeStringWidth(text, FONT));
        card.setParaFont(FONT);
        LabelAPI label = VanillaText.plainPara(card, shownName, 0f, COLOR);
        card.setParaFontDefault();
        label.setAlignment(Alignment.MID);
        label.autoSizeToWidth(cardWidth).inTL(0f, TOP_INSET);
    }

    static String fit(String name, float maxWidth, ToDoubleFunction<String> widthOf) {
        if (widthOf.applyAsDouble(name) <= maxWidth) {
            return name;
        }
        int end = name.length();
        while (end > 0) {
            end = name.offsetByCodePoints(end, -1);
            String candidate = name.substring(0, end).stripTrailing() + ELLIPSIS;
            if (widthOf.applyAsDouble(candidate) <= maxWidth) {
                return candidate;
            }
        }
        return ELLIPSIS;
    }
}
