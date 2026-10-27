package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledlib.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.content.ChartSites;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class RelayNetwork implements ChartAnomaly {

    public static final String ID = "relay_network";
    public static final String COST_KEY = ID + ".cost";
    public static final String STAGES_KEY = ID + ".stages";
    public static final String RELAY_TAG = ChartIds.PREFIX + "relay";
    public static final String KIND_MEMORY_KEY = "$exiledSector_chartRelayKind";
    public static final RelayNetwork INSTANCE = new RelayNetwork();
    static final long PLACEMENT_SALT = 0x52454c4159504cL;

    private RelayNetwork() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        StarSystemAPI system = site.system();
        SectorEntityToken centre = system.getCenter() != null ? system.getCenter() : system.getStar();
        Random random = site.random(PLACEMENT_SALT);
        List<SectorEntityToken> guarded = new ArrayList<>(site.guardedPois());
        Collections.shuffle(guarded, random);
        List<RelayKind> kinds = new ArrayList<>(List.of(RelayKind.values()));
        Collections.shuffle(kinds, random);
        for (int band = 0; band < kinds.size(); band++) {
            RelayKind kind = kinds.get(band);
            ChartSites.Orbit orbit = null;
            if (!guarded.isEmpty() && random.nextFloat() < RelayRules.GUARDED_CHANCE) {
                SectorEntityToken poi = guarded.remove(0);
                orbit = site.clearOrbit(poi, poi.getRadius() + RelayRules.GUARD_GAP_MIN, poi.getRadius() + RelayRules.GUARD_GAP_MAX, random);
            }
            if (orbit == null && centre != null) {
                float inner = RelayRules.FIRST_BAND_RADIUS + band * RelayRules.BAND_WIDTH;
                orbit = site.clearOrbit(centre, inner, inner + RelayRules.BAND_WIDTH, random);
            }
            if (orbit == null) {
                throw new IllegalStateException("no clear orbit for the " + kind.key());
            }
            addRelay(site, kind, orbit);
        }
    }

    static SectorEntityToken addRelay(AnomalyContentSite site, RelayKind kind, ChartSites.Orbit orbit) {
        SectorEntityToken relay = site.addEntity(relayId(site.runId(), kind), Translation.gameText("chart.relay." + kind.key() + ".name"),
                kind.entityType(), Factions.NEUTRAL, orbit);
        relay.addTag(RELAY_TAG);
        MemoryAPI memory = relay.getMemoryWithoutUpdate();
        memory.set(KIND_MEMORY_KEY, kind.key());
        memory.set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);
        return relay;
    }

    public static String relayId(String runId, RelayKind kind) {
        return ChartIds.PREFIX + "relay_" + kind.key() + "_" + runId;
    }

    public static boolean isRelay(SectorEntityToken entity) {
        return RelayKind.of(entity) != null;
    }
}
