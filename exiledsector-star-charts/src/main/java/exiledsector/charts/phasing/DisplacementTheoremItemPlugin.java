package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.i18n.Translation;
import exiledsector.ui.CampaignMessages;
import exiledsector.ui.VanillaText;

public class DisplacementTheoremItemPlugin extends BaseSpecialItemPlugin {

    private static final float TOOLTIP_PAD = 10f;

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        tooltip.addTitle(getName());
        String description = spec == null ? null : spec.getDesc();
        if (description != null && !description.isEmpty()) {
            VanillaText.plainPara(tooltip, description, TOOLTIP_PAD, Misc.getGrayColor());
        }
        VanillaText.plainPara(tooltip, Translation.text(usageKey(WormholeGateway.researchGated(), PhaseGateRules.isUnlocked(Global.getSector()))),
                TOOLTIP_PAD, Misc.getHighlightColor());
        addCostLabel(tooltip, TOOLTIP_PAD, transferHandler, stackSource);
    }

    static String usageKey(boolean researchMod, boolean alreadyUnlocked) {
        if (researchMod) {
            return "phasing.tooltip.research";
        }
        return alreadyUnlocked ? "phasing.tooltip.unlocked" : "phasing.tooltip.rightClick";
    }

    @Override
    public boolean hasRightClickAction() {
        return !WormholeGateway.researchGated() && !PhaseGateRules.isUnlocked(Global.getSector());
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
        PhaseGateRules.unlock(Global.getSector());
        CampaignMessages.plain(Translation.gameText("phasing.unlocked"), Misc.getPositiveHighlightColor());
    }
}
