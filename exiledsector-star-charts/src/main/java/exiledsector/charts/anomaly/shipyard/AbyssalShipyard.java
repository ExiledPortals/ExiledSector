package exiledsector.charts.anomaly.shipyard;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.anomaly.VanillaDefenders;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.layout.abyss.ChartAbyss;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;

import java.util.List;
import java.util.Random;

public final class AbyssalShipyard implements ContentPlacer, RunParticipant, LootShaper {

    public static final String ANOMALY_ID = "abyssal_shipyard";
    public static final AbyssalShipyard INSTANCE = new AbyssalShipyard();
    static final String ENTITY_TYPE = ChartIds.PREFIX + "abyssal_shipyard";
    static final String SHIPYARD_TAG = ChartIds.PREFIX + "abyssal_shipyard";
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
        Random random = site.random(PLACEMENT_SALT);
        List<CustomCampaignEntityAPI> photospheres = ChartAbyss.photospheres(site.system());
        SectorEntityToken focus = photospheres.isEmpty() ? site.centre() : photospheres.get(random.nextInt(photospheres.size()));
        float coreRadius = focus == null ? 0f : focus.getRadius();
        ChartSites.Orbit orbit = site.clearOrbit(focus, coreRadius + MIN_ORBIT_GAP, coreRadius + MAX_ORBIT_GAP, random);
        if (orbit == null) {
            orbit = AnomalyContentSite.orbit(focus, random.nextFloat() * 360f, coreRadius + MIN_ORBIT_GAP);
        }
        SectorEntityToken shipyard = site.addEntity(shipyardId(site.runId()), Translation.gameText("chart.anomaly.abyssal_shipyard.name"),
                ENTITY_TYPE, Factions.NEUTRAL, orbit);
        shipyard.addTag(SHIPYARD_TAG);
        VanillaDefenders.arm(shipyard, site.random(ShipyardGuards.GUARD_SALT).nextLong());
    }

    public static String shipyardId(String runId) {
        return ChartIds.PREFIX + "abyssal_shipyard_" + runId;
    }

    static boolean isShipyard(SectorEntityToken entity) {
        return entity != null && entity.hasTag(SHIPYARD_TAG);
    }

    static ChartRun liveRunFor(SectorEntityToken shipyard) {
        ChartRun run = isShipyard(shipyard) ? ChartRuns.liveOwning(shipyard) : null;
        return run != null && run.anomalyIds().contains(ANOMALY_ID) ? run : null;
    }

    static boolean salvaged(SectorEntityToken shipyard) {
        return shipyard.getMemoryWithoutUpdate().getBoolean(SALVAGED_KEY);
    }

    static SectorEntityToken shipyardIn(LocationAPI location) {
        List<SectorEntityToken> found = location == null ? List.of() : location.getEntitiesWithTag(SHIPYARD_TAG);
        return found.isEmpty() ? null : found.get(0);
    }

    @Override
    public InteractionDialogPlugin dialogFor(AnomalyRun run, SectorEntityToken target) {
        return isShipyard(target) && run.holds(target) ? new ShipyardDialog() : null;
    }

    @Override
    public String intelText(AnomalyRun run) {
        SectorEntityToken shipyard = shipyardIn(run.system());
        if (shipyard == null || salvaged(shipyard)) {
            return null;
        }
        return Translation.gameText(VanillaDefenders.guarded(shipyard) ? "chart.anomaly.abyssal_shipyard.intel.guarded"
                : "chart.anomaly.abyssal_shipyard.intel.open");
    }
}
