package exiledsector.hullitems;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;

public final class HullItemTooltip {

    public static final float WIDTH = 700f;
    static final float PAD = 10f;
    private static final float LINE_PAD = 3f;

    private HullItemTooltip() {
    }

    public static void write(TooltipMakerAPI tooltip, HullItem hullItem, Supplier<List<StyledText>> footer, boolean showRollRanges) {
        write(tooltip, hullItem, null, footer, showRollRanges);
    }

    public static void write(TooltipMakerAPI tooltip, HullItem hullItem, HullSize hullSize, Supplier<List<StyledText>> footer,
                             boolean showRollRanges) {
        writeFrame(tooltip, hullItem == null ? null : () -> {
            HullItemName hullItemName = hullItem.displayName();
            tooltip.addTitle(hullItemName.title(), hullItemName.rarity().color());
            if (hullItemName.baseName() != null) {
                VanillaText.plainPara(tooltip, hullItemName.baseName(), 0f, Misc.getGrayColor());
            }
            addLines(tooltip, hullItem.headerLines(), Misc.getGrayColor());
            List<StyledText> implicitLines = hullItem.implicitLines(showRollRanges);
            if (!implicitLines.isEmpty()) {
                tooltip.addSectionHeading(Translation.text("hullItem.tooltip.implicit"), Alignment.MID, PAD);
                addLines(tooltip, implicitLines, Misc.getTextColor());
            }
            List<StyledText> effectLines = hullItem.effectLines(showRollRanges, hullSize);
            if (!effectLines.isEmpty()) {
                tooltip.addSectionHeading(Translation.text("socketable.tooltip.primaryData"), Alignment.MID, PAD);
                addLines(tooltip, effectLines, Misc.getTextColor());
            }
        }, footer);
    }

    static void writeFrame(TooltipMakerAPI tooltip, Runnable knownItemBody, Supplier<List<StyledText>> footer) {
        I18n.forGameText(() -> {
            tooltip.setTitleOrbitronVeryLarge();
            tooltip.setParaInsigniaVeryLarge();
            if (knownItemBody == null) {
                tooltip.addTitle(Translation.text("socketable.unknown"));
            } else {
                knownItemBody.run();
            }
            addLines(tooltip, footer.get(), Misc.getGrayColor());
            tooltip.setParaFontDefault();
        });
    }

    private static void addLines(TooltipMakerAPI tooltip, List<StyledText> lines, Color color) {
        float linePad = PAD;
        for (StyledText line : lines) {
            VanillaText.addPara(tooltip, line, linePad, color);
            linePad = LINE_PAD;
        }
    }
}
