package exiledsector.items;

import exiledsector.data.RandomRounding;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.FleetLoss;
import exiledsector.loot.Loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        private final boolean rarityScaled;

        Drop(String rateId, int modifierCount, boolean rarityScaled) {
            this.rateId = rateId;
            this.modifierCount = modifierCount;
            this.rarityScaled = rarityScaled;
        }

        public boolean rarityScaled() {
            return rarityScaled;
        }

        public String rateId() {
            return rateId;
        }

        public int modifierCount() {
            return modifierCount;
        }
    }

    public record Battle(List<FleetLoss> fleetLosses, String uniqueFactionId, float levelScaledShare) {

        public Battle {
            fleetLosses = fleetLosses == null ? List.of() : List.copyOf(fleetLosses);
        }

        public boolean isEmpty() {
            return deploymentPoints() <= 0f;
        }

        float deploymentPoints() {
            return FleetLoss.totalDeploymentPoints(fleetLosses);
        }
    }

    record Planned(Drop drop, ItemKind kind, boolean overcharged) {
    }

    private record Expected(float regular, float overcharged) {

        static final Expected NONE = new Expected(0f, 0f);

        Expected plus(FleetLoss loss, float amount) {
            return loss.rules().overchargesHullItems() ? new Expected(regular, overcharged + amount) : new Expected(regular + amount, overcharged);
        }
    }

    private BattleItemDrops() {
    }

    public static float levelBoost(float averageShipLevel) {
        return 1f + Math.max(0f, averageShipLevel) / NODES_PER_DOUBLING;
    }

    static float twoModifierShare(FleetLoss loss) {
        return Math.min(MAX_TWO_MODIFIER_SHARE, BASE_TWO_MODIFIER_SHARE * levelBoost(loss.averageShipLevel()));
    }

    private static ItemChances chancesOf(FleetLoss loss) {
        return loss.rules().multipliers().chances();
    }

    public static Loot roll(Battle battle, float dropMultiplier, float basicItemMult, Random random,
                            Predicate<ItemDefinition> uniqueAllowed, boolean hullItemDropsOpen) {
        if (battle == null || battle.isEmpty() || !(dropMultiplier > 0f)) {
            return Loot.NONE;
        }
        List<ItemData> items = new ArrayList<>();
        List<ShipSchematic> schematics = new ArrayList<>();
        List<ItemKind> kindsWithBasics = hullItemDropsOpen ? DroppableDefinitions.hullItemKindsWithBasics() : List.of();
        for (Planned planned : plan(battle, dropMultiplier, basicItemMult, kindsWithBasics, random, hullItemDropsOpen)) {
            if (planned.drop() == Drop.REFIT_SCHEMATIC) {
                schematics.add(RefitSchematic.roll(random));
            } else if (planned.drop() == Drop.MASTER_SCHEMATIC) {
                schematics.add(MasterSchematic.INSTANCE);
            } else if (planned.drop() == Drop.UNIQUE) {
                ItemDefinition definition = DroppableDefinitions.pickUniqueFromFaction(battle.uniqueFactionId(), random,
                        candidate -> candidate.isHullItem() && uniqueAllowed.test(candidate));
                if (definition != null) {
                    items.add(overchargedIfPlanned(planned, ItemData.rolled(definition, random.nextLong()), random));
                }
            } else {
                int modifierCount = planned.drop().modifierCount();
                Predicate<ItemDefinition> holdsCount = candidate -> ItemRoller.canRollModifierCount(candidate, modifierCount);
                ItemDefinition definition = planned.kind() == null ? DroppableDefinitions.pickHullItemBasic(random, holdsCount)
                        : DroppableDefinitions.pickBasicOfKind(planned.kind(), random, holdsCount);
                if (definition != null) {
                    items.add(overchargedIfPlanned(planned, ItemData.rolled(definition, random.nextLong(), ItemRoll.withModifierCount(modifierCount)), random));
                }
            }
        }
        return new Loot(items, schematics, Map.of());
    }

    private static ItemData overchargedIfPlanned(Planned planned, ItemData item, Random random) {
        return planned.overcharged() ? ItemOvercharge.overcharge(item, random) : item;
    }

    static List<Planned> plan(Battle battle, float dropMultiplier, float basicItemMult, List<ItemKind> kindsWithBasics, Random random,
                              boolean hullItemDropsOpen) {
        List<Planned> planned = new ArrayList<>();
        float battleDeploymentPoints = battle.deploymentPoints();
        boolean anyOvercharging = battle.fleetLosses().stream().anyMatch(loss -> loss.rules().overchargesHullItems());
        for (Drop drop : Drop.values()) {
            if (!allowed(drop, hullItemDropsOpen)) {
                continue;
            }
            float quantityMult = drop.modifierCount() > 0 ? basicItemMult : 1f;
            Expected expected = Expected.NONE;
            for (FleetLoss loss : battle.fleetLosses()) {
                expected = expected.plus(loss,
                        expected(loss, drop, battleDeploymentPoints, battle.levelScaledShare(), dropMultiplier * quantityMult)
                                * chancesOf(loss).general());
            }
            addPlanned(planned, drop, null, expected, anyOvercharging, random);
        }
        for (ItemKind kind : kindsWithBasics) {
            for (Drop drop : Drop.values()) {
                if (drop.modifierCount() == 0) {
                    continue;
                }
                Expected expected = Expected.NONE;
                for (FleetLoss loss : battle.fleetLosses()) {
                    expected = expected.plus(loss,
                            expected(loss, drop, battleDeploymentPoints, battle.levelScaledShare(), dropMultiplier * basicItemMult)
                                    * chancesOf(loss).targeted(kind) / kindsWithBasics.size());
                }
                addPlanned(planned, drop, kind, expected, anyOvercharging, random);
            }
        }
        return planned;
    }

    private static void addPlanned(List<Planned> planned, Drop drop, ItemKind kind, Expected expected, boolean anyOvercharging, Random random) {
        addPlanned(planned, drop, kind, false, RandomRounding.round(expected.regular(), random));
        if (anyOvercharging) {
            addPlanned(planned, drop, kind, true, RandomRounding.round(expected.overcharged(), random));
        }
    }

    static boolean allowed(Drop drop, boolean hullItemDropsOpen) {
        return switch (drop) {
            case MASTER_SCHEMATIC -> MasterSchematic.required();
            case REFIT_SCHEMATIC -> hullItemDropsOpen && RefitSchematic.dropsAllowed();
            default -> hullItemDropsOpen;
        };
    }

    private static float expected(FleetLoss loss, Drop drop, float battleDeploymentPoints, float levelScaledShare, float dropMultiplier) {
        BattleLootRates.Rate rate = BattleLootRates.rate(drop.rateId());
        if (rate == null) {
            return 0f;
        }
        float rarityMult = drop.rarityScaled() ? chancesOf(loss).rarity() : 1f;
        float modifierShare = switch (drop) {
            case MODS_1 -> 1f - twoModifierShare(loss);
            case MODS_2 -> twoModifierShare(loss);
            default -> 1f;
        };
        return rate.expectedFrom(loss, battleDeploymentPoints, levelScaledShare) * modifierShare * dropMultiplier * rarityMult;
    }

    private static void addPlanned(List<Planned> planned, Drop drop, ItemKind kind, boolean overcharged, int count) {
        for (int i = 0; i < count; i++) {
            planned.add(new Planned(drop, kind, overcharged));
        }
    }
}
