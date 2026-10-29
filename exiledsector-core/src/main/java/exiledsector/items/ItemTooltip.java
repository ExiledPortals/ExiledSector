package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;

public final class ItemTooltip {

    public static final float WIDTH = 700f;
    public static final float PAD = 10f;
    public static final float SPLIT_WIDTH = 1100f;
    static final float SPLIT_GAP = 20f;
    private static final float LINE_PAD = 3f;

    private ItemTooltip() {
    }

    public static void write(TooltipMakerAPI tooltip, Item item, Supplier<List<StyledText>> footer, boolean showRollRanges) {
        write(tooltip, item, null, footer, showRollRanges);
    }

    public static void write(TooltipMakerAPI tooltip, Item item, HullSize hullSize, Supplier<List<StyledText>> footer,
                             boolean showRollRanges) {
        writeFrame(tooltip, item == null ? null : () -> writeBody(tooltip, item, hullSize, showRollRanges, true), footer);
    }

    public static boolean splits(Item item) {
        return item != null && !item.explicits().isEmpty();
    }

    public static void writeSplit(TooltipMakerAPI tooltip, float contentWidth, Item item, HullSize hullSize, Supplier<List<StyledText>> footer,
                                  boolean showRollRanges) {
        float columnWidth = (contentWidth - SPLIT_GAP) / 2f;
        CustomPanelAPI row = Global.getSettings().createCustom(contentWidth, 0f, null);
        TooltipMakerAPI details = row.createUIElement(columnWidth, 0f, false);
        writeFrame(details, () -> writeBody(details, item, hullSize, showRollRanges, false), footer);
        TooltipMakerAPI modifiers = row.createUIElement(columnWidth, 0f, false);
        I18n.forGameText(() -> {
            modifiers.setParaInsigniaVeryLarge();
            modifiers.addSectionHeading(Translation.text("socketable.tooltip.primaryData"), Alignment.MID, 0f);
            addLines(modifiers, item.effectLines(showRollRanges, hullSize), Misc.getTextColor());
            modifiers.setParaFontDefault();
        });
        float rowHeight = Math.max(details.getHeightSoFar(), modifiers.getHeightSoFar());
        details.getPosition().setSize(columnWidth, details.getHeightSoFar());
        modifiers.getPosition().setSize(columnWidth, modifiers.getHeightSoFar());
        row.addUIElement(details).inTL(0f, 0f);
        row.addUIElement(modifiers).inTL(columnWidth + SPLIT_GAP, 0f);
        row.getPosition().setSize(contentWidth, rowHeight);
        tooltip.addCustom(row, 0f);
    }

    private static void writeBody(TooltipMakerAPI tooltip, Item item, HullSize hullSize, boolean showRollRanges, boolean withEffects) {
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
        List<StyledText> effectLines = withEffects ? item.effectLines(showRollRanges, hullSize) : List.of();
        if (!effectLines.isEmpty()) {
            tooltip.addSectionHeading(Translation.text("socketable.tooltip.primaryData"), Alignment.MID, PAD);
            addLines(tooltip, effectLines, Misc.getTextColor());
        }
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
