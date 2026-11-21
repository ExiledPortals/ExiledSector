package exiledsector.harness;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import exiledsector.charts.phasing.PhaseDisplacementGate;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.json.JSONObject;

import java.util.List;

final class HarnessCampaign {

    private static final float FLEET_GAP = 200f;

    private HarnessCampaign() {
    }

    static StepOutcome notInCampaign(String stepName) {
        return Global.getCurrentState() == GameState.CAMPAIGN ? null : StepOutcome.failed(stepName + " only works in the campaign");
    }

    static void unpause(SectorAPI sector) {
        if (sector.isPaused()) {
            sector.setPaused(false);
        }
    }

    static ChartRun runAroundPlayer(SectorAPI sector) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        return playerFleet == null ? null : ChartRuns.liveIn(playerFleet.getContainingLocation());
    }

    static MarketAPI gateMarket(SectorAPI sector) {
        return PhaseDisplacementGate.playerGateMarket(sector);
    }

    static String colonyStatus(SectorAPI sector, HarnessResults results) {
        JSONObject colony = results.data(FoundColonyStep.COLONY_DATA);
        String marketId = colony == null ? null : colony.optString(FoundColonyStep.MARKET_ID_KEY, null);
        if (marketId == null) {
            return "no colony was founded in this run";
        }
        MarketAPI market = sector.getEconomy().getMarket(marketId);
        if (market == null) {
            return "the colony market " + marketId + " is no longer in the economy";
        }
        List<String> industries = market.getIndustries().stream().map(Industry::getId).toList();
        return market.getName() + " belongs to " + market.getFactionId() + (market.isPlayerOwned() ? " (player owned)" : " (not player owned)")
                + ", size " + market.getSize() + ", industries " + industries;
    }

    static void movePlayerFleetBeside(SectorAPI sector, SectorEntityToken target) {
        movePlayerFleetTo(sector, target.getContainingLocation(), target.getLocation().x + target.getRadius() + FLEET_GAP, target.getLocation().y);
    }

    static void movePlayerFleetTo(SectorAPI sector, LocationAPI destination, float x, float y) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet.getContainingLocation() != destination) {
            playerFleet.getContainingLocation().removeEntity(playerFleet);
            destination.addEntity(playerFleet);
            sector.setCurrentLocation(destination);
        }
        playerFleet.setLocation(x, y);
    }
}
