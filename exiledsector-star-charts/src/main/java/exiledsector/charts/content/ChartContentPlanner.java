package exiledsector.charts.content;

import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.content.ChartContentPlan.PlannedPoi;
import exiledsector.charts.content.ChartContentPlan.PlannedStation;
import exiledsector.charts.anomaly.skirmish.FactionSkirmish;
import exiledsector.charts.layout.ChartLayoutGenerator;
import exiledsector.charts.objective.ChartObjective;
import exiledsector.items.WeightedPick;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public final class ChartContentPlanner {

    static final long FACTION_SALT = 0x4348464143544eL;
    static final long OBJECTIVE_SALT = 0x43484f424a4543L;
    static final long LOOT_SALT = 0x43484c4f4f5453L;
    static final long FLEET_SALT = 0x4348464c454554L;
    static final long EXTRA_POI_SALT = 0x43484558545241L;
    static final long BOSS_SALT = 0x4348424f535354L;
    static final long SKIRMISH_SALT = 0x4348534b524d53L;
    static final int MAX_POIS = 24;
    public static final int MAX_FLEETS = 15;
    public static final float FLEET_SIZE_CAP = 1200f;
    public static final float BOSS_SIZE_CAP = 2400f;
    static final float MIN_FLEET_SIZE = 30f;
    static final float SHARE_SPREAD = 0.25f;
    public static final int MAX_OVERFLOW_STRENGTH = 50;
    static final float BOSS_QUALITY_BONUS = 15f;
    static final int BOSS_OFFICER_LEVEL_BONUS = 2;
    static final int BOSS_OFFICER_COUNT_BONUS = 2;
    static final int STATION_OFFICER_LEVEL = 5;
    static final float MAX_QUALITY = 300f;
    static final float QUALITY_PERCENT_PER_SMOD = 50f;
    static final int NO_POI = -1;
    static final List<ChartPoi.Kind> SPEND_ORDER = List.of(ChartPoi.Kind.DOMAIN, ChartPoi.Kind.STATION, ChartPoi.Kind.DERELICT, ChartPoi.Kind.CACHE);

    private ChartContentPlanner() {
    }

    public static ChartContentPlan plan(long seed, ChartTier tier, List<ChartPoi> pois, List<ChartFaction> factions, Predicate<String> factionLoaded) {
        return plan(seed, tier, pois, factions, factionLoaded, ChartGenContext.none());
    }

    public static ChartContentPlan plan(long seed, ChartTier baseTier, List<ChartPoi> pois, List<ChartFaction> factions, Predicate<String> factionLoaded,
                                        ChartGenContext chartContext) {
        return plan(seed, baseTier, pois, factions, factionLoaded, chartContext, StarChartSettings.DEFAULT_FLEET_LIMIT);
    }

    public static ChartContentPlan plan(long seed, ChartTier baseTier, List<ChartPoi> pois, List<ChartFaction> factions, Predicate<String> factionLoaded,
                                        ChartGenContext chartContext, int fleetLimit) {
        return plan(seed, baseTier, pois, factions, factionLoaded, chartContext, fleetLimit, null);
    }

    static ChartContentPlan plan(long seed, ChartTier baseTier, List<ChartPoi> pois, List<ChartFaction> factions, Predicate<String> factionLoaded,
                                 ChartGenContext chartContext, int fleetLimit, List<String> anomalyIds) {
        List<String> plannedAnomalies = anomalyIds == null ? ChartAnomalies.planned(chartContext) : anomalyIds;
        ChartGenContext context = chartContext.withAnomalyEffects(plannedAnomalies);
        ChartTier tier = context.adjust(baseTier);
        ChartFaction faction = ChartFactions.resolve(context.factionId(), factions, factionLoaded, ChartLayoutGenerator.derivedRandom(seed, FACTION_SALT));
        ChartObjective objective = pickObjective(tier, ChartLayoutGenerator.derivedRandom(seed, OBJECTIVE_SALT));
        List<PlannedPoi> plannedPois = spendLoot(tier, pois, ChartLayoutGenerator.derivedRandom(seed, LOOT_SALT));
        plannedPois = addExtraPois(tier, pois, plannedPois, context.extraPois(), ChartLayoutGenerator.derivedRandom(seed, EXTRA_POI_SALT));
        boolean skirmish = plannedAnomalies.contains(FactionSkirmish.ID);
        Random bossRandom = ChartLayoutGenerator.derivedRandom(seed, BOSS_SALT);
        ChartBossType bossType = skirmish ? ChartBossType.STATION
                : tier.boss() ? ChartBosses.pickType(tier.tier(), faction.factionId(), bossRandom) : ChartBossType.FLEET;
        String stationVariant = bossType == ChartBossType.STATION ? ChartBosses.pickStation(faction.factionId(), tier.tier(), bossRandom) : null;
        if (stationVariant == null) {
            bossType = ChartBossType.FLEET;
        }
        List<PlannedFleet> plannedFleets = new ArrayList<>();
        plannedPois = planFleets(tier, objective, plannedPois, plannedFleets, ChartLayoutGenerator.derivedRandom(seed, FLEET_SALT),
                context.extraFleets(), fleetLimit, bossType);
        PlannedStation bossStation = stationVariant == null ? null : plannedStation(tier, stationVariant, plannedFleets);
        ChartContentPlan plan = new ChartContentPlan(tier.tier(), faction, objective, plannedPois, plannedFleets, plannedAnomalies, bossType, bossStation);
        return skirmish ? SkirmishPlanner.apply(plan, tier, context, factions, factionLoaded, ChartLayoutGenerator.derivedRandom(seed, SKIRMISH_SALT))
                : plan;
    }

    static PlannedStation plannedStation(ChartTier tier, String variantId, List<PlannedFleet> plannedFleets) {
        int guardedPoi = NO_POI;
        for (PlannedFleet planned : plannedFleets) {
            if (planned.role() == FleetRole.DEFENDER) {
                guardedPoi = planned.poiIndex();
            }
        }
        return new PlannedStation(variantId, STATION_OFFICER_LEVEL + tier.officerLevelBonus() + BOSS_OFFICER_LEVEL_BONUS,
                plannedBoss(tier, fleetQuality(tier), guardedPoi, ChartBossType.FLEET));
    }

    static float fleetQuality(ChartTier tier) {
        return Math.max(0f, Math.min(MAX_QUALITY, tier.fleetQuality()));
    }

    static ChartObjective pickObjective(ChartTier tier, Random random) {
        Map<ChartObjective, Float> weights = new EnumMap<>(ChartObjective.class);
        for (ChartObjective objective : ChartObjective.values()) {
            float weight = tier.weightFor(objective);
            if (weight > 0f) {
                weights.put(objective, weight);
            }
        }
        if (weights.isEmpty()) {
            return ChartObjective.CLEAR;
        }
        List<ChartObjective> candidates = new ArrayList<>(weights.keySet());
        return WeightedPick.pick(candidates, weights::get, random);
    }

    static List<PlannedPoi> spendLoot(ChartTier tier, List<ChartPoi> pois, Random random) {
        List<PlannedPoi> planned = new ArrayList<>();
        List<ChartPoi> eligible = ChartPois.lootAt(pois, tier.tier());
        for (ChartPoi.Kind kind : SPEND_ORDER) {
            spendSiteBudget(tier.siteBudget(kind), ChartPois.ofKind(eligible, kind), planned, random);
        }
        return planned;
    }

    private static void spendSiteBudget(float budget, List<ChartPoi> members, List<PlannedPoi> planned, Random random) {
        float remaining = budget;
        while (planned.size() < MAX_POIS && remaining > 0f && !members.isEmpty()) {
            List<ChartPoi> affordable = new ArrayList<>();
            for (ChartPoi poi : members) {
                if (poi.cost() <= remaining) {
                    affordable.add(poi);
                }
            }
            if (affordable.isEmpty()) {
                buyLeftover(remaining, members, planned, random);
                return;
            }
            float target = targetCost(affordable, remaining);
            ChartPoi picked = WeightedPick.pick(affordable, poi -> poi.weight() / (1f + Math.abs(poi.cost() - target) / target), random);
            planned.add(new PlannedPoi(picked, picked.guarded()));
            remaining -= picked.cost();
        }
    }

    private static void buyLeftover(float remaining, List<ChartPoi> members, List<PlannedPoi> planned, Random random) {
        float cheapestCost = Float.MAX_VALUE;
        for (ChartPoi poi : members) {
            cheapestCost = Math.min(cheapestCost, poi.cost());
        }
        if (random.nextFloat() >= remaining / cheapestCost) {
            return;
        }
        List<ChartPoi> cheapest = new ArrayList<>();
        for (ChartPoi poi : members) {
            if (poi.cost() == cheapestCost) {
                cheapest.add(poi);
            }
        }
        ChartPoi picked = WeightedPick.pick(cheapest, ChartPoi::weight, random);
        planned.add(new PlannedPoi(picked, picked.guarded()));
    }

    static List<PlannedPoi> addExtraPois(ChartTier tier, List<ChartPoi> pois, List<PlannedPoi> planned, int extraPois, Random random) {
        List<ChartPoi> eligible = ChartPois.lootAt(pois, tier.tier());
        if (extraPois <= 0 || eligible.isEmpty() || planned.size() >= MAX_POIS) {
            return planned;
        }
        List<PlannedPoi> extended = new ArrayList<>(planned);
        for (int added = 0; added < extraPois && extended.size() < MAX_POIS; added++) {
            ChartPoi picked = WeightedPick.pick(eligible, poi -> Math.max(poi.weight(), 0.0001f), random);
            extended.add(new PlannedPoi(picked, picked.guarded()));
        }
        return extended;
    }

    static float targetCost(List<ChartPoi> affordable, float remaining) {
        float costTotal = 0f;
        for (ChartPoi poi : affordable) {
            costTotal += poi.cost();
        }
        float meanCost = costTotal / affordable.size();
        int expectedPicks = Math.max(1, Math.round(remaining / meanCost));
        return Math.max(0.0001f, remaining / expectedPicks);
    }

    static List<PlannedPoi> planFleets(ChartTier tier, ChartObjective objective, List<PlannedPoi> pois,
                                       List<PlannedFleet> fleets, Random random, int extraFleets, int fleetLimit) {
        return planFleets(tier, objective, pois, fleets, random, extraFleets, fleetLimit, ChartBossType.FLEET);
    }

    static List<PlannedPoi> planFleets(ChartTier tier, ChartObjective objective, List<PlannedPoi> pois,
                                       List<PlannedFleet> fleets, Random random, int extraFleets, int fleetLimit, ChartBossType bossType) {
        int rolledFleets = tier.fleetsMin() + random.nextInt(tier.fleetsMax() - tier.fleetsMin() + 1);
        ChartFleetSpread spread = ChartFleetSpread.of(rolledFleets, tier.fleetSize(), extraFleets, fleetLimit);
        int fleetCount = spread.fleets();
        float fleetSize = spread.fleetSize();
        float[] shares = new float[fleetCount];
        float shareTotal = 0f;
        for (int i = 0; i < fleetCount; i++) {
            shares[i] = 1f - SHARE_SPREAD + random.nextFloat() * SHARE_SPREAD * 2f;
            shareTotal += shares[i];
        }
        List<PlannedPoi> guardedPois = new ArrayList<>(pois);
        List<Integer> guardedIndices = guardedIndices(guardedPois);
        if (objective == ChartObjective.CLEAR && guardedIndices.isEmpty() && !guardedPois.isEmpty()) {
            int mostValuable = mostValuableIndex(guardedPois);
            guardedPois.set(mostValuable, new PlannedPoi(guardedPois.get(mostValuable).poi(), true));
            guardedIndices = List.of(mostValuable);
        }
        int guardCount = Math.min(fleetCount, guardedIndices.size());
        if (objective == ChartObjective.CLEAR && guardCount == 0) {
            guardCount = 1;
        }
        float quality = fleetQuality(tier);
        for (int i = 0; i < fleetCount; i++) {
            float sizePercent = Math.max(MIN_FLEET_SIZE, fleetSize * shares[i] * fleetCount / shareTotal);
            boolean guard = i < guardCount;
            int poiIndex = guard && i < guardedIndices.size() ? guardedIndices.get(i) : NO_POI;
            fleets.add(new PlannedFleet(guard ? FleetRole.GUARD : FleetRole.PATROL, poiIndex, ChartTier.fleetPoints(Math.min(FLEET_SIZE_CAP, sizePercent)),
                    quality / ChartTier.PERCENT, tier.officerLevelBonus(), tier.officerCountBonus(),
                    Math.min(MAX_OVERFLOW_STRENGTH, spread.strengthBonus() + overflowStrength(sizePercent, FLEET_SIZE_CAP)),
                    averageSMods(quality)));
        }
        for (int i = 0; i < guardedPois.size(); i++) {
            PlannedPoi planned = guardedPois.get(i);
            if (planned.guarded() && !guardedIndices.subList(0, Math.min(guardCount, guardedIndices.size())).contains(i)) {
                guardedPois.set(i, new PlannedPoi(planned.poi(), false));
            }
        }
        if (tier.boss()) {
            fleets.add(plannedBoss(tier, quality, mostValuableIndex(guardedPois), bossType));
        }
        return guardedPois;
    }

    public static PlannedFleet plannedBoss(ChartTier tier, float fleetQuality, int targetPoi) {
        return plannedBoss(tier, fleetQuality, targetPoi, ChartBossType.FLEET);
    }

    static PlannedFleet plannedBoss(ChartTier tier, float fleetQuality, int targetPoi, ChartBossType bossType) {
        ChartBosses.Definition definition = ChartBosses.definition(bossType);
        float fleetShare = definition == null ? 1f : definition.fleetShare();
        float bossSize = Math.max(MIN_FLEET_SIZE, tier.bossSize() * fleetShare);
        float bossQuality = Math.max(0f, Math.min(MAX_QUALITY, fleetQuality + BOSS_QUALITY_BONUS));
        FleetRole role = bossType == ChartBossType.STATION ? FleetRole.DEFENDER : FleetRole.BOSS;
        return new PlannedFleet(role, targetPoi, ChartTier.fleetPoints(Math.min(BOSS_SIZE_CAP, bossSize)),
                bossQuality / ChartTier.PERCENT,
                tier.officerLevelBonus() + BOSS_OFFICER_LEVEL_BONUS, tier.officerCountBonus() + BOSS_OFFICER_COUNT_BONUS,
                overflowStrength(bossSize, BOSS_SIZE_CAP), averageSMods(bossQuality));
    }

    static int averageSMods(float qualityPercent) {
        return Math.max(0, Math.round((qualityPercent - ChartTier.PERCENT) / QUALITY_PERCENT_PER_SMOD));
    }

    public static int overflowStrength(float sizePercent, float cap) {
        float overflow = sizePercent - cap;
        return overflow <= 0f ? 0 : Math.min(MAX_OVERFLOW_STRENGTH, Math.round(overflow / cap * ChartTier.PERCENT));
    }

    private static List<Integer> guardedIndices(List<PlannedPoi> pois) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < pois.size(); i++) {
            if (pois.get(i).guarded()) {
                indices.add(i);
            }
        }
        return indices;
    }

    static int mostValuableIndex(List<PlannedPoi> pois) {
        int best = NO_POI;
        for (int i = 0; i < pois.size(); i++) {
            if (best == NO_POI || pois.get(i).poi().cost() > pois.get(best).poi().cost()) {
                best = i;
            }
        }
        return best;
    }
}
