package exiledsector.items.drop;

import exiledsector.data.RandomRounding;
import exiledsector.items.schematic.DeintegrationSchematic;
import exiledsector.items.schematic.LogisticsSchematic;
import exiledsector.items.schematic.RefitSchematic;
import exiledsector.items.schematic.ShipSchematic;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.DropType;
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

    public int rollBattle(List<FleetLoss> fleetLosses, float levelScaledShare, Random random) {
        return RandomRounding.round(expectedBattleDrops(fleetLosses, BattleLootRates.rate(rateId), levelScaledShare), random);
    }

    public static float expectedBattleDrops(List<FleetLoss> fleetLosses, BattleLootRates.Rate rate, float levelScaledShare) {
        if (rate == null || fleetLosses.isEmpty()) {
            return 0f;
        }
        return rate.expectedFromAll(fleetLosses, levelScaledShare, loss -> loss.rules().multipliers().quantity(DropType.SCHEMATIC));
    }

    public static boolean rollSiteFind(String siteId, Random random, ItemChances chances) {
        SalvageSiteDrops.Rule rule = SalvageSiteDrops.activeRule(siteId);
        return rule != null && !rule.chances().isEmpty() && random.nextFloat() < siteFindChance(rule.chances().get(0), chances.quantity(DropType.SCHEMATIC));
    }

    public static float siteFindChance(float firstItemChance, float itemQuantityMult) {
        return firstItemChance * itemQuantityMult * matchedTierShare();
    }

    public static float matchedTierShare() {
        float totalWeight = 0f;
        for (RefitSchematic tier : RefitSchematic.values()) {
            totalWeight += tier.dropWeight();
        }
        return totalWeight > 0f ? MATCHED_TIER.dropWeight() / totalWeight : 0f;
    }
}
