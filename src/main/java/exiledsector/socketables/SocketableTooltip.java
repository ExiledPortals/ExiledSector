package exiledsector.socketables;

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

    public static void write(TooltipMakerAPI tooltip, Socketable socketable, Supplier<List<StyledText>> footer) {
        I18n.forGameText(() -> {
            tooltip.setTitleOrbitronVeryLarge();
            tooltip.setParaInsigniaVeryLarge();
            if (socketable == null) {
                tooltip.addTitle(Translation.text("socketable.unknown"));
            } else {
                SocketableName name = socketable.displayName();
                tooltip.addTitle(name.title(), name.rarity().color());
                if (name.baseName() != null) {
                    tooltip.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), name.baseName());
                }
                addLines(tooltip, socketable.headerLines(), Misc.getGrayColor());
                List<StyledText> effects = socketable.effectLines();
                if (!effects.isEmpty()) {
                    tooltip.addSectionHeading(Translation.text("socketable.tooltip.primaryData"), Alignment.MID, PAD);
                    addLines(tooltip, effects, Misc.getTextColor());
                }
            }
            float pad = PAD;
            for (StyledText line : footer.get()) {
                VanillaText.addPara(tooltip, line, pad, Misc.getGrayColor());
                pad = LINE_PAD;
            }
            tooltip.setParaFontDefault();
        });
    }

    private static void addLines(TooltipMakerAPI tooltip, List<StyledText> lines, Color color) {
        float pad = PAD;
        for (StyledText line : lines) {
            VanillaText.addPara(tooltip, line, pad, color);
            pad = LINE_PAD;
        }
    }
}
