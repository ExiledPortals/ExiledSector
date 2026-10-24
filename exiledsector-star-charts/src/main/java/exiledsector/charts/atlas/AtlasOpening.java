package exiledsector.charts.atlas;

import exiledsector.charts.AtlasRunNode;
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.ChartLayoutRequest;
import exiledsector.charts.ChartPhenomena;
import exiledsector.charts.ChartTiers;
import exiledsector.charts.phenomenon.ChartPhenomenon;
import exiledsector.items.StarChart;
import exiledsector.modifiers.ModifierSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AtlasOpening {

    public static final int NEXUS_MODIFIERS = 4;
    public static final String POWERFUL_BOSS = "powerful_boss";
    public static final String INFESTED = "infested";
    public static final String CLEANSED = "cleansed";
    public static final String NEXUS = "nexus";
    private static final long LAYOUT_SEED_SALT = 0x4c41594f5554L;

    public enum Blocker {
        NONE,
        NOT_REACHABLE,
        BASTION_NEEDS_TOP_TIER,
        NEXUS_NEEDS_MODIFIERS;

        public boolean allowsOpening() {
            return this == NONE;
        }

        public String reasonKey() {
            return "chart.atlas.blocked." + name().toLowerCase(Locale.ROOT);
        }
    }

    private AtlasOpening() {
    }

    public static Blocker check(AtlasGenerator generator, AtlasState state, AtlasCell cell, StarChart chart) {
        if (!AtlasReach.isReachable(generator, state, cell)) {
            return Blocker.NOT_REACHABLE;
        }
        AtlasSpecial special = generator.specialAt(cell);
        if (special != null && AtlasReach.BASTION.equals(special.kind()) && (chart == null || chart.tier() < ChartTiers.HIGHEST_TIER)) {
            return Blocker.BASTION_NEEDS_TOP_TIER;
        }
        if (special != null && special.infests() && explicitModifiers(chart) < NEXUS_MODIFIERS) {
            return Blocker.NEXUS_NEEDS_MODIFIERS;
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
        boolean spent = state.has(cell, AtlasState.MARKERS_SPENT);
        List<ChartGenContext.AppliedEffect> effects = new ArrayList<>();
        boolean bossTierUp = false;
        if (node.powerfulBoss() && !spent) {
            effects.addAll(AtlasData.nodeEffects(POWERFUL_BOSS));
            bossTierUp = true;
        }
        String factionId = node.factionId();
        AtlasSpecial special = generator.specialAt(cell);
        if (node.infestationId() != null) {
            AtlasSpecial infestation = generator.rules().special(node.infestationId());
            if (state.has(cell, AtlasState.CLEANSED)) {
                effects.addAll(AtlasData.nodeEffects(CLEANSED));
            } else {
                effects.addAll(AtlasData.nodeEffects(INFESTED));
                if (infestation != null && !infestation.factionId().isEmpty()) {
                    factionId = infestation.factionId();
                }
            }
        }
        if (special != null) {
            effects.addAll(AtlasData.nodeEffects(special.infests() ? NEXUS : special.kind()));
        }
        String expansionId = spent || node.expansionId() == null ? "" : node.expansionId();
        String phenomenonId = node.phenomenonId() == null ? "" : node.phenomenonId();
        ChartPhenomena.Definition phenomenon = ChartPhenomena.definition(phenomenonId);
        if (spent && phenomenon != null && phenomenon.kind() == ChartPhenomenon.Kind.OVERLAY) {
            phenomenonId = "";
        }
        return new AtlasRunNode(cell.pack(), factionId, effects, expansionId, phenomenonId, bossTierUp);
    }

    public static ChartLayoutRequest request(AtlasGenerator generator, AtlasState state, AtlasCell cell, StarChart chart) {
        AtlasNode node = generator.node(cell);
        AtlasRunNode runNode = runNode(generator, state, cell);
        long layoutSeed = AtlasHash.of(state.seed(), LAYOUT_SEED_SALT, cell.gridX(), cell.gridY());
        return new ChartLayoutRequest(layoutSeed, null, chart.tier(), ChartGenContext.from(chart).withAtlasNode(runNode),
                AtlasData.recipeFor(node.regionId(), node.layoutId()), runNode.phenomenonId(), chart.seed());
    }
}
