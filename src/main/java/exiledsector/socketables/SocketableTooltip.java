package exiledsector.socketables;

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

public final class SocketableTooltip {

    public static final float WIDTH = 700f;
    static final float PAD = 10f;
    private static final float LINE_PAD = 3f;

    private SocketableTooltip() {
    }

    public static void write(TooltipMakerAPI tooltip, Socketable socketable, Supplier<List<StyledText>> footer, boolean showRollRanges) {
        write(tooltip, socketable, null, footer, showRollRanges);
    }

    public static void write(TooltipMakerAPI tooltip, Socketable socketable, HullSize hullSize, Supplier<List<StyledText>> footer,
                             boolean showRollRanges) {
        writeFrame(tooltip, socketable == null ? null : () -> {
            SocketableName socketableName = socketable.displayName();
            tooltip.addTitle(socketableName.title(), socketableName.rarity().color());
            if (socketableName.baseName() != null) {
                VanillaText.plainPara(tooltip, socketableName.baseName(), 0f, Misc.getGrayColor());
            }
            addLines(tooltip, socketable.headerLines(), Misc.getGrayColor());
            List<StyledText> implicitLines = socketable.implicitLines(showRollRanges);
            if (!implicitLines.isEmpty()) {
                tooltip.addSectionHeading(Translation.text("hullItem.tooltip.implicit"), Alignment.MID, PAD);
                addLines(tooltip, implicitLines, Misc.getTextColor());
            }
            List<StyledText> effectLines = socketable.effectLines(showRollRanges, hullSize);
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
