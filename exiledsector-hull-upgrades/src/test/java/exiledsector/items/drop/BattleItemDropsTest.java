package exiledsector.items.drop;

import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.hull.HullItem;
import exiledsector.items.hull.HullUpgradePoints;
import exiledsector.items.hull.HullUpgradesSetting;
import exiledsector.items.modifier.ItemRoll;
import exiledsector.items.modifier.ItemRoller;
import exiledsector.items.modifier.RolledModifier;
import exiledsector.items.schematic.MasterSchematic;
import exiledsector.items.schematic.RefitSchematic;
import exiledsector.items.schematic.ShipSchematic;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.DropType;
import exiledsector.loot.FleetLoss;
import exiledsector.loot.FleetLootRules;
import exiledsector.loot.Loot;
import exiledsector.loot.LootMultipliers;
import exiledsector.skills.skilleffect.logistics.FoundItemQuantity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static exiledsector.items.HullItemRows.MILITARY_PREFIXES;
import static exiledsector.items.HullItemRows.MILITARY_SUFFIXES;
import static exiledsector.items.HullItemRows.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleItemDropsTest {

    private static final String FULL_BRIDGE = "bridge_full";
    private static final String THIN_BRIDGE = "bridge_thin";
    private static final String FULL_REACTOR = "reactor_full";
    private static final String HULL_RELIC = "hull_relic";
    private static final String FACTION_RELIC = "faction_relic";
    private static final float LEVEL_ONE_SHARE = 0.5f;

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(row(FULL_BRIDGE, "bridge", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row(THIN_BRIDGE, "bridge", "HULL_PERCENT:4:6"))
                .put(row(FULL_REACTOR, "reactor", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row(HULL_RELIC, "weapon_mount", "HULL_PERCENT:4:6").put("unique", "true")));
        BattleLootRates.register(new JSONArray()
                .put(rateRow("hull_item", "0.6", "linear"))
                .put(rateRow("refit_schematic", "0.1", "linear"))
                .put(rateRow("master_schematic", "0.1", "linear")));
    }

    @AfterEach
    void tearDown() {
        MasterSchematic.requiredWhen(null);
        BattleLootRates.clear();
        ItemDefinitions.clear();
    }

    private static JSONObject rateRow(String drop, String rate, String curve) throws Exception {
        return new JSONObject().put("drop", drop).put("per_100_dp", rate).put("curve", curve);
    }

    private static FleetLoss source(float deploymentPoints) {
        return source(deploymentPoints, ItemChances.BASE);
    }

    private static FleetLoss source(float deploymentPoints, ItemChances chances) {
        return new FleetLoss(deploymentPoints, 1f, 0f, rules(chances, true));
    }

    private static FleetLootRules rules(ItemChances chances, boolean ownLedger) {
        FleetLootRules.Kind kind = ownLedger ? FleetLootRules.Kind.OWN_LEDGER : FleetLootRules.Kind.LEVEL_SCALED;
        return new FleetLootRules(kind, new LootMultipliers(chances, CraftingDrops.BASE_RARITY), false);
    }

    private static BattleItemDrops.Battle battle(FleetLoss... sources) {
        return new BattleItemDrops.Battle(List.of(sources), null, LEVEL_ONE_SHARE);
    }

    private static List<BattleItemDrops.Planned> plan(BattleItemDrops.Battle battle) {
        return BattleItemDrops.plan(battle, List.of(), neverLucky(), true);
    }

    private static Random neverLucky() {
        return new Random() {
            @Override
            public float nextFloat() {
                return 0.999f;
            }
        };
    }

    private static Map<BattleItemDrops.Drop, Integer> counts(List<BattleItemDrops.Planned> planned) {
        Map<BattleItemDrops.Drop, Integer> counts = new EnumMap<>(BattleItemDrops.Drop.class);
        planned.forEach(entry -> counts.merge(entry.drop(), 1, Integer::sum));
        return counts;
    }

    @Test
    void aShipsTreeBoostsItsTwoModifierShareDoublingAtThirtyNodesAndTriplingAtSixty() {
        assertEquals(1f, BattleItemDrops.levelBoost(0), 1e-4f);
        assertEquals(2f, BattleItemDrops.levelBoost(30), 1e-4f);
        assertEquals(3f, BattleItemDrops.levelBoost(60), 1e-4f);
        assertEquals(1f, BattleItemDrops.levelBoost(-5), 1e-4f);
    }

    @Test
    void eachRowExpectsDeploymentPointsOverAHundredTimesItsRateAndAThirdOfHullItemsRollTwoModifiers() {
        assertEquals(Map.of(BattleItemDrops.Drop.MODS_1, 4, BattleItemDrops.Drop.MODS_2, 2, BattleItemDrops.Drop.REFIT_SCHEMATIC, 1),
                counts(plan(battle(source(1000f)))));
    }

    @Test
    void thePlayersHullItemQuantityScalesTheHullItemsButNotTheSchematics() {
        try (MockedStatic<FoundItemQuantity> found = Mockito.mockStatic(FoundItemQuantity.class)) {
            found.when(() -> FoundItemQuantity.nodePercent(DropType.HULL_ITEM)).thenReturn(100f);

            assertEquals(Map.of(BattleItemDrops.Drop.MODS_1, 8, BattleItemDrops.Drop.MODS_2, 4, BattleItemDrops.Drop.REFIT_SCHEMATIC, 1),
                    counts(plan(battle(source(1000f)))));
            assertEquals(Map.of(BattleItemDrops.Drop.MODS_1, 80, BattleItemDrops.Drop.MODS_2, 40,
                    BattleItemDrops.Drop.REFIT_SCHEMATIC, 10), counts(plan(battle(source(10000f)))));
        }
    }

    @Test
    void thePlayersItemQuantityScalesEveryRowIncludingSchematics() {
        try (MockedStatic<FoundItemQuantity> found = Mockito.mockStatic(FoundItemQuantity.class)) {
            found.when(() -> FoundItemQuantity.nodePercent(DropType.ITEM)).thenReturn(100f);

            assertEquals(Map.of(BattleItemDrops.Drop.MODS_1, 80, BattleItemDrops.Drop.MODS_2, 40,
                    BattleItemDrops.Drop.REFIT_SCHEMATIC, 20), counts(plan(battle(source(10000f)))));
        }
    }

    @Test
    void thePlayersHullItemQuantityAddsToTheChartsItemQuantityInsteadOfMultiplyingIt() {
        BattleItemDrops.Battle chartFleet = battle(source(1000f, ItemChances.increasedBy(DropType.ITEM, 100f)));

        try (MockedStatic<FoundItemQuantity> found = Mockito.mockStatic(FoundItemQuantity.class)) {
            found.when(() -> FoundItemQuantity.nodePercent(DropType.HULL_ITEM)).thenReturn(100f);
            List<BattleItemDrops.Planned> planned = plan(chartFleet);

            assertEquals(12, counts(planned).get(BattleItemDrops.Drop.MODS_1));
            assertEquals(6, counts(planned).get(BattleItemDrops.Drop.MODS_2));
        }
    }

    @Test
    void aBiggerEnemyTreeTurnsMoreHullItemsTwoModifierWithoutDroppingMore() {
        FleetLootRules full = rules(ItemChances.BASE, true);
        Map<BattleItemDrops.Drop, Integer> doubled = counts(plan(battle(new FleetLoss(1000f, 1f, 30f, full))));
        Map<BattleItemDrops.Drop, Integer> capped = counts(plan(battle(new FleetLoss(1000f, 1f, 60f, full))));

        assertEquals(2, doubled.get(BattleItemDrops.Drop.MODS_1));
        assertEquals(4, doubled.get(BattleItemDrops.Drop.MODS_2));
        assertEquals(1, capped.get(BattleItemDrops.Drop.MODS_1));
        assertEquals(4, capped.get(BattleItemDrops.Drop.MODS_2));
    }

    @Test
    void difficultyStopsAtThreeTimesAndTheFleetsItemQuantityAndShareScaleItsDrops() throws Exception {
        FleetLootRules full = rules(ItemChances.BASE, true);
        assertEquals(12, counts(plan(battle(new FleetLoss(1000f, 6f, 0f, full)))).get(BattleItemDrops.Drop.MODS_1));
        assertEquals(8, counts(plan(battle(new FleetLoss(1000f, 1f, 0f, rules(ItemChances.increasedBy(DropType.ITEM, 100f), true)))))
                .get(BattleItemDrops.Drop.MODS_1));

        assertEquals(2, counts(plan(battle(new FleetLoss(1000f, 1f, 0f, rules(ItemChances.BASE, false)))))
                .get(BattleItemDrops.Drop.MODS_1));
        assertEquals(4, counts(plan(battle(new FleetLoss(1000f, 1f, 0f, rules(ItemChances.BASE, true)))))
                .get(BattleItemDrops.Drop.MODS_1));
    }

    @Test
    void severalSourcesAddUpAndTheGeneralChanceScalesEveryRow() {
        BattleItemDrops.Battle twoFleets = battle(source(400f, ItemChances.atShare(2f)), source(200f));

        assertEquals(4, counts(plan(twoFleets)).get(BattleItemDrops.Drop.MODS_1));
    }

    @Test
    void aSectionsQuantityAddsItemsOfThatSectionOnTopOfTheHullItemRoll() {
        ItemChances bridgeBonus = ItemChances.increasedBy(DropType.BRIDGE, 100f);
        List<BattleItemDrops.Planned> planned = BattleItemDrops.plan(battle(source(2000f, bridgeBonus)),
                List.of(ItemKind.BRIDGE, ItemKind.REACTOR), neverLucky(), true);

        long targeted = planned.stream().filter(entry -> entry.kind() != null).count();
        assertTrue(planned.stream().filter(entry -> entry.kind() != null).allMatch(entry -> entry.kind() == ItemKind.BRIDGE));
        assertEquals(4 + 2, targeted);
        assertEquals(8, counts(planned.stream().filter(entry -> entry.kind() == null).toList()).get(BattleItemDrops.Drop.MODS_1));
    }

    @Test
    void aLinearRowScalesWithTheWholeBattle() {
        List<BattleItemDrops.Planned> planned = plan(battle(source(10000f)));

        assertEquals(Map.of(BattleItemDrops.Drop.MODS_1, 40, BattleItemDrops.Drop.MODS_2, 20,
                BattleItemDrops.Drop.REFIT_SCHEMATIC, 10), counts(planned));
    }

    @Test
    void aDiminishingRowSlowsPastTheKneeOfTheWholeBattleAndSharesThatAcrossItsFleets() throws Exception {
        BattleLootRates.register(new JSONArray().put(rateRow("hull_item", "0.6", "diminishing")));

        assertEquals(Map.of(BattleItemDrops.Drop.MODS_1, 36, BattleItemDrops.Drop.MODS_2, 18), counts(plan(battle(source(12000f)))));
        assertEquals(Map.of(BattleItemDrops.Drop.MODS_1, 36, BattleItemDrops.Drop.MODS_2, 18),
                counts(plan(battle(source(6000f), source(6000f)))));
    }

    @Test
    void onlyTheHullItemsAnOverchargingFleetDropsAreOvercharged() {
        FleetLoss overcharging = new FleetLoss(1000f, 1f, 0f, rules(ItemChances.BASE, true).overchargingHullItems());

        List<BattleItemDrops.Planned> planned = plan(battle(source(1000f), overcharging));

        List<BattleItemDrops.Planned> overchargedItems = planned.stream().filter(BattleItemDrops.Planned::overcharged).toList();
        assertFalse(overchargedItems.isEmpty());
        assertEquals(counts(overchargedItems), counts(planned.stream().filter(entry -> !entry.overcharged()).toList()));
        Random random = new Random(5L);
        for (int trial = 0; trial < 20; trial++) {
            for (ItemData item : BattleItemDrops.roll(battle(overcharging), random, definition -> false, true).items()) {
                assertEquals(1, item.explicits().stream().filter(RolledModifier::overcharged).count(), item.toString());
            }
            for (ItemData item : BattleItemDrops.roll(battle(source(1000f)), random, definition -> false, true).items()) {
                assertTrue(item.explicits().stream().noneMatch(RolledModifier::overcharged), item.toString());
            }
        }
    }

    @Test
    void aForcedModifierCountRollsExactlyThatManyModifiersAndSurvivesTheCargoRoundTrip() {
        ItemDefinition fullBridge = ItemDefinitions.get(FULL_BRIDGE);
        for (int modifierCount = 1; modifierCount <= 4; modifierCount++) {
            for (long seed = 0; seed < 50; seed++) {
                ItemData rolled = ItemData.rolled(fullBridge, seed, ItemRoll.withModifierCount(modifierCount));

                assertEquals(modifierCount, rolled.explicits().size());
                assertEquals(rolled.explicits(), ItemData.of(rolled.toSpecialItem()).explicits());
            }
        }
        assertTrue(ItemRoller.canRollModifierCount(ItemDefinitions.get(THIN_BRIDGE), 1));
        assertFalse(ItemRoller.canRollModifierCount(ItemDefinitions.get(THIN_BRIDGE), 2));
        assertFalse(ItemRoller.canRollModifierCount(fullBridge, 5));
        assertFalse(ItemRoller.canRollModifierCount(ItemDefinitions.get(HULL_RELIC), 1));
    }

    @Test
    void aBattleNeverRollsAnItemThatCannotHoldItsModifiersAndUniquesNeedTheirUnlock() {
        BattleItemDrops.Battle bigBattle = new BattleItemDrops.Battle(List.of(source(4000f)), "pirates", LEVEL_ONE_SHARE);
        Random random = new Random(3L);
        int relics = 0;
        int lockedRelics = 0;
        for (int trial = 0; trial < 100; trial++) {
            Loot loot = BattleItemDrops.roll(bigBattle, random, definition -> true, true);
            for (ItemData item : loot.items()) {
                relics += HULL_RELIC.equals(item.definitionId()) ? 1 : 0;
                assertFalse(THIN_BRIDGE.equals(item.definitionId()) && item.explicits().size() > 1, item.toString());
            }
            for (ItemData item : BattleItemDrops.roll(bigBattle, random, definition -> false, true).items()) {
                lockedRelics += HULL_RELIC.equals(item.definitionId()) ? 1 : 0;
            }
        }

        assertTrue(relics > 0);
        assertEquals(0, lockedRelics);
    }

    @Test
    void aFactionSourcedUniqueIsTwiceAsLikelyFromThatFactionsBattlesAndStillDropsFromOthers() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(row(HULL_RELIC, "weapon_mount", "HULL_PERCENT:4:6").put("unique", "true"))
                .put(row(FACTION_RELIC, "reactor", "HULL_PERCENT:4:6").put("unique", "true")
                        .put(HullItem.UNIQUE_SOURCE_COLUMN, "faction:tritachyon")));
        List<FleetLoss> sources = List.of(source(4000f));

        Map<String, Integer> fromPirates = uniqueDrops(new BattleItemDrops.Battle(sources, "pirates", LEVEL_ONE_SHARE));
        Map<String, Integer> fromTriTachyon = uniqueDrops(new BattleItemDrops.Battle(sources, "tritachyon", LEVEL_ONE_SHARE));

        assertEquals(List.of(UniqueSource.parse("faction:tritachyon")), ItemDefinitions.get(FACTION_RELIC).uniqueSources());
        assertEquals(0.5, share(fromPirates, FACTION_RELIC), 0.05);
        assertEquals(2.0 / 3.0, share(fromTriTachyon, FACTION_RELIC), 0.05);
    }

    private static double share(Map<String, Integer> drops, String definitionId) {
        int total = drops.values().stream().mapToInt(Integer::intValue).sum();
        assertTrue(total > 500, drops.toString());
        return drops.getOrDefault(definitionId, 0) / (double) total;
    }

    private static Map<String, Integer> uniqueDrops(BattleItemDrops.Battle battle) {
        Map<String, Integer> drops = new HashMap<>();
        Random random = new Random(5L);
        for (int trial = 0; trial < 2000; trial++) {
            for (ItemData item : BattleItemDrops.roll(battle, random, definition -> true, true).items()) {
                if (ItemDefinitions.get(item.definitionId()).unique()) {
                    drops.merge(item.definitionId(), 1, Integer::sum);
                }
            }
        }
        return drops;
    }

    @Test
    void aBattleWithNoLossesDropsNothing() {
        assertEquals(Loot.NONE, BattleItemDrops.roll(battle(), new Random(1L), definition -> true, true));
    }

    @Test
    void battlesRollRefitSchematicTiersByTheirDropWeights() {
        Map<RefitSchematic, Integer> tierCounts = schematicTiersDropped(6000);
        int schematicCount = tierCounts.values().stream().mapToInt(Integer::intValue).sum();

        assertTrue(schematicCount > 10000, "schematics rolled: " + schematicCount);
        assertEquals(0.60, tierCounts.get(RefitSchematic.SCRAPPED) / (double) schematicCount, 0.02);
        assertEquals(0.28, tierCounts.get(RefitSchematic.STANDARD) / (double) schematicCount, 0.02);
        assertEquals(0.10, tierCounts.get(RefitSchematic.MILITARY) / (double) schematicCount, 0.01);
        assertEquals(0.02, tierCounts.get(RefitSchematic.DOMAIN) / (double) schematicCount, 0.005);
    }

    @Test
    void noRefitSchematicDropsFromBattlesWhileStarChartProgressIsOn() {
        HullUpgradePoints.install();
        HullUpgradesSetting.setPointsModeForTests(HullUpgradesSetting.PointsMode.STAR_CHART_PROGRESS);
        try {
            assertEquals(Map.of(), schematicTiersDropped(200));
        } finally {
            HullUpgradesSetting.setPointsModeForTests(HullUpgradesSetting.PointsMode.REFIT_SCHEMATICS);
        }
        assertFalse(schematicTiersDropped(200).isEmpty());
    }

    @Test
    void schematicsOnlyDropWhileTheyAreRequired() {
        BattleItemDrops.Battle battle = battle(source(1000f));

        assertNull(counts(plan(battle)).get(BattleItemDrops.Drop.MASTER_SCHEMATIC));

        MasterSchematic.requiredWhen(() -> true);
        assertEquals(1, counts(plan(battle)).get(BattleItemDrops.Drop.MASTER_SCHEMATIC));
    }

    @Test
    void beforeSectionsOpenABattleDropsMasterSchematicsAndNothingElse() {
        MasterSchematic.requiredWhen(() -> true);
        BattleItemDrops.Battle battle = battle(source(1000f));

        Map<BattleItemDrops.Drop, Integer> closed = counts(BattleItemDrops.plan(battle, List.of(), neverLucky(), false));

        assertEquals(Map.of(BattleItemDrops.Drop.MASTER_SCHEMATIC, 1), closed);
        Loot loot = BattleItemDrops.roll(battle, neverLucky(),
                definition -> true, false);
        assertEquals(List.of(MasterSchematic.INSTANCE), loot.schematics());
        assertTrue(loot.items().isEmpty());
    }

    private static Map<RefitSchematic, Integer> schematicTiersDropped(int battles) {
        BattleItemDrops.Battle battle = battle(source(2000f));
        Random random = new Random(5L);
        Map<RefitSchematic, Integer> tierCounts = new EnumMap<>(RefitSchematic.class);
        for (int trial = 0; trial < battles; trial++) {
            for (ShipSchematic upgrade : BattleItemDrops.roll(battle, random, definition -> true, true).schematics()) {
                if (upgrade instanceof RefitSchematic schematic) {
                    tierCounts.merge(schematic, 1, Integer::sum);
                }
            }
        }
        return tierCounts;
    }
    @Test
    void rarityLeavesHowManyItemsABattleDropsUnchanged() {
        List<BattleItemDrops.Planned> plain = plan(battle(source(10000f)));
        List<BattleItemDrops.Planned> rarer = plan(battle(source(10000f, ItemChances.BASE.plusRarityPercent(DropType.ITEM, 100f))));

        assertEquals(counts(plain), counts(rarer));
    }

    @Test
    void aTwelfthOfABattlesHullItemsDropUniqueAndRarityRaisesThatShareUpToAll() {
        assertEquals(1f / 12f, BattleItemDrops.uniqueChance(battle(source(1000f)), DropType.HULL_ITEM), 1e-6f);
        assertEquals(1f / 6f, BattleItemDrops.uniqueChance(battle(source(1000f, ItemChances.BASE.plusRarityPercent(DropType.ITEM, 100f))),
                DropType.HULL_ITEM), 1e-6f);
        assertEquals(1f, BattleItemDrops.uniqueChance(battle(source(1000f, ItemChances.BASE.plusRarityPercent(DropType.ITEM, 2000f))),
                DropType.HULL_ITEM), 1e-6f);
    }

    @Test
    void theUniqueShareReadsTheHullItemRarityChain() {
        BattleItemDrops.Battle hullItems = battle(source(1000f, ItemChances.BASE.plusRarityPercent(DropType.HULL_ITEM, 100f)));
        BattleItemDrops.Battle subroutines = battle(source(1000f, ItemChances.BASE.plusRarityPercent(DropType.SUBROUTINE, 100f)));

        assertEquals(1f / 6f, BattleItemDrops.uniqueChance(hullItems, DropType.HULL_ITEM), 1e-6f);
        assertEquals(1f / 12f, BattleItemDrops.uniqueChance(subroutines, DropType.HULL_ITEM), 1e-6f);
    }

    @Test
    void aSectionsRarityRaisesOnlyThatSectionsUniqueShare() {
        BattleItemDrops.Battle weaponMounts = battle(source(1000f, ItemChances.BASE.plusRarityPercent(DropType.WEAPON_MOUNT, 100f)));

        assertEquals(1f / 6f, BattleItemDrops.uniqueChance(weaponMounts, DropType.WEAPON_MOUNT), 1e-6f);
        assertEquals(1f / 12f, BattleItemDrops.uniqueChance(weaponMounts, DropType.REACTOR), 1e-6f);
    }

    @Test
    void fleetsShareTheBattlesUniqueChanceByHowManyHullItemsEachDrops() {
        FleetLoss rarer = source(1000f, ItemChances.BASE.plusRarityPercent(DropType.ITEM, 100f));
        FleetLoss moreItems = source(1000f, ItemChances.increasedBy(DropType.HULL_ITEM, 100f));

        assertEquals(1f / 8f, BattleItemDrops.uniqueChance(battle(rarer, source(1000f)), DropType.HULL_ITEM), 1e-6f);
        assertEquals((2f / 12f + 2f / 12f) / 3f, BattleItemDrops.uniqueChance(battle(rarer, moreItems), DropType.HULL_ITEM), 1e-6f);
    }

    @Test
    void aHullItemThatRollsUniqueDropsAsACommonWhenNoUniqueFits() {
        ItemChances alwaysUnique = ItemChances.BASE.plusRarityPercent(DropType.ITEM, 2000f).plusPercent(DropType.BRIDGE, 100f);
        BattleItemDrops.Battle battle = battle(source(2000f, alwaysUnique));
        long planned = BattleItemDrops.plan(battle, DroppableDefinitions.hullItemKindsWithBasics(), neverLucky(), true).stream()
                .filter(entry -> entry.drop().modifierCount() > 0).count();

        List<ItemData> unlocked = BattleItemDrops.roll(battle, neverLucky(), definition -> true, true).items();
        List<ItemData> locked = BattleItemDrops.roll(battle, neverLucky(), definition -> false, true).items();

        assertEquals(planned, unlocked.size());
        assertEquals(planned, locked.size());
        assertTrue(locked.stream().noneMatch(item -> item.definition().unique()));
        assertTrue(unlocked.stream().anyMatch(item -> item.definition().kind() == ItemKind.BRIDGE));
        assertTrue(unlocked.stream().filter(item -> item.definition().kind() == ItemKind.BRIDGE).noneMatch(item -> item.definition().unique()));
        assertTrue(unlocked.stream().filter(item -> item.definition().kind() != ItemKind.BRIDGE).allMatch(item -> HULL_RELIC.equals(item.definitionId())));
    }

    @Test
    void schematicsReadTheSchematicNodeAndNotTheHullItemNode() {
        MasterSchematic.requiredWhen(() -> true);
        Map<BattleItemDrops.Drop, Integer> plain = counts(plan(battle(source(10000f))));
        Map<BattleItemDrops.Drop, Integer> moreSchematics = counts(plan(battle(source(10000f, ItemChances.increasedBy(DropType.SCHEMATIC, 100f)))));
        Map<BattleItemDrops.Drop, Integer> moreHullItems = counts(plan(battle(source(10000f, ItemChances.increasedBy(DropType.HULL_ITEM, 100f)))));

        assertTrue(moreSchematics.get(BattleItemDrops.Drop.REFIT_SCHEMATIC) > plain.get(BattleItemDrops.Drop.REFIT_SCHEMATIC));
        assertTrue(moreSchematics.get(BattleItemDrops.Drop.MASTER_SCHEMATIC) > plain.get(BattleItemDrops.Drop.MASTER_SCHEMATIC));
        assertEquals(plain.get(BattleItemDrops.Drop.MODS_1), moreSchematics.get(BattleItemDrops.Drop.MODS_1));
        assertEquals(plain.get(BattleItemDrops.Drop.REFIT_SCHEMATIC), moreHullItems.get(BattleItemDrops.Drop.REFIT_SCHEMATIC));
        assertEquals(plain.get(BattleItemDrops.Drop.MASTER_SCHEMATIC), moreHullItems.get(BattleItemDrops.Drop.MASTER_SCHEMATIC));
    }

    @Test
    void aDroppedHullItemRollsOneOrTwoModifiersWhateverTheSiteRarity() {
        ItemDefinition fullBridge = ItemDefinitions.get(FULL_BRIDGE);
        int singleModifier = 0;
        for (long seed = 0; seed < 4000; seed++) {
            ItemData dropped = ItemData.rolled(fullBridge, seed, ItemRoll.dropAtRarity(1f));
            assertEquals(dropped, ItemData.rolled(fullBridge, seed, ItemRoll.dropAtRarity(2f)));
            assertTrue(dropped.explicits().size() == 1 || dropped.explicits().size() == 2, dropped.toString());
            singleModifier += dropped.explicits().size() == 1 ? 1 : 0;
        }

        assertEquals(0.64, singleModifier / 4000.0, 0.03);
    }
}
