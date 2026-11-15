package exiledsector.charts.anomaly.shipyard;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AbyssOrbits;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.GuardedStation;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.anomaly.VanillaDefenders;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.run.ChartRun;

import java.util.List;

public final class AbyssalShipyard implements ContentPlacer, RunParticipant, LootShaper {

    public static final String ANOMALY_ID = "abyssal_shipyard";
    public static final AbyssalShipyard INSTANCE = new AbyssalShipyard();
    static final String ENTITY_TYPE = ChartIds.PREFIX + "abyssal_shipyard";
    static final String SHIPYARD_TAG = ChartIds.PREFIX + "abyssal_shipyard";
    public static final GuardedStation STATION = new GuardedStation(ENTITY_TYPE, SHIPYARD_TAG, ChartIds.PREFIX + "abyssal_shipyard_",
            "chart.anomaly.abyssal_shipyard.name", ShipyardGuards.GUARD_SALT);
    static final String SALVAGED_KEY = "$" + ChartIds.PREFIX + "shipyardSalvaged";
    static final float MIN_ORBIT_GAP = 1500f;
    static final float MAX_ORBIT_GAP = 2500f;
    static final long PLACEMENT_SALT = 0x53485059524fL;

    private AbyssalShipyard() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public List<String> lootScopes() {
        return List.of(ShipyardDialog.LOOT_SCOPE);
    }

    @Override
    public void generate(AnomalyContentSite site) {
        ChartSites.Orbit orbit = AbyssOrbits.nearPhotosphere(site, MIN_ORBIT_GAP, MAX_ORBIT_GAP, site.random(PLACEMENT_SALT));
        STATION.place(site, orbit);
    }

    static ChartRun liveRunFor(SectorEntityToken shipyard) {
        return STATION.liveRun(shipyard, ANOMALY_ID);
    }

    static boolean salvaged(SectorEntityToken shipyard) {
        return shipyard.getMemoryWithoutUpdate().getBoolean(SALVAGED_KEY);
    }

    @Override
    public InteractionDialogPlugin dialogFor(AnomalyRun run, SectorEntityToken target) {
        return STATION.holds(target) && run.holds(target) ? new ShipyardDialog() : null;
    }

    @Override
    public String intelText(AnomalyRun run) {
        SectorEntityToken shipyard = STATION.in(run.system());
        if (shipyard == null || salvaged(shipyard)) {
            return null;
        }
        return Translation.gameText(VanillaDefenders.guarded(shipyard) ? "chart.anomaly.abyssal_shipyard.intel.guarded"
                : "chart.anomaly.abyssal_shipyard.intel.open");
    }
}
