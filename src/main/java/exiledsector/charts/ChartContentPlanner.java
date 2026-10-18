package exiledsector.charts;

import exiledsector.charts.ChartContentPlan.FleetRole;
import exiledsector.charts.ChartContentPlan.PlannedFleet;
import exiledsector.charts.ChartContentPlan.PlannedPoi;
import exiledsector.items.WeightedPick;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

final class ChartContentPlanner {

    static final long FACTION_SALT = 0x4348464143544eL;
    static final long OBJECTIVE_SALT = 0x43484f424a4543L;
    static final long LOOT_SALT = 0x43484c4f4f5453L;
    static final long FLEET_SALT = 0x4348464c454554L;
    static final long EXTRA_POI_SALT = 0x43484558545241L;
    static final int MAX_POIS = 24;
    static final int MAX_FLEETS = 12;
    static final float FLEET_POWER_CAP = 300f;
    static final float BOSS_POWER_CAP = 450f;
    static final float MIN_FLEET_POWER = 15f;
    static final float SHARE_SPREAD = 0.25f;
    static final float MAX_OVERFLOW_QUALITY = 0.25f;
    static final int OVERFLOW_OFFICER_LEVEL_BONUS = 1;
    static final float BOSS_QUALITY_BONUS = 0.15f;
    static final int BOSS_OFFICER_LEVEL_BONUS = 2;
    static final int BOSS_OFFICER_COUNT_BONUS = 2;
    static final int BOSS_SMOD_BONUS = 1;
    static final float MAX_QUALITY = 1.5f;
    static final int NO_POI = -1;

    private ChartContentPlanner() {
    }

    static ChartContentPlan plan(long seed, ChartTier tier, List<ChartPoi> pois, List<ChartFaction> factions, Predicate<String> factionLoaded) {
        return plan(seed, tier, pois, factions, factionLoaded, 0);
    }

    static ChartContentPlan plan(long seed, ChartTier tier, List<ChartPoi> pois, List<ChartFaction> factions, Predicate<String> factionLoaded,
                                 int extraPois) {
        ChartFaction faction = ChartFactions.pick(factions, tier.tier(), factionLoaded, ChartLayoutGenerator.derivedRandom(seed, FACTION_SALT));
        List<ChartPoi> vaults = affordableVaults(pois, tier);
        ChartObjective objective = pickObjective(tier, !vaults.isEmpty(), ChartLayoutGenerator.derivedRandom(seed, OBJECTIVE_SALT));
        List<PlannedPoi> plannedPois = spendLoot(tier, pois, objective == ChartObjective.VAULT ? vaults : List.of(),
                ChartLayoutGenerator.derivedRandom(seed, LOOT_SALT));
        plannedPois = addExtraPois(tier, pois, plannedPois, extraPois, ChartLayoutGenerator.derivedRandom(seed, EXTRA_POI_SALT));
        List<PlannedFleet> plannedFleets = new ArrayList<>();
        plannedPois = planFleets(tier, faction, objective, plannedPois, plannedFleets, ChartLayoutGenerator.derivedRandom(seed, FLEET_SALT));
        return new ChartContentPlan(tier.tier(), faction, objective, plannedPois, plannedFleets);
    }

    static List<ChartPoi> affordableVaults(List<ChartPoi> pois, ChartTier tier) {
        List<ChartPoi> affordable = new ArrayList<>();
        for (ChartPoi vault : ChartPois.vaultsAt(pois, tier.tier())) {
            if (vault.cost() <= tier.lootBudget()) {
                affordable.add(vault);
            }
        }
        return affordable;
    }

    static ChartObjective pickObjective(ChartTier tier, boolean vaultAvailable, Random random) {
        Map<ChartObjective, Float> weights = new EnumMap<>(ChartObjective.class);
        for (ChartObjective objective : ChartObjective.values()) {
            float weight = tier.weightFor(objective);
            if (objective == ChartObjective.VAULT && !vaultAvailable) {
                weight = 0f;
            }
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

    static List<PlannedPoi> spendLoot(ChartTier tier, List<ChartPoi> pois, List<ChartPoi> vaults, Random random) {
        List<PlannedPoi> planned = new ArrayList<>();
        float remaining = tier.lootBudget();
        if (!vaults.isEmpty()) {
            ChartPoi vault = WeightedPick.pick(vaults, candidate -> Math.max(candidate.weight(), 0.0001f), random);
            planned.add(new PlannedPoi(vault, true));
            remaining -= vault.cost();
        }
        List<ChartPoi> eligible = ChartPois.lootAt(pois, tier.tier());
        while (planned.size() < MAX_POIS) {
            List<ChartPoi> affordable = new ArrayList<>();
            for (ChartPoi poi : eligible) {
                if (poi.cost() <= remaining) {
                    affordable.add(poi);
                }
            }
            if (affordable.isEmpty()) {
                break;
            }
            float target = targetCost(affordable, remaining);
            ChartPoi picked = WeightedPick.pick(affordable, poi -> poi.weight() / (1f + Math.abs(poi.cost() - target) / target), random);
            planned.add(new PlannedPoi(picked, picked.guarded()));
            remaining -= picked.cost();
        }
        return planned;
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

    static List<PlannedPoi> planFleets(ChartTier tier, ChartFaction faction, ChartObjective objective, List<PlannedPoi> pois,
                                       List<PlannedFleet> fleets, Random random) {
        float power = tier.fleetPower() * faction.powerMult();
        int fleetCount = tier.fleetsMin() + random.nextInt(tier.fleetsMax() - tier.fleetsMin() + 1);
        if (power / fleetCount > FLEET_POWER_CAP) {
            fleetCount = Math.min(MAX_FLEETS, (int) Math.ceil(power / FLEET_POWER_CAP));
        }
        float overflow = Math.max(0f, power - fleetCount * FLEET_POWER_CAP);
        float overflowQuality = overflow <= 0f ? 0f : Math.min(MAX_OVERFLOW_QUALITY, overflow / (fleetCount * FLEET_POWER_CAP) * MAX_OVERFLOW_QUALITY);
        int overflowOfficerLevels = overflow > 0f ? OVERFLOW_OFFICER_LEVEL_BONUS : 0;
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
        float quality = Math.min(MAX_QUALITY, tier.quality() + faction.qualityBonus() + overflowQuality);
        for (int i = 0; i < fleetCount; i++) {
            float combatPoints = Math.max(MIN_FLEET_POWER, Math.min(FLEET_POWER_CAP, power * shares[i] / shareTotal));
            boolean guard = i < guardCount;
            int poiIndex = guard && i < guardedIndices.size() ? guardedIndices.get(i) : NO_POI;
            fleets.add(new PlannedFleet(guard ? FleetRole.GUARD : FleetRole.PATROL, poiIndex, combatPoints, Math.max(0f, quality),
                    tier.officerLevelBonus() + overflowOfficerLevels, tier.officerCountBonus(), tier.averageSMods()));
        }
        for (int i = 0; i < guardedPois.size(); i++) {
            PlannedPoi planned = guardedPois.get(i);
            if (planned.guarded() && !guardedIndices.subList(0, Math.min(guardCount, guardedIndices.size())).contains(i)) {
                guardedPois.set(i, new PlannedPoi(planned.poi(), false));
            }
        }
        if (tier.boss()) {
            float bossPower = Math.max(MIN_FLEET_POWER, Math.min(BOSS_POWER_CAP, tier.bossPower() * faction.powerMult()));
            fleets.add(new PlannedFleet(FleetRole.BOSS, bossTarget(guardedPois), bossPower,
                    Math.max(0f, Math.min(MAX_QUALITY, quality + BOSS_QUALITY_BONUS)), tier.officerLevelBonus() + BOSS_OFFICER_LEVEL_BONUS,
                    tier.officerCountBonus() + BOSS_OFFICER_COUNT_BONUS, tier.averageSMods() + BOSS_SMOD_BONUS));
        }
        return guardedPois;
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

    static int bossTarget(List<PlannedPoi> pois) {
        for (int i = 0; i < pois.size(); i++) {
            if (pois.get(i).poi().isVault()) {
                return i;
            }
        }
        return mostValuableIndex(pois);
    }
}
