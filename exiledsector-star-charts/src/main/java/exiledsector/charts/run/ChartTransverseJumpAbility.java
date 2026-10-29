package exiledsector.charts.run;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.abilities.EmergencyBurnAbility;
import com.fs.starfarer.api.impl.campaign.abilities.FractureJumpAbility;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.FleetMemberDamageLevel;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.ui.VanillaText;

public class ChartTransverseJumpAbility extends FractureJumpAbility {

    static final String EXTRA_CR_ONCE_KEY = "$makeTranverseJumpCostMoreCROnce";
    static final float TOOLTIP_PAD = 10f;

    @Override
    protected void applyEffect(float amount, float level) {
        CampaignFleetAPI fleet = getFleet();
        ChartRun run = fleet == null ? null : chartRunAround(fleet);
        if (level < 1f || primed == null || run == null) {
            super.applyEffect(amount, level);
            return;
        }
        if (ChartRunActions.returnToOrigin(Global.getSector(), run, fleet)) {
            chargeJumpCost(fleet);
        }
        primed = null;
        well = null;
    }

    void cancelCharge() {
        if (ping != null) {
            Global.getSector().removeScript(ping);
            ping = null;
        }
        primed = null;
        well = null;
    }

    private void chargeJumpCost(CampaignFleetAPI fleet) {
        float crCostFleetMult = getCRCostMult(fleet);
        if (crCostFleetMult > 0f) {
            for (FleetMemberAPI member : getNonReadyShips()) {
                if ((float) Math.random() < EmergencyBurnAbility.ACTIVATION_DAMAGE_PROB) {
                    Misc.applyDamage(member, null, FleetMemberDamageLevel.LOW, false, null, null, true, null,
                            I18n.forGameText(() -> Translation.msg("chart.jump.damage").arg("ship", member.getShipName()).text()));
                }
            }
            String crReason = Translation.gameText("chart.jump.crReason");
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                member.getRepairTracker().applyCREvent(-member.getDeployCost() * CR_COST_MULT * crCostFleetMult, crReason);
            }
            fleet.getMemoryWithoutUpdate().unset(EXTRA_CR_ONCE_KEY);
        }
        fleet.getCargo().removeFuel(computeFuelCost());
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltip(tooltip, expanded);
        CampaignFleetAPI fleet = getFleet();
        if (fleet != null && chartRunAround(fleet) != null) {
            VanillaText.plainPara(tooltip, Translation.gameText("chart.jump.tooltip"), TOOLTIP_PAD, Misc.getHighlightColor());
        }
    }

    static ChartRun chartRunAround(CampaignFleetAPI fleet) {
        LocationAPI location = fleet.getContainingLocation();
        if (fleet.isInHyperspace() || location == null || !location.hasTag(ChartIds.SYSTEM_TAG)) {
            return null;
        }
        ChartRun run = ChartRuns.currentOrNull();
        SectorAPI sector = Global.getSector();
        return run != null && sector.getStarSystem(run.systemId()) == location ? run : null;
    }
}
