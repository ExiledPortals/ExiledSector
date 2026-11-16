package exiledsector.items;

import exiledsector.data.RandomRounding;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.FleetLoss;

import java.util.List;
import java.util.Random;

public enum SkillTreeSchematicDrops {

    LOGISTICS(LogisticsSchematic.INSTANCE, "logistics_schematic"),
    DEINTEGRATION(DeintegrationSchematic.INSTANCE, "deintegration_schematic");

    static final RefitSchematic MATCHED_TIER = RefitSchematic.MILITARY;

    private final ShipSchematic schematic;
    private final String rateId;

    SkillTreeSchematicDrops(ShipSchematic schematic, String rateId) {
        this.schematic = schematic;
        this.rateId = rateId;
    }

    public ShipSchematic schematic() {
        return schematic;
    }

    public String rateId() {
        return rateId;
    }

    public int rollBattle(List<FleetLoss> fleetLosses, float levelScaledShare, float dropMultiplier, Random random) {
        return RandomRounding.round(expectedBattleDrops(fleetLosses, BattleLootRates.rate(rateId), levelScaledShare, dropMultiplier), random);
    }

    static float expectedBattleDrops(List<FleetLoss> fleetLosses, BattleLootRates.Rate rate, float levelScaledShare, float dropMultiplier) {
        if (rate == null || fleetLosses.isEmpty() || !(dropMultiplier > 0f)) {
            return 0f;
        }
        return rate.expectedFromAll(fleetLosses, levelScaledShare, loss -> loss.rules().multipliers().chances().general()) * dropMultiplier;
    }

    public static boolean rollSiteFind(String siteId, Random random, ItemChances chances) {
        ItemDrops.Rule rule = ItemDrops.activeRule(siteId);
        return rule != null && !rule.chances().isEmpty() && random.nextFloat() < siteFindChance(rule.chances().get(0), chances.general());
    }

    static float siteFindChance(float firstItemChance, float generalChanceMult) {
        return firstItemChance * generalChanceMult * matchedTierShare();
    }

    static float matchedTierShare() {
        float totalWeight = 0f;
        for (RefitSchematic tier : RefitSchematic.values()) {
            totalWeight += tier.dropWeight();
        }
        return totalWeight > 0f ? MATCHED_TIER.dropWeight() / totalWeight : 0f;
    }
}
