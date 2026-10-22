package exiledsector.items;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.ShipHullSizes;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.function.Supplier;

public final class HullUpgradeTooltip {

    private HullUpgradeTooltip() {
    }

    public static void write(TooltipMakerAPI tooltip, HullUpgradeData upgrade, Supplier<List<StyledText>> footer) {
        ItemTooltip.writeFrame(tooltip, upgrade == null ? null : () -> {
            tooltip.addTitle(upgrade.name(), ItemRarity.UNIQUE.color());
            VanillaText.addPara(tooltip, Translation.msg("upgrade.tooltip.body")
                    .arg("size", ShipHullSizes.displayName(upgrade.hullSize())).styled(), ItemTooltip.PAD, Misc.getTextColor());
            if (!HullUpgradesSetting.enabled()) {
                VanillaText.addPara(tooltip, Translation.msg("upgrade.tooltip.disabled").styled(), ItemTooltip.PAD, Misc.getNegativeHighlightColor());
            }
        }, footer);
    }

    public static StyledText sectionLine(ShipSection section) {
        if (section.requirementTag() == null) {
            return StyledText.of(section.displayName());
        }
        return Translation.msg("framework.socketNeeds").arg("type", section.displayName())
                .arg("requirement", requirementText(section.requirementTag())).styled();
    }

    public static String requirementText(String requirementTag) {
        return Translation.text("framework.requirement." + requirementTag);
    }
}
