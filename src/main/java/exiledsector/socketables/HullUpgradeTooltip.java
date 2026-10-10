package exiledsector.socketables;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.function.Supplier;

public final class HullUpgradeTooltip {

    private static final float PAD = 10f;
    private static final float LINE_PAD = 3f;

    private HullUpgradeTooltip() {
    }

    public static void write(TooltipMakerAPI tooltip, HullUpgradeData upgrade, Supplier<List<StyledText>> footer) {
        I18n.forGameText(() -> {
            tooltip.setTitleOrbitronVeryLarge();
            tooltip.setParaInsigniaVeryLarge();
            if (upgrade == null) {
                tooltip.addTitle(Translation.text("socketable.unknown"));
            } else {
                tooltip.addTitle(upgrade.name(), SocketableRarity.UNIQUE.color());
                VanillaText.addPara(tooltip, Translation.msg("upgrade.tooltip.body")
                        .arg("size", Translation.text("hullSize." + upgrade.hullSize().name()))
                        .arg("max", FrameworkSockets.MAX_POINTS).styled(), PAD, Misc.getTextColor());
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
        return Translation.msg("framework.socketNeeds").arg("type", socketType.displayName())
                .arg("requirement", requirementText(socketType.requirementTag())).styled();
    }

    public static String requirementText(String requirementTag) {
        return Translation.text("framework.requirement." + requirementTag);
    }
}
