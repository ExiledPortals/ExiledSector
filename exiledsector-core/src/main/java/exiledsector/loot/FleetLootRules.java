package exiledsector.loot;

public record FleetLootRules(Rates rates, boolean sourceRollsMaterials, LootMultipliers multipliers, float deploymentPointsMult,
                             boolean overchargesHullItems) {

    public enum Rates {
        REGULAR,
        FULL,
        FEEDSTOCK_ONLY,
        NONE
    }

    public static final FleetLootRules REGULAR = new FleetLootRules(Rates.REGULAR, false, LootMultipliers.NONE, 1f);
    public static final FleetLootRules FEEDSTOCK_ONLY = new FleetLootRules(Rates.FEEDSTOCK_ONLY, false, LootMultipliers.NONE, 1f);
    public static final FleetLootRules NONE = new FleetLootRules(Rates.NONE, false, LootMultipliers.NONE, 1f);

    public FleetLootRules {
        rates = rates == null ? Rates.REGULAR : rates;
        multipliers = multipliers == null ? LootMultipliers.NONE : multipliers;
        deploymentPointsMult = Math.max(0f, deploymentPointsMult);
    }

    public FleetLootRules(Rates rates, boolean sourceRollsMaterials, LootMultipliers multipliers, float deploymentPointsMult) {
        this(rates, sourceRollsMaterials, multipliers, deploymentPointsMult, false);
    }

    public FleetLootRules overchargingHullItems() {
        return new FleetLootRules(rates, sourceRollsMaterials, multipliers, deploymentPointsMult, true);
    }

    public static FleetLootRules full(LootMultipliers multipliers) {
        return new FleetLootRules(Rates.FULL, false, multipliers, 1f);
    }

    public float share(BattleLootRates.Rate rate) {
        return switch (rates) {
            case FULL -> 1f;
            case REGULAR -> rate.regularShare();
            case FEEDSTOCK_ONLY, NONE -> 0f;
        };
    }

    public boolean feedstockOnly() {
        return rates == Rates.FEEDSTOCK_ONLY;
    }

    public boolean paysNothing() {
        return rates == Rates.NONE;
    }
}
