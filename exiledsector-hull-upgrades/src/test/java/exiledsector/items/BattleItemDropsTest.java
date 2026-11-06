package exiledsector.items;

import exiledsector.loot.BattleLootRates;
import exiledsector.loot.FleetLootRules;
import exiledsector.loot.LootMultipliers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(row(FULL_BRIDGE, "bridge", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row(THIN_BRIDGE, "bridge", "HULL_PERCENT:4:6"))
                .put(row(FULL_REACTOR, "reactor", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row(HULL_RELIC, "weapon_mount", "HULL_PERCENT:4:6").put("unique", "true")));
        BattleLootRates.register(new JSONArray()
                .put(rateRow("hull_item", "0.6", "linear"))
                .put(rateRow("unique_hull_item", "0.015", "linear"))
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
        return new JSONObject().put("drop", drop).put("per_100_dp", rate).put("curve", curve).put("regular_share", "1");
    }

    private static BattleItemDrops.SourceLoss source(float deploymentPoints) {
        return source(deploymentPoints, ItemChances.BASE);
    }

    private static BattleItemDrops.SourceLoss source(float deploymentPoints, ItemChances chances) {
        return new BattleItemDrops.SourceLoss(deploymentPoints, 1f, 1f, rules(chances, false, 1f));
    }

    private static FleetLootRules rules(ItemChances chances, boolean fullRates, float deploymentPointsMult) {
        return new FleetLootRules(fullRates ? FleetLootRules.Rates.FULL : FleetLootRules.Rates.REGULAR, false, new LootMultipliers(chances, 1f, 1f),
                deploymentPointsMult);
    }

    private static BattleItemDrops.Battle battle(BattleItemDrops.SourceLoss... sources) {
        return new BattleItemDrops.Battle(List.of(sources), null);
    }

    private static List<BattleItemDrops.Planned> plan(BattleItemDrops.Battle battle, float dropMultiplier) {
        return BattleItemDrops.plan(battle, dropMultiplier, List.of(), neverLucky(), true);
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
                counts(plan(battle(source(1000f)), 1f)));
    }

    @Test
    void aBiggerEnemyTreeTurnsMoreHullItemsTwoModifierWithoutDroppingMore() {
        FleetLootRules regular = rules(ItemChances.BASE, false, 1f);
        Map<BattleItemDrops.Drop, Integer> doubled = counts(plan(battle(new BattleItemDrops.SourceLoss(1000f, 1f, 2f, regular)), 1f));
        Map<BattleItemDrops.Drop, Integer> capped = counts(plan(battle(new BattleItemDrops.SourceLoss(1000f, 1f, 3f, regular)), 1f));

        assertEquals(2, doubled.get(BattleItemDrops.Drop.MODS_1));
        assertEquals(4, doubled.get(BattleItemDrops.Drop.MODS_2));
        assertEquals(1, capped.get(BattleItemDrops.Drop.MODS_1));
        assertEquals(4, capped.get(BattleItemDrops.Drop.MODS_2));
    }

    @Test
    void difficultyStopsAtThreeTimesAndTheFleetsDeploymentPointMultiplierAndShareScaleItsDrops() throws Exception {
        FleetLootRules regular = rules(ItemChances.BASE, false, 1f);
        assertEquals(12, counts(plan(battle(new BattleItemDrops.SourceLoss(1000f, 6f, 1f, regular)), 1f)).get(BattleItemDrops.Drop.MODS_1));
        assertEquals(8, counts(plan(battle(new BattleItemDrops.SourceLoss(1000f, 1f, 1f, rules(ItemChances.BASE, false, 2f))), 1f))
                .get(BattleItemDrops.Drop.MODS_1));

        BattleLootRates.register(new JSONArray().put(rateRow("hull_item", "0.6", "linear").put("regular_share", "0.5")));
        assertEquals(2, counts(plan(battle(source(1000f)), 1f)).get(BattleItemDrops.Drop.MODS_1));
        assertEquals(4, counts(plan(battle(new BattleItemDrops.SourceLoss(1000f, 1f, 1f, rules(ItemChances.BASE, true, 1f))), 1f))
                .get(BattleItemDrops.Drop.MODS_1));
    }

    @Test
    void severalSourcesAddUpAndTheGeneralChanceAndSettingScaleEveryRow() {
        BattleItemDrops.Battle twoFleets = battle(source(400f, ItemChances.of(2f)), source(200f));

        assertEquals(4, counts(plan(twoFleets, 1f)).get(BattleItemDrops.Drop.MODS_1));
        assertEquals(2, counts(plan(twoFleets, 0.5f)).get(BattleItemDrops.Drop.MODS_1));
    }

    @Test
    void aTargetedChanceAddsItemsOfThatSectionOnTopOfTheGeneralRoll() {
        ItemChances bridgeBonus = new ItemChances(1f, Map.of(ItemKind.BRIDGE, 1f));
        List<BattleItemDrops.Planned> planned = BattleItemDrops.plan(battle(source(2000f, bridgeBonus)), 1f,
                List.of(ItemKind.BRIDGE, ItemKind.REACTOR), neverLucky(), true);

        long targeted = planned.stream().filter(entry -> entry.kind() != null).count();
        assertTrue(planned.stream().filter(entry -> entry.kind() != null).allMatch(entry -> entry.kind() == ItemKind.BRIDGE));
        assertEquals(4 + 2, targeted);
        assertEquals(8, counts(planned.stream().filter(entry -> entry.kind() == null).toList()).get(BattleItemDrops.Drop.MODS_1));
    }

    @Test
    void aLinearRowScalesWithTheWholeBattle() {
        List<BattleItemDrops.Planned> planned = plan(battle(source(10000f)), 1f);

        assertEquals(Map.of(BattleItemDrops.Drop.MODS_1, 40, BattleItemDrops.Drop.MODS_2, 20, BattleItemDrops.Drop.UNIQUE, 1,
                BattleItemDrops.Drop.REFIT_SCHEMATIC, 10), counts(planned));
    }

    @Test
    void aDiminishingRowSlowsPastTheKneeOfTheWholeBattleAndSharesThatAcrossItsFleets() throws Exception {
        BattleLootRates.register(new JSONArray().put(rateRow("hull_item", "0.6", "diminishing")));

        assertEquals(Map.of(BattleItemDrops.Drop.MODS_1, 36, BattleItemDrops.Drop.MODS_2, 18), counts(plan(battle(source(12000f)), 1f)));
        assertEquals(Map.of(BattleItemDrops.Drop.MODS_1, 36, BattleItemDrops.Drop.MODS_2, 18),
                counts(plan(battle(source(6000f), source(6000f)), 1f)));
    }

    @Test
    void aForcedModifierCountRollsExactlyThatManyModifiersAndSurvivesTheCargoRoundTrip() {
        ItemDefinition fullBridge = ItemDefinitions.get(FULL_BRIDGE);
        for (int modifierCount = 1; modifierCount <= 4; modifierCount++) {
            for (long seed = 0; seed < 50; seed++) {
                ItemData rolled = ItemData.rolledWithModifierCount(fullBridge, seed, modifierCount);

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
        BattleItemDrops.Battle bigBattle = new BattleItemDrops.Battle(List.of(source(4000f)), "pirates");
        Random random = new Random(3L);
        int relics = 0;
        int lockedRelics = 0;
        for (int trial = 0; trial < 100; trial++) {
            HullItemDrops.SectionItemLoot loot = BattleItemDrops.roll(bigBattle, 1f, random, definition -> true, true);
            for (ItemData item : loot.items()) {
                relics += HULL_RELIC.equals(item.definitionId()) ? 1 : 0;
                assertFalse(THIN_BRIDGE.equals(item.definitionId()) && item.explicits().size() > 1, item.toString());
            }
            for (ItemData item : BattleItemDrops.roll(bigBattle, 1f, random, definition -> false, true).items()) {
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
        List<BattleItemDrops.SourceLoss> sources = List.of(source(4000f));

        Map<String, Integer> fromPirates = uniqueDrops(new BattleItemDrops.Battle(sources, "pirates"));
        Map<String, Integer> fromTriTachyon = uniqueDrops(new BattleItemDrops.Battle(sources, "tritachyon"));

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
            for (ItemData item : BattleItemDrops.roll(battle, 1f, random, definition -> true, true).items()) {
                if (ItemDefinitions.get(item.definitionId()).unique()) {
                    drops.merge(item.definitionId(), 1, Integer::sum);
                }
            }
        }
        return drops;
    }

    @Test
    void aBattleWithNoLossesOrADropMultiplierOfZeroDropsNothing() {
        BattleItemDrops.Battle battle = battle(source(4000f));

        assertEquals(HullItemDrops.SectionItemLoot.NONE, BattleItemDrops.roll(battle, 0f, new Random(1L), definition -> true, true));
        assertEquals(HullItemDrops.SectionItemLoot.NONE, BattleItemDrops.roll(new BattleItemDrops.Battle(List.of(), null), 1f,
                new Random(1L), definition -> true, true));
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

        assertNull(counts(plan(battle, 1f)).get(BattleItemDrops.Drop.MASTER_SCHEMATIC));

        MasterSchematic.requiredWhen(() -> true);
        assertEquals(1, counts(plan(battle, 1f)).get(BattleItemDrops.Drop.MASTER_SCHEMATIC));
    }

    @Test
    void beforeSectionsOpenABattleDropsMasterSchematicsAndNothingElse() {
        MasterSchematic.requiredWhen(() -> true);
        BattleItemDrops.Battle battle = battle(source(1000f));

        Map<BattleItemDrops.Drop, Integer> closed = counts(BattleItemDrops.plan(battle, 1f, List.of(), neverLucky(), false));

        assertEquals(Map.of(BattleItemDrops.Drop.MASTER_SCHEMATIC, 1), closed);
        HullItemDrops.SectionItemLoot loot = BattleItemDrops.roll(battle, 1f, neverLucky(),
                definition -> true, false);
        assertEquals(List.of(MasterSchematic.INSTANCE), loot.upgrades());
        assertTrue(loot.items().isEmpty());
    }

    private static Map<RefitSchematic, Integer> schematicTiersDropped(int battles) {
        BattleItemDrops.Battle battle = battle(source(2000f));
        Random random = new Random(5L);
        Map<RefitSchematic, Integer> tierCounts = new EnumMap<>(RefitSchematic.class);
        for (int trial = 0; trial < battles; trial++) {
            for (ShipSchematic upgrade : BattleItemDrops.roll(battle, 1f, random, definition -> true, true).upgrades()) {
                if (upgrade instanceof RefitSchematic schematic) {
                    tierCounts.merge(schematic, 1, Integer::sum);
                }
            }
        }
        return tierCounts;
    }
    @Test
    void rarityMultipliesOnlyTheUniqueRowOfABattle() {
        List<BattleItemDrops.Planned> plain = plan(battle(source(10000f)), 1f);
        List<BattleItemDrops.Planned> rarer = plan(battle(source(10000f, ItemChances.BASE.withRarity(2f))), 1f);

        assertEquals(counts(plain).get(BattleItemDrops.Drop.MODS_1), counts(rarer).get(BattleItemDrops.Drop.MODS_1));
        assertEquals(counts(plain).get(BattleItemDrops.Drop.MODS_2), counts(rarer).get(BattleItemDrops.Drop.MODS_2));
        assertEquals(counts(plain).get(BattleItemDrops.Drop.REFIT_SCHEMATIC), counts(rarer).get(BattleItemDrops.Drop.REFIT_SCHEMATIC));
        assertEquals(1, counts(plain).get(BattleItemDrops.Drop.UNIQUE));
        assertEquals(3, counts(rarer).get(BattleItemDrops.Drop.UNIQUE));
    }

    @Test
    void aDroppedHullItemRollsOneOrTwoModifiersWhateverTheSiteRarity() {
        ItemDefinition fullBridge = ItemDefinitions.get(FULL_BRIDGE);
        int singleModifier = 0;
        for (long seed = 0; seed < 4000; seed++) {
            ItemData dropped = ItemData.rolledWithRarity(fullBridge, seed, 1f);
            assertEquals(dropped, ItemData.rolledWithRarity(fullBridge, seed, 2f));
            assertTrue(dropped.explicits().size() == 1 || dropped.explicits().size() == 2, dropped.toString());
            singleModifier += dropped.explicits().size() == 1 ? 1 : 0;
        }

        assertEquals(0.64, singleModifier / 4000.0, 0.03);
    }
}
