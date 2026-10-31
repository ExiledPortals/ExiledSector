package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.charts.wormhole.ColonyFreeAtlas;
import exiledsector.i18n.Translation;
import exiledsector.ui.CampaignMessages;
import exiledsector.ui.VanillaText;

public class WormholeTechItemPlugin extends BaseSpecialItemPlugin {

    private static final float TOOLTIP_PAD = 10f;

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        tooltip.addTitle(getName());
        String description = spec == null ? null : spec.getDesc();
        if (description != null && !description.isEmpty()) {
            VanillaText.plainPara(tooltip, description, TOOLTIP_PAD, Misc.getGrayColor());
        }
        VanillaText.plainPara(tooltip, Translation.text(usageKey(itemId, WormholeGateway.researchGated(), ColonyFreeAtlas.active())), TOOLTIP_PAD,
                Misc.getHighlightColor());
        addCostLabel(tooltip, TOOLTIP_PAD, transferHandler, stackSource);
    }

    static String usageKey(String itemId, boolean researchMod, boolean colonyFree) {
        return colonyFree && WormholeTechItems.BLUEPRINT.equals(itemId) ? "gatewayItem.tooltip.blueprint.atlas" : usageKey(itemId, researchMod);
    }

    static String usageKey(String itemId, boolean researchMod) {
        if (WormholeTechItems.BLUEPRINT.equals(itemId)) {
            return researchMod ? "gatewayItem.tooltip.blueprint.research" : "gatewayItem.tooltip.blueprint.rightClick";
        }
        if (!researchMod) {
            return "gatewayItem.tooltip.upgrade.noResearch";
        }
        return WormholeTechItems.UPGRADE_PERFECTED.equals(itemId) ? "gatewayItem.tooltip.upgrade.perfected"
                : "gatewayItem.tooltip.upgrade.advanced";
    }

    @Override
    public boolean hasRightClickAction() {
        return WormholeTechItems.BLUEPRINT.equals(itemId) && !WormholeGateway.researchGated();
    }

    @Override
    public boolean shouldRemoveOnRightClickAction() {
        return hasRightClickAction();
    }

    @Override
    public void performRightClickAction() {
        if (!hasRightClickAction()) {
            return;
        }
        WormholeGateway.unlock(Global.getSector());
        ColonyFreeAtlas.syncAbility(Global.getSector());
        CampaignMessages.plain(Translation.gameText(ColonyFreeAtlas.active() ? "gatewayItem.unlocked.atlas" : "gatewayItem.unlocked"),
                Misc.getPositiveHighlightColor());
    }
}
