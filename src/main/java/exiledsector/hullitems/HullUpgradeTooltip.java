package exiledsector.hullitems;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.ShipHullSizes;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.function.Supplier;

public final class HullUpgradeTooltip {

    private HullUpgradeTooltip() {
    }

    public static void write(TooltipMakerAPI tooltip, HullUpgradeData upgrade, Supplier<List<StyledText>> footer) {
        HullItemTooltip.writeFrame(tooltip, upgrade == null ? null : () -> {
            tooltip.addTitle(upgrade.name(), HullItemRarity.UNIQUE.color());
            VanillaText.addPara(tooltip, Translation.msg("upgrade.tooltip.body")
                    .arg("size", ShipHullSizes.displayName(upgrade.hullSize())).styled(), HullItemTooltip.PAD, Misc.getTextColor());
            if (!HullUpgradesSetting.enabled()) {
                VanillaText.addPara(tooltip, Translation.msg("upgrade.tooltip.disabled").styled(), HullItemTooltip.PAD, Misc.getNegativeHighlightColor());
            }
        }, footer);
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
