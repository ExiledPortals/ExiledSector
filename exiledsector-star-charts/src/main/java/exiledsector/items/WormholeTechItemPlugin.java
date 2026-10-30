package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.compat.VaultsOfKnowledgeCompat;
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
        VanillaText.plainPara(tooltip, Translation.text(usageKey(itemId, VaultsOfKnowledgeCompat.isModEnabled())), TOOLTIP_PAD,
                Misc.getHighlightColor());
        addCostLabel(tooltip, TOOLTIP_PAD, transferHandler, stackSource);
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
        return WormholeTechItems.BLUEPRINT.equals(itemId) && !VaultsOfKnowledgeCompat.isModEnabled();
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
        CampaignMessages.plain(Translation.gameText("gatewayItem.unlocked"), Misc.getPositiveHighlightColor());
    }
}
