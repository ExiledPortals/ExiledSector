package exiledsector.loot;

public record FleetLootRules(Kind kind, LootMultipliers multipliers, boolean overchargesHullItems) {

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

    public static final FleetLootRules LEVEL_SCALED = new FleetLootRules(Kind.LEVEL_SCALED, LootMultipliers.NONE, false);
    public static final FleetLootRules FEEDSTOCK_ONLY = new FleetLootRules(Kind.FEEDSTOCK_ONLY, LootMultipliers.NONE, false);
    public static final FleetLootRules SPENT = new FleetLootRules(Kind.SPENT, LootMultipliers.NONE, false);

    public FleetLootRules {
        kind = kind == null ? Kind.LEVEL_SCALED : kind;
        multipliers = multipliers == null ? LootMultipliers.NONE : multipliers;
    }

    public static FleetLootRules ownLedger(LootMultipliers multipliers) {
        return new FleetLootRules(Kind.OWN_LEDGER, multipliers, false);
    }

    public FleetLootRules overchargingHullItems() {
        return new FleetLootRules(kind, multipliers, true);
    }

    public float itemShare(float levelScaledShare) {
        return kind.itemShare(levelScaledShare);
    }
}
