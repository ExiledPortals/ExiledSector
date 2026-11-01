package exiledsector.charts.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.layout.ChartLayoutGenerator;
import exiledsector.charts.layout.ChartLayoutRequest;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.wormhole.GatewayTierLimit;
import exiledsector.items.StarChart;
import exiledsector.modifiers.ModifierSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AtlasOpening {

    public static final int NEXUS_MODIFIERS = 4;
    public static final String INFESTED = "infested";
    public static final String CLEANSED = "cleansed";
    public static final String NEXUS = "nexus";
    private static final long LAYOUT_SEED_SALT = 0x4c41594f5554L;

    public enum Blocker {
        NONE,
        NOT_REACHABLE,
        BASTION_NEEDS_TOP_TIER,
        NEXUS_NEEDS_MODIFIERS,
        GATEWAY_TIER_LIMIT;

        public boolean allowsOpening() {
            return this == NONE;
        }

        public String reasonKey() {
            String key = "chart.atlas.blocked." + name().toLowerCase(Locale.ROOT);
            if (this == GATEWAY_TIER_LIMIT) {
                return key + (GatewayTierLimit.maxTier(Global.getSector()) < GatewayTierLimit.ADVANCED_MAX_TIER ? ".advanced" : ".perfected");
            }
            return key;
        }
    }

    private AtlasOpening() {
    }

    public static Blocker check(AtlasGenerator generator, AtlasState state, AtlasCell cell, StarChart chart) {
        return check(generator, state, cell, chart, chart == null ? ChartTiers.CORRUPTED_TIER : GatewayTierLimit.maxTier(Global.getSector()));
    }

    static Blocker check(AtlasGenerator generator, AtlasState state, AtlasCell cell, StarChart chart, int maxTier) {
        if (!AtlasReach.isReachable(generator, state, cell)) {
            return Blocker.NOT_REACHABLE;
        }
        AtlasSpecial special = generator.specialAt(cell);
        Blocker specialBlocker = special == null ? null : special.kind().openBlocker(chart);
        if (specialBlocker != null) {
            return specialBlocker;
        }
        if (chart != null && chart.tier() > maxTier) {
            return Blocker.GATEWAY_TIER_LIMIT;
        }
        return Blocker.NONE;
    }

    static int explicitModifiers(StarChart chart) {
        if (chart == null) {
            return 0;
        }
        return (int) chart.rolls().stream().filter(roll -> roll.slot() == ModifierSlot.PREFIX || roll.slot() == ModifierSlot.SUFFIX).count();
    }

    public static AtlasRunNode runNode(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        AtlasNode node = generator.node(cell);
        List<ChartGenContext.AppliedEffect> effects = new ArrayList<>();
        AtlasSpecial special = generator.specialAt(cell);
        if (node.infestationId() != null) {
            effects.addAll(AtlasData.nodeEffects(state.has(cell, AtlasState.CLEANSED) ? CLEANSED : INFESTED));
        }
        if (special != null) {
            effects.addAll(AtlasData.nodeEffects(special.kind().nodeEffectsId()));
        }
        List<String> anomalyIds = AtlasAnomalies.effective(generator, state, cell);
        return new AtlasRunNode(cell.pack(), factionFor(generator, state, cell, node), effects, anomalyIds, ChartAnomalies.guaranteeBossTierUp(anomalyIds));
    }

    public static String factionFor(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasNode node) {
        return owner(generator, state, cell, node.factionId(), node.infestationId());
    }

    public static String ownerOf(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return owner(generator, state, cell, generator.factionAt(cell), generator.infestationAt(cell));
    }

    public static String territoryOwnerOf(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return held(state, cell, generator.factionAt(cell));
    }

    private static String held(AtlasState state, AtlasCell cell, String factionId) {
        String captor = state.capturedBy(cell);
        return captor == null ? factionId : captor;
    }

    private static String owner(AtlasGenerator generator, AtlasState state, AtlasCell cell, String factionId, String infestationId) {
        String holder = held(state, cell, factionId);
        if (infestationId == null || state.has(cell, AtlasState.CLEANSED)) {
            return holder;
        }
        AtlasSpecial infestation = generator.rules().special(infestationId);
        return infestation == null || infestation.factionId().isEmpty() ? holder : infestation.factionId();
    }

    public static long layoutSeed(AtlasState state, AtlasCell cell) {
        return AtlasHash.of(state.seed(), LAYOUT_SEED_SALT, cell.gridX(), cell.gridY());
    }

    public static StarSystemType systemTypeAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        AtlasNode node = generator.node(cell);
        List<String> anomalyIds = new ArrayList<>(node.anomalyIds());
        state.anomalyRecords(cell).forEach(record -> anomalyIds.add(record.anomalyId()));
        ChartLayoutRequest request = new ChartLayoutRequest(layoutSeed(state, cell), null).withRecipe(AtlasData.recipeFor(node.regionId(), node.layoutId()));
        return ChartLayoutGenerator.systemTypeFor(request, anomalyIds);
    }

    public static ChartLayoutRequest request(AtlasGenerator generator, AtlasState state, AtlasCell cell, StarChart chart, AtlasRunNode runNode) {
        AtlasNode node = generator.node(cell);
        return new ChartLayoutRequest(layoutSeed(state, cell), null, chart.tier(), ChartGenContext.from(chart).withAtlasNode(runNode),
                AtlasData.recipeFor(node.regionId(), node.layoutId()), chart.seed());
    }
}
