package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseAbilityPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.wormhole.ColonyFreeAtlas;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

public class AtlasAbility extends BaseAbilityPlugin {

    private static final float TOOLTIP_PAD = 10f;

    @Override
    public boolean isUsable() {
        CampaignFleetAPI fleet = getFleet();
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        return super.isUsable() && ColonyFreeAtlas.active() && fleet != null && fleet.isPlayerFleet() && campaignUI != null
                && !campaignUI.isShowingDialog();
    }

    @Override
    public void pressButton() {
        if (isUsable()) {
            Global.getSector().getCampaignUI().showInteractionDialog(new AtlasAbilityDialog(), getFleet());
        }
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        tooltip.addTitle(Translation.gameText("chart.gateway.option.open"));
        VanillaText.plainPara(tooltip, Translation.gameText("chart.gateway.option.tooltip"), TOOLTIP_PAD, Misc.getTextColor());
    }

    @Override
    public boolean isTooltipExpandable() {
        return false;
    }

    @Override
    public void setCooldownLeft(float days) {
    }

    @Override
    public float getCooldownLeft() {
        return 0f;
    }
}
