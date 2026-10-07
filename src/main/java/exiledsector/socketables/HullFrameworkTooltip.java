package exiledsector.socketables;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.function.Supplier;

public final class HullFrameworkTooltip {

    private static final float PAD = 10f;
    private static final float LINE_PAD = 3f;

    private HullFrameworkTooltip() {
    }

    public static void write(TooltipMakerAPI tooltip, HullFramework framework, Supplier<List<StyledText>> footer) {
        I18n.forGameText(() -> {
            tooltip.setTitleOrbitronVeryLarge();
            tooltip.setParaInsigniaVeryLarge();
            if (framework == null) {
                tooltip.addTitle(Translation.text("socketable.unknown"));
            } else {
                tooltip.addTitle(framework.name(), framework.rarity().color());
                VanillaText.addPara(tooltip, Translation.msg("framework.tooltip.fits")
                        .arg("size", Translation.text("hullSize." + framework.hullSize().name())).styled(), PAD, Misc.getGrayColor());
                tooltip.addSectionHeading(Translation.text("framework.tooltip.sockets"), Alignment.MID, PAD);
                float linePad = PAD;
                for (SocketType socketType : framework.socketTypes()) {
                    VanillaText.addPara(tooltip, socketLine(socketType), linePad, Misc.getTextColor());
                    linePad = LINE_PAD;
                }
            }
            float linePad = PAD;
            for (StyledText line : footer.get()) {
                VanillaText.addPara(tooltip, line, linePad, Misc.getGrayColor());
                linePad = LINE_PAD;
            }
            tooltip.setParaFontDefault();
        });
    }

    public static StyledText socketLine(SocketType socketType) {
        if (socketType.requirementTag() == null) {
            return StyledText.of(socketType.displayName());
        }
        return Translation.msg("framework.tooltip.socketNeeds").arg("type", socketType.displayName())
                .arg("requirement", requirementText(socketType.requirementTag())).styled();
    }

    public static String requirementText(String requirementTag) {
        return Translation.text("framework.requirement." + requirementTag);
    }
}
