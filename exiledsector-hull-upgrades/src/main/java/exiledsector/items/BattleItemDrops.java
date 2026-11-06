package exiledsector.items;

import exiledsector.loot.BattleLootRates;
import exiledsector.loot.FleetLootRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class BattleItemDrops {

    static final String HULL_ITEM_RATE = "hull_item";
    static final float NODES_PER_DOUBLING = 30f;
    static final float BASE_TWO_MODIFIER_SHARE = 1f / 3f;
    static final float MAX_TWO_MODIFIER_SHARE = 0.75f;

    public enum Drop {
        MODS_1(HULL_ITEM_RATE, 1, false),
        MODS_2(HULL_ITEM_RATE, 2, false),
        REFIT_SCHEMATIC("refit_schematic", 0, false),
        MASTER_SCHEMATIC("master_schematic", 0, false),
        UNIQUE("unique_hull_item", 0, true);

        private final String rateId;
        private final int modifierCount;
        private final boolean rare;

        Drop(String rateId, int modifierCount, boolean rare) {
            this.rateId = rateId;
            this.modifierCount = modifierCount;
            this.rare = rare;
        }

        public boolean rare() {
            return rare;
        }

        public String rateId() {
            return rateId;
        }

        public int modifierCount() {
            return modifierCount;
        }
    }

    public record SourceLoss(float deploymentPoints, float difficultyMultiplier, float levelBoost, FleetLootRules rules) {

        public SourceLoss {
            deploymentPoints = Math.max(0f, deploymentPoints);
            levelBoost = Math.max(1f, levelBoost);
            rules = rules == null ? FleetLootRules.REGULAR : rules;
        }

        ItemChances chances() {
            return rules.multipliers().itemChances();
        }

        float twoModifierShare() {
            return Math.min(MAX_TWO_MODIFIER_SHARE, BASE_TWO_MODIFIER_SHARE * levelBoost);
        }
    }

    public record Battle(List<SourceLoss> sources, String uniqueFactionId) {

        public Battle {
            sources = sources == null ? List.of() : List.copyOf(sources);
        }

        public boolean isEmpty() {
            return deploymentPoints() <= 0f;
        }

        float deploymentPoints() {
            float total = 0f;
            for (SourceLoss source : sources) {
                total += source.deploymentPoints();
            }
            return total;
        }
    }

    record Planned(Drop drop, ItemKind kind) {
    }

    private BattleItemDrops() {
    }

    public static float levelBoost(int allocatedNodeCount) {
        return 1f + Math.max(0, allocatedNodeCount) / NODES_PER_DOUBLING;
    }

    public static HullItemDrops.SectionItemLoot roll(Battle battle, float dropMultiplier, Random random, Predicate<ItemDefinition> uniqueAllowed,
                                                 boolean hullItemDropsOpen) {
        if (battle == null || battle.isEmpty() || !(dropMultiplier > 0f)) {
            return HullItemDrops.SectionItemLoot.NONE;
        }
        List<ItemData> items = new ArrayList<>();
        List<ShipSchematic> upgrades = new ArrayList<>();
        List<ItemKind> kindsWithBasics = hullItemDropsOpen ? HullItemDrops.sectionKindsWithBasics() : List.of();
        for (Planned planned : plan(battle, dropMultiplier, kindsWithBasics, random, hullItemDropsOpen)) {
            if (planned.drop() == Drop.REFIT_SCHEMATIC) {
                upgrades.add(RefitSchematic.roll(random));
            } else if (planned.drop() == Drop.MASTER_SCHEMATIC) {
                upgrades.add(MasterSchematic.INSTANCE);
            } else if (planned.drop() == Drop.UNIQUE) {
                ItemDefinition definition = ItemDrops.pickUniqueFromFaction(battle.uniqueFactionId(), random,
                        candidate -> candidate.type() == ItemType.HULL_ITEM && uniqueAllowed.test(candidate));
                if (definition != null) {
                    items.add(ItemData.rolled(definition, random.nextLong()));
                }
            } else {
                int modifierCount = planned.drop().modifierCount();
                Predicate<ItemDefinition> holdsCount = candidate -> ItemRoller.canRollModifierCount(candidate, modifierCount);
                ItemDefinition definition = planned.kind() == null ? HullItemDrops.pickSectionItemBasic(random, holdsCount)
                        : ItemDrops.pickSectionItemBasic(planned.kind(), random, holdsCount);
                if (definition != null) {
                    items.add(ItemData.rolledWithModifierCount(definition, random.nextLong(), modifierCount));
                }
            }
        }
        return new HullItemDrops.SectionItemLoot(items, upgrades);
    }

    static List<Planned> plan(Battle battle, float dropMultiplier, List<ItemKind> kindsWithBasics, Random random, boolean hullItemDropsOpen) {
        List<Planned> planned = new ArrayList<>();
        float battleDeploymentPoints = battle.deploymentPoints();
        for (Drop drop : Drop.values()) {
            if (!allowed(drop, hullItemDropsOpen)) {
                continue;
            }
            float expected = 0f;
            for (SourceLoss source : battle.sources()) {
                expected += expected(source, drop, battleDeploymentPoints, dropMultiplier) * source.chances().general();
            }
            addPlanned(planned, drop, null, ItemDrops.wholeFeedstock(expected, random));
        }
        for (ItemKind kind : kindsWithBasics) {
            for (Drop drop : Drop.values()) {
                if (drop.modifierCount() == 0) {
                    continue;
                }
                float expected = 0f;
                for (SourceLoss source : battle.sources()) {
                    expected += expected(source, drop, battleDeploymentPoints, dropMultiplier) * source.chances().targeted(kind) / kindsWithBasics.size();
                }
                addPlanned(planned, drop, kind, ItemDrops.wholeFeedstock(expected, random));
            }
        }
        return planned;
    }

    static boolean allowed(Drop drop, boolean hullItemDropsOpen) {
        return switch (drop) {
            case MASTER_SCHEMATIC -> MasterSchematic.required();
            case REFIT_SCHEMATIC -> hullItemDropsOpen && RefitSchematic.dropsAllowed();
            default -> hullItemDropsOpen;
        };
    }

    private static float expected(SourceLoss source, Drop drop, float battleDeploymentPoints, float dropMultiplier) {
        BattleLootRates.Rate rate = BattleLootRates.rate(drop.rateId());
        if (rate == null) {
            return 0f;
        }
        FleetLootRules rules = source.rules();
        float rarityMult = drop.rare() ? source.chances().rarity() : 1f;
        float modifierShare = switch (drop) {
            case MODS_1 -> 1f - source.twoModifierShare();
            case MODS_2 -> source.twoModifierShare();
            default -> 1f;
        };
        return rate.expected(source.deploymentPoints() * rules.deploymentPointsMult(), battleDeploymentPoints)
                * rate.difficulty(source.difficultyMultiplier()) * rules.share(rate) * modifierShare * dropMultiplier * rarityMult;
    }

    private static void addPlanned(List<Planned> planned, Drop drop, ItemKind kind, int count) {
        for (int i = 0; i < count; i++) {
            planned.add(new Planned(drop, kind));
        }
    }
}
