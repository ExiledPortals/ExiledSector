package exiledsector.items.drop;

import exiledsector.data.RandomRounding;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemKind;
import exiledsector.items.modifier.ItemOvercharge;
import exiledsector.items.modifier.ItemRoll;
import exiledsector.items.modifier.ItemRoller;
import exiledsector.items.schematic.MasterSchematic;
import exiledsector.items.schematic.RefitSchematic;
import exiledsector.items.schematic.ShipSchematic;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.DropType;
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
        MODS_1(HULL_ITEM_RATE, 1, DropType.HULL_ITEM),
        MODS_2(HULL_ITEM_RATE, 2, DropType.HULL_ITEM),
        REFIT_SCHEMATIC("refit_schematic", 0, DropType.SCHEMATIC),
        MASTER_SCHEMATIC("master_schematic", 0, DropType.SCHEMATIC),
        UNIQUE("unique_hull_item", 0, DropType.HULL_ITEM);

        private final String rateId;
        private final int modifierCount;
        private final DropType dropType;

        Drop(String rateId, int modifierCount, DropType dropType) {
            this.rateId = rateId;
            this.modifierCount = modifierCount;
            this.dropType = dropType;
        }

        public DropType dropType() {
            return dropType;
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

    public static Loot roll(Battle battle, Random random, Predicate<ItemDefinition> uniqueAllowed, boolean hullItemDropsOpen) {
        if (battle == null || battle.isEmpty()) {
            return Loot.NONE;
        }
        List<ItemData> items = new ArrayList<>();
        List<ShipSchematic> schematics = new ArrayList<>();
        List<ItemKind> kindsWithBasics = hullItemDropsOpen ? DroppableDefinitions.hullItemKindsWithBasics() : List.of();
        for (Planned planned : plan(battle, kindsWithBasics, random, hullItemDropsOpen)) {
            if (planned.drop() == Drop.REFIT_SCHEMATIC) {
                schematics.add(RefitSchematic.roll(random));
            } else if (planned.drop() == Drop.MASTER_SCHEMATIC) {
                schematics.add(MasterSchematic.INSTANCE);
            } else if (planned.drop() == Drop.UNIQUE) {
                ItemDefinition definition = DroppableDefinitions.pickUniqueFromFaction(battle.uniqueFactionId(), random,
                        candidate -> candidate.isHullItem() && uniqueAllowed.test(candidate));
                if (definition != null && keepsUnique(battle, definition, random)) {
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

    static List<Planned> plan(Battle battle, List<ItemKind> kindsWithBasics, Random random, boolean hullItemDropsOpen) {
        List<Planned> planned = new ArrayList<>();
        float battleDeploymentPoints = battle.deploymentPoints();
        boolean anyOvercharging = battle.fleetLosses().stream().anyMatch(loss -> loss.rules().overchargesHullItems());
        for (Drop drop : Drop.values()) {
            if (!allowed(drop, hullItemDropsOpen)) {
                continue;
            }
            Expected expected = Expected.NONE;
            for (FleetLoss loss : battle.fleetLosses()) {
                expected = expected.plus(loss, plannedExpected(loss, drop, battleDeploymentPoints, battle.levelScaledShare()));
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
                            expected(loss, drop, battleDeploymentPoints, battle.levelScaledShare())
                                    * chancesOf(loss).extraOverParent(DropType.forItemKind(kind)) / kindsWithBasics.size());
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

    private static float expected(FleetLoss loss, Drop drop, float battleDeploymentPoints, float levelScaledShare) {
        BattleLootRates.Rate rate = BattleLootRates.rate(drop.rateId());
        if (rate == null) {
            return 0f;
        }
        float modifierShare = switch (drop) {
            case MODS_1 -> 1f - twoModifierShare(loss);
            case MODS_2 -> twoModifierShare(loss);
            default -> 1f;
        };
        return rate.expectedFrom(loss, battleDeploymentPoints, levelScaledShare) * modifierShare;
    }

    private static float plannedExpected(FleetLoss loss, Drop drop, float battleDeploymentPoints, float levelScaledShare) {
        if (drop == Drop.UNIQUE) {
            return highestUniqueExpected(loss, battleDeploymentPoints, levelScaledShare);
        }
        return expected(loss, drop, battleDeploymentPoints, levelScaledShare) * chancesOf(loss).quantity(drop.dropType());
    }

    private static float uniqueExpected(FleetLoss loss, DropType uniqueType, float battleDeploymentPoints, float levelScaledShare) {
        ItemChances chances = chancesOf(loss);
        return expected(loss, Drop.UNIQUE, battleDeploymentPoints, levelScaledShare) * chances.rarity(uniqueType) * chances.quantity(uniqueType);
    }

    private static float highestUniqueExpected(FleetLoss loss, float battleDeploymentPoints, float levelScaledShare) {
        float highest = uniqueExpected(loss, DropType.HULL_ITEM, battleDeploymentPoints, levelScaledShare);
        for (ItemKind kind : ItemKind.sectionItemTypes()) {
            highest = Math.max(highest, uniqueExpected(loss, DropType.forItemKind(kind), battleDeploymentPoints, levelScaledShare));
        }
        return highest;
    }

    static float uniqueKeepChance(Battle battle, ItemDefinition unique) {
        DropType uniqueType = DropType.forDefinition(unique);
        float battleDeploymentPoints = battle.deploymentPoints();
        float plannedTotal = 0f;
        float ownTotal = 0f;
        for (FleetLoss loss : battle.fleetLosses()) {
            plannedTotal += highestUniqueExpected(loss, battleDeploymentPoints, battle.levelScaledShare());
            ownTotal += uniqueExpected(loss, uniqueType, battleDeploymentPoints, battle.levelScaledShare());
        }
        return plannedTotal > 0f ? ownTotal / plannedTotal : 0f;
    }

    private static boolean keepsUnique(Battle battle, ItemDefinition unique, Random random) {
        float keepChance = uniqueKeepChance(battle, unique);
        return keepChance >= 1f || random.nextFloat() < keepChance;
    }

    private static void addPlanned(List<Planned> planned, Drop drop, ItemKind kind, boolean overcharged, int count) {
        for (int i = 0; i < count; i++) {
            planned.add(new Planned(drop, kind, overcharged));
        }
    }
}
