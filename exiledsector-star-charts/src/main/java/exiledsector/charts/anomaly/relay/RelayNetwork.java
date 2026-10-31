package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.run.ChartRun;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class RelayNetwork implements ContentPlacer, RunParticipant, LootShaper {

    public static final String ANOMALY_ID = "relay_network";
    public static final String COST_KEY = ChartEffect.CHART_RELAY_COST_PERCENT.anomalyTuningKey();
    public static final String STAGES_KEY = ChartEffect.CHART_RELAY_CHAIN_STAGES.anomalyTuningKey();
    public static final String RELAY_TAG = ChartIds.PREFIX + "relay";
    public static final String KIND_MEMORY_KEY = "$exiledSector_chartRelayKind";
    public static final RelayNetwork INSTANCE = new RelayNetwork();
    static final long PLACEMENT_SALT = 0x52454c4159504cL;

    private RelayNetwork() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public InteractionDialogPlugin dialogFor(AnomalyRun run, SectorEntityToken target) {
        return isRelay(target) ? new RelayDialog() : null;
    }

    @Override
    public void checkRun(SectorAPI sector, AnomalyRun run) {
        ChartRelays.check(sector, run);
    }

    @Override
    public String intelText(AnomalyRun run) {
        return intelLine();
    }

    @Override
    public boolean holdsBackBossUnique(ChartRun run) {
        return !RelayStates.allWoken(run);
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
        Set<Integer> usedBands = new HashSet<>();
        List<Vector2f> placedAt = new ArrayList<>();
        for (int slot = 0; slot < kinds.size(); slot++) {
            RelayKind kind = kinds.get(slot);
            ChartSites.Orbit orbit = null;
            if (!guarded.isEmpty() && random.nextFloat() < RelayRules.GUARDED_CHANCE) {
                SectorEntityToken poi = guarded.remove(0);
                orbit = spreadOrbit(site, poi, poi.getRadius() + RelayRules.GUARD_GAP_MIN, poi.getRadius() + RelayRules.GUARD_GAP_MAX, placedAt, random);
            }
            if (orbit == null && centre != null) {
                orbit = bandOrbit(site, centre, RelayRules.FIRST_HOME_BAND + slot, usedBands, placedAt, random);
            }
            if (orbit == null) {
                throw new IllegalStateException("no clear orbit for the " + kind.key());
            }
            addRelay(site, kind, orbit);
            placedAt.add(orbit.position());
        }
    }

    static List<Integer> bandOrder(int homeBand) {
        List<Integer> bands = new ArrayList<>();
        for (int band = 0; band < RelayRules.BAND_COUNT; band++) {
            bands.add(band);
        }
        bands.sort(Comparator.comparingInt((Integer band) -> Math.abs(band - homeBand)).thenComparing(band -> band < homeBand));
        return bands;
    }

    private static ChartSites.Orbit bandOrbit(AnomalyContentSite site, SectorEntityToken centre, int homeBand, Set<Integer> usedBands,
                                              List<Vector2f> placedAt, Random random) {
        for (int band : bandOrder(homeBand)) {
            if (usedBands.contains(band)) {
                continue;
            }
            ChartSites.Orbit orbit = spreadOrbit(site, centre, band * RelayRules.BAND_WIDTH, (band + 1) * RelayRules.BAND_WIDTH, placedAt, random);
            if (orbit != null) {
                usedBands.add(band);
                return orbit;
            }
        }
        return null;
    }

    private static ChartSites.Orbit spreadOrbit(AnomalyContentSite site, SectorEntityToken focus, float minRadius, float maxRadius, List<Vector2f> placedAt,
                                                Random random) {
        for (int attempt = 0; attempt < RelayRules.ORBIT_ATTEMPTS; attempt++) {
            float radius = minRadius + random.nextFloat() * Math.max(0f, maxRadius - minRadius);
            ChartSites.Orbit orbit = new ChartSites.Orbit(focus, random.nextFloat() * 360f, radius, radius / RelayRules.ORBIT_SPEED);
            if (site.isClear(orbit) && clearOfRelays(orbit.position(), placedAt)) {
                return orbit;
            }
        }
        return null;
    }

    private static boolean clearOfRelays(Vector2f position, List<Vector2f> placedAt) {
        for (Vector2f other : placedAt) {
            if (Vector2f.sub(position, other, new Vector2f()).length() < RelayRules.MIN_RELAY_GAP) {
                return false;
            }
        }
        return true;
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
