package exiledsector.items;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.function.Supplier;

public final class HullUpgradeTooltip {

    private HullUpgradeTooltip() {
    }

    public static void write(TooltipMakerAPI tooltip, RefitSchematic schematic, Supplier<List<StyledText>> footer) {
        ItemTooltip.writeFrame(tooltip, schematic == null ? null : () -> {
            tooltip.addTitle(schematic.displayName(), schematic.color());
            VanillaText.addPara(tooltip, Translation.msg("upgrade.tooltip.body").count(schematic.requiredPoints())
                    .arg("points", schematic.points()).styled(), ItemTooltip.PAD, Misc.getTextColor());
            if (!HullUpgradesSetting.enabled()) {
                VanillaText.addPara(tooltip, Translation.msg("upgrade.tooltip.disabled").styled(), ItemTooltip.PAD, Misc.getNegativeHighlightColor());
            } else if (!HullUpgradePoints.schematicsInUse()) {
                VanillaText.addPara(tooltip, Translation.styled("upgrade.tooltip.chartMode"), ItemTooltip.PAD, Misc.getNegativeHighlightColor());
            }
        }, footer);
    }

    public static void writeMasterSchematic(TooltipMakerAPI tooltip, Supplier<List<StyledText>> footer) {
        ItemTooltip.writeFrame(tooltip, () -> {
            tooltip.addTitle(MasterSchematic.INSTANCE.displayName(), MasterSchematic.COLOR);
            VanillaText.addPara(tooltip, Translation.styled("masterSchematic.tooltip.body"), ItemTooltip.PAD, Misc.getTextColor());
            VanillaText.addPara(tooltip, Translation.styled("masterSchematic.tooltip.flavour"), ItemTooltip.PAD, Misc.getGrayColor());
            if (!MasterSchematic.required()) {
                VanillaText.addPara(tooltip, Translation.styled("masterSchematic.tooltip.notRequired"), ItemTooltip.PAD, Misc.getNegativeHighlightColor());
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
