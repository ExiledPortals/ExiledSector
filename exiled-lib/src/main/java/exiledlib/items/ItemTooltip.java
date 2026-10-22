package exiledlib.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.I18n;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.ui.VanillaText;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;

public final class ItemTooltip {

    public static final float WIDTH = 700f;
    public static final float PAD = 10f;
    private static final float LINE_PAD = 3f;

    private ItemTooltip() {
    }

    public static void write(TooltipMakerAPI tooltip, Item item, Supplier<List<StyledText>> footer, boolean showRollRanges) {
        write(tooltip, item, null, footer, showRollRanges);
    }

    public static void write(TooltipMakerAPI tooltip, Item item, HullSize hullSize, Supplier<List<StyledText>> footer,
                             boolean showRollRanges) {
        writeFrame(tooltip, item == null ? null : () -> {
            ItemName itemName = item.displayName();
            tooltip.addTitle(itemName.title(), itemName.rarity().color());
            if (itemName.baseName() != null) {
                VanillaText.plainPara(tooltip, itemName.baseName(), 0f, Misc.getGrayColor());
            }
            StyledText limitLine = item.limitLine();
            if (limitLine != null) {
                VanillaText.addPara(tooltip, limitLine, LINE_PAD, Misc.getGrayColor());
            }
            addLines(tooltip, item.headerLines(), Misc.getGrayColor());
            item.writeTooltipSections(tooltip, hullSize, showRollRanges);
            List<StyledText> implicitLines = item.implicitLines(showRollRanges);
            if (!implicitLines.isEmpty()) {
                tooltip.addSectionHeading(Translation.text("hullItem.tooltip.implicit"), Alignment.MID, PAD);
                addLines(tooltip, implicitLines, Misc.getTextColor());
            }
            List<StyledText> effectLines = item.effectLines(showRollRanges, hullSize);
            if (!effectLines.isEmpty()) {
                tooltip.addSectionHeading(Translation.text("socketable.tooltip.primaryData"), Alignment.MID, PAD);
                addLines(tooltip, effectLines, Misc.getTextColor());
            }
        }, footer);
    }

    public static void writeFrame(TooltipMakerAPI tooltip, Runnable knownItemBody, Supplier<List<StyledText>> footer) {
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

    public static void addSection(TooltipMakerAPI tooltip, String heading, List<StyledText> lines) {
        tooltip.addSectionHeading(heading, Alignment.MID, PAD);
        addLines(tooltip, lines, Misc.getTextColor());
    }

    public static void addCentredLines(TooltipMakerAPI tooltip, List<StyledText> lines) {
        float linePad = PAD;
        for (StyledText line : lines) {
            VanillaText.addPara(tooltip, line, linePad, Misc.getTextColor()).setAlignment(Alignment.MID);
            linePad = LINE_PAD;
        }
    }

    private static void addLines(TooltipMakerAPI tooltip, List<StyledText> lines, Color color) {
        float linePad = PAD;
        for (StyledText line : lines) {
            VanillaText.addPara(tooltip, line, linePad, color);
            linePad = LINE_PAD;
        }
    }
}
