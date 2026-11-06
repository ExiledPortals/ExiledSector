package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.ui.TooltipTextSize;
import exiledsector.ui.VanillaText;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;

public final class ItemTooltip {

    private static final float VANILLA_WIDTH = 450f;
    private static final float LARGE_TEXT_WIDTH = 700f;
    public static final float PAD = 10f;
    private static final float LINE_PAD = 3f;
    private static final float SLOT_TAG_WIDTH = 30f;
    private static final float SLOT_TAG_GAP = 6f;

    private ItemTooltip() {
    }

    public static float width() {
        return TooltipTextSize.large() ? LARGE_TEXT_WIDTH : VANILLA_WIDTH;
    }

    public static void write(TooltipMakerAPI tooltip, Item item, Supplier<List<StyledText>> footer, boolean showRollRanges) {
        write(tooltip, item, null, footer, showRollRanges);
    }

    public static void write(TooltipMakerAPI tooltip, Item item, HullSize hullSize, Supplier<List<StyledText>> footer,
                             boolean showRollRanges) {
        writeFrame(tooltip, item == null ? null : () -> writeBody(tooltip, item, hullSize, showRollRanges), footer);
    }

    private static void writeBody(TooltipMakerAPI tooltip, Item item, HullSize hullSize, boolean showRollRanges) {
        ItemName itemName = item.displayName();
        tooltip.addTitle(itemName.title(), itemName.rarity().color());
        if (itemName.baseName() != null) {
            VanillaText.plainPara(tooltip, itemName.baseName(), 0f, Misc.getTextColor());
        }
        StyledText limitLine = item.limitLine();
        if (limitLine != null) {
            VanillaText.addPara(tooltip, limitLine, LINE_PAD, Misc.getTextColor());
        }
        StyledText flavour = item.flavourLine();
        if (flavour != null) {
            addLines(tooltip, List.of(flavour), Misc.getGrayColor());
        }
        addLines(tooltip, item.detailLines(), Misc.getTextColor());
        item.writeTooltipSections(tooltip, hullSize, showRollRanges);
        List<StyledText> implicitLines = item.implicitLines(showRollRanges);
        if (!implicitLines.isEmpty()) {
            tooltip.addSectionHeading(Translation.text("item.tooltip.implicit"), Alignment.MID, PAD);
            addLines(tooltip, implicitLines, Misc.getTextColor());
        }
        List<SlottedLine> effectRows = item.slottedEffectRows(showRollRanges, hullSize);
        if (effectRows.isEmpty()) {
            return;
        }
        tooltip.addSectionHeading(Translation.text("item.tooltip.explicit"), Alignment.MID, PAD);
        if (showRollRanges) {
            SlottedRows.Layout layout = new SlottedRows.Layout(tooltip.getWidthSoFar(), SLOT_TAG_WIDTH, SLOT_TAG_GAP, TooltipTextSize::applyBodyFont);
            SlottedRows.add(tooltip, effectRows, layout, PAD, LINE_PAD);
        } else {
            addLines(tooltip, effectRows.stream().map(SlottedLine::text).toList(), Misc.getTextColor());
        }
    }

    public static void writeFrame(TooltipMakerAPI tooltip, Runnable knownItemBody, Supplier<List<StyledText>> footer) {
        I18n.forGameText(() -> {
            TooltipTextSize.applyTitleFont(tooltip);
            TooltipTextSize.applyBodyFont(tooltip);
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
