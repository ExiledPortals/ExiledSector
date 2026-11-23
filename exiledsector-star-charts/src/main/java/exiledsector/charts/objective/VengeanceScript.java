package exiledsector.charts.objective;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.content.ChartFleetMarks;
import exiledsector.charts.content.ChartFleetOrders;
import exiledsector.compat.FirstFailureLog;
import exiledsector.i18n.Translation;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class VengeanceScript implements EveryFrameScript {

    static final FleetAssignment MUSTER = FleetAssignment.ATTACK_LOCATION;
    static final FleetAssignment STRIKE = FleetAssignment.INTERCEPT;
    static final float CHECK_DAYS = 0.1f;
    static final float GATHER_RADIUS = 1000f;
    static final float GATHERED_SHARE = 0.75f;
    static final float MOST_MUSTER_DAYS = 15f;
    private static final FirstFailureLog FAILURES = FirstFailureLog.errors(Logger.getLogger(VengeanceScript.class));

    private final LocationAPI location;
    private final SectorEntityToken rallyPoint;
    private float daysMustering;
    private float daysSinceCheck;
    private boolean striking;
    private boolean done;

    VengeanceScript(LocationAPI location, SectorEntityToken rallyPoint) {
        this.location = location;
        this.rallyPoint = rallyPoint;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        SectorAPI sector = Global.getSector();
        daysSinceCheck += sector.getClock().convertToDays(amount);
        if (daysSinceCheck < CHECK_DAYS) {
            return;
        }
        float days = daysSinceCheck;
        daysSinceCheck = 0f;
        try {
            tick(sector.getPlayerFleet(), days);
        } catch (RuntimeException | LinkageError e) {
            FAILURES.report("[ExiledSector] Leading the Star Chart fleets avenging their boss failed; trying again shortly", e);
        }
    }

    void tick(CampaignFleetAPI playerFleet, float days) {
        List<CampaignFleetAPI> avengers = avengers();
        if (avengers.isEmpty()) {
            done = true;
            return;
        }
        boolean playerHere = playerFleet != null && playerFleet.getContainingLocation() == location;
        if (!striking) {
            daysMustering += days;
            striking = playerHere && readyToStrike(gathered(avengers), avengers.size(), daysMustering);
        }
        if (striking && !playerHere) {
            return;
        }
        String actionText = Translation.gameText(striking ? "chart.objective.vengeance.strike" : "chart.objective.vengeance.muster");
        for (CampaignFleetAPI avenger : avengers) {
            if (avenger.getBattle() != null) {
                continue;
            }
            if (striking) {
                ChartFleetOrders.withdraw(avenger, MUSTER, rallyPoint);
                ChartFleetOrders.putFirst(avenger, STRIKE, playerFleet, actionText);
            } else {
                ChartFleetOrders.putFirst(avenger, MUSTER, rallyPoint, actionText);
            }
        }
    }

    static boolean readyToStrike(int gathered, int avengers, float daysMustering) {
        return daysMustering >= MOST_MUSTER_DAYS || gathered >= Math.ceil(avengers * GATHERED_SHARE);
    }

    boolean isStriking() {
        return striking;
    }

    private List<CampaignFleetAPI> avengers() {
        List<CampaignFleetAPI> avengers = new ArrayList<>();
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (ChartFleetMarks.isAvenger(fleet) && ChartFleetOrders.canTakeOrders(fleet) && !fleet.isEmpty()) {
                avengers.add(fleet);
            }
        }
        return avengers;
    }

    private int gathered(List<CampaignFleetAPI> avengers) {
        int gathered = 0;
        for (CampaignFleetAPI avenger : avengers) {
            if (avenger.getLocation() != null && Misc.getDistance(avenger.getLocation(), rallyPoint.getLocation()) <= GATHER_RADIUS) {
                gathered++;
            }
        }
        return gathered;
    }
}
