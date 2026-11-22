package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.json.JSONObject;

final class VisitColonyStep implements HarnessStep {

    private String colonyName;

    @Override
    public String name() {
        return "visitColony";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign(name());
        if (notInCampaign != null) {
            return notInCampaign;
        }
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (context.firstFrame()) {
            JSONObject colony = context.results().data(FoundColonyStep.COLONY_DATA);
            String marketId = colony == null ? null : colony.optString(FoundColonyStep.MARKET_ID_KEY, null);
            MarketAPI market = marketId == null ? null : Global.getSector().getEconomy().getMarket(marketId);
            if (market == null || market.getPrimaryEntity() == null) {
                return StepOutcome.failed("visitColony needs a colony founded earlier in this run by foundColony");
            }
            colonyName = market.getName();
            campaignUI.showInteractionDialog(market.getPrimaryEntity());
            return StepOutcome.RUNNING;
        }
        if (campaignUI.getCurrentInteractionDialog() != null) {
            return StepOutcome.passed("opened the interaction dialog with " + colonyName);
        }
        return new StepOutcome(false, false, "the interaction dialog with " + colonyName + " never opened");
    }
}
