package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.skills.LogisticsNodes;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.ui.VanillaText;

import java.util.List;

public class LogisticsSchematicItemPlugin extends BaseSpecialItemPlugin {

    @Override
    public String getName() {
        return I18n.forGameText(LogisticsSchematic.INSTANCE::displayName);
    }

    @Override
    public float getTooltipWidth() {
        return ItemTooltip.width();
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        ItemTooltip.writeFrame(tooltip, () -> {
            tooltip.addTitle(LogisticsSchematic.INSTANCE.displayName(), Misc.getStoryOptionColor());
            VanillaText.addPara(tooltip, Translation.msg("logisticsSchematic.tooltip.body").arg("cap", LogisticsNodes.DEFAULT_CAP).styled(),
                    ItemTooltip.PAD, Misc.getTextColor());
            VanillaText.addPara(tooltip, Translation.styled("logisticsSchematic.tooltip.flavour"), ItemTooltip.PAD, Misc.getGrayColor());
            if (!SkillTreeSetting.enabled()) {
                VanillaText.addPara(tooltip, Translation.styled("logisticsSchematic.tooltip.disabled"), ItemTooltip.PAD,
                        Misc.getNegativeHighlightColor());
            }
        }, List::of);
        addCostLabel(tooltip, ItemTooltip.PAD, transferHandler, stackSource);
    }
}
