package exiledsector.loot;

public record FleetLootRules(Kind kind, LootMultipliers multipliers, float deploymentPointsMult, boolean overchargesHullItems) {

    public enum Kind {
        LEVEL_SCALED,
        OWN_LEDGER,
        FEEDSTOCK_ONLY,
        SPENT;

        public float itemShare(float levelScaledShare) {
            return switch (this) {
                case LEVEL_SCALED -> levelScaledShare;
                case OWN_LEDGER -> 1f;
                case FEEDSTOCK_ONLY, SPENT -> 0f;
            };
        }

        public boolean rollsBattleCraftingItems() {
            return this == LEVEL_SCALED;
        }
    }

    public static final FleetLootRules LEVEL_SCALED = new FleetLootRules(Kind.LEVEL_SCALED, LootMultipliers.NONE, 1f, false);
    public static final FleetLootRules FEEDSTOCK_ONLY = new FleetLootRules(Kind.FEEDSTOCK_ONLY, LootMultipliers.NONE, 1f, false);
    public static final FleetLootRules SPENT = new FleetLootRules(Kind.SPENT, LootMultipliers.NONE, 1f, false);

    public FleetLootRules {
        kind = kind == null ? Kind.LEVEL_SCALED : kind;
        multipliers = multipliers == null ? LootMultipliers.NONE : multipliers;
        deploymentPointsMult = Math.max(0f, deploymentPointsMult);
    }

    public static FleetLootRules ownLedger(LootMultipliers multipliers, float deploymentPointsMult) {
        return new FleetLootRules(Kind.OWN_LEDGER, multipliers, deploymentPointsMult, false);
    }

    public FleetLootRules overchargingHullItems() {
        return new FleetLootRules(kind, multipliers, deploymentPointsMult, true);
    }

    public float itemShare(float levelScaledShare) {
        return kind.itemShare(levelScaledShare);
    }
}
