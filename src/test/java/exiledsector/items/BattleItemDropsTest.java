package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.ShipHullSizes;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static exiledsector.items.ItemFixtures.MILITARY_PREFIXES;
import static exiledsector.items.ItemFixtures.MILITARY_SUFFIXES;
import static exiledsector.items.ItemFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleItemDropsTest {

    private static final String FULL_BRIDGE = "bridge_full";
    private static final String THIN_BRIDGE = "bridge_thin";
    private static final String FULL_REACTOR = "reactor_full";
    private static final String HULL_RELIC = "hull_relic";

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(row(FULL_BRIDGE, "bridge", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row(THIN_BRIDGE, "bridge", "HULL_PERCENT:4:6"))
                .put(row(FULL_REACTOR, "reactor", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row(HULL_RELIC, "weapon_mount", "HULL_PERCENT:4:6").put("unique", "true")));
        BattleItemDrops.register(new JSONArray()
                .put(rateRow("mods_1", "0.4"))
                .put(rateRow("mods_2", "0.25"))
                .put(rateRow("mods_3", "0.15"))
                .put(rateRow("mods_4", "0.04"))
                .put(rateRow("unique", "0.015"))
                .put(rateRow("hull_upgrade", "0.1")));
    }

    @AfterEach
    void tearDown() {
        BattleItemDrops.clear();
        ItemDefinitions.clear();
    }

    private static JSONObject rateRow(String drop, String rate) throws Exception {
        return new JSONObject().put("drop", drop).put("per_100_power", rate);
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
    void aShipCountsDoubleAtThirtyNodesAndTripleAtSixty() {
        assertEquals(100f, BattleItemDrops.shipPower(100f, 0), 1e-4f);
        assertEquals(200f, BattleItemDrops.shipPower(100f, 30), 1e-4f);
        assertEquals(300f, BattleItemDrops.shipPower(100f, 60), 1e-4f);
        assertEquals(0f, BattleItemDrops.shipPower(-5f, 10), 1e-4f);
    }

    @Test
    void theTableSkipsUnknownDropsAndBadRates() throws Exception {
        BattleItemDrops.register(new JSONArray()
                .put(rateRow("mods_1", "0.5"))
                .put(rateRow("mods_9", "1"))
                .put(rateRow("mods_2", "-1"))
                .put(rateRow("unique", "lots")));

        assertEquals(0.5f, BattleItemDrops.rate(BattleItemDrops.Drop.MODS_1), 1e-6f);
        assertEquals(0f, BattleItemDrops.rate(BattleItemDrops.Drop.MODS_2), 1e-6f);
        assertEquals(0f, BattleItemDrops.rate(BattleItemDrops.Drop.UNIQUE), 1e-6f);
    }

    @Test
    void eachRowExpectsPowerOverAHundredTimesItsRateAndKeepsTheWholePart() {
        List<BattleItemDrops.Planned> planned = BattleItemDrops.plan(List.of(new BattleItemDrops.SourcePower(1000f, ItemChances.BASE)),
                1f, List.of(), neverLucky());

        assertEquals(Map.of(BattleItemDrops.Drop.MODS_1, 4, BattleItemDrops.Drop.MODS_2, 2, BattleItemDrops.Drop.MODS_3, 1,
                BattleItemDrops.Drop.HULL_UPGRADE, 1), counts(planned));
    }

    @Test
    void severalSourcesAddUpAndTheGeneralChanceAndSettingScaleEveryRow() {
        List<BattleItemDrops.SourcePower> twoFleets = List.of(new BattleItemDrops.SourcePower(400f, ItemChances.of(2f)),
                new BattleItemDrops.SourcePower(200f, ItemChances.BASE));

        List<BattleItemDrops.Planned> planned = BattleItemDrops.plan(twoFleets, 1f, List.of(), neverLucky());
        List<BattleItemDrops.Planned> halved = BattleItemDrops.plan(twoFleets, 0.5f, List.of(), neverLucky());

        assertEquals(4, counts(planned).get(BattleItemDrops.Drop.MODS_1));
        assertEquals(2, counts(halved).get(BattleItemDrops.Drop.MODS_1));
    }

    @Test
    void aTargetedChanceAddsItemsOfThatSectionOnTopOfTheGeneralRoll() {
        ItemChances bridgeBonus = new ItemChances(1f, Map.of(ItemKind.BRIDGE, 1f));
        List<BattleItemDrops.Planned> planned = BattleItemDrops.plan(List.of(new BattleItemDrops.SourcePower(2000f, bridgeBonus)), 1f,
                List.of(ItemKind.BRIDGE, ItemKind.REACTOR), neverLucky());

        long targeted = planned.stream().filter(entry -> entry.kind() != null).count();
        assertTrue(planned.stream().filter(entry -> entry.kind() != null).allMatch(entry -> entry.kind() == ItemKind.BRIDGE));
        assertEquals(4 + 2 + 1, targeted);
        assertEquals(8, counts(planned.stream().filter(entry -> entry.kind() == null).toList()).get(BattleItemDrops.Drop.MODS_1));
    }

    @Test
    void aBigBattleKeepsEveryPlannedDropWithNoCap() {
        List<BattleItemDrops.Planned> planned = BattleItemDrops.plan(List.of(new BattleItemDrops.SourcePower(10000f, ItemChances.BASE)),
                1f, List.of(), neverLucky());

        assertEquals(Map.of(BattleItemDrops.Drop.MODS_1, 40, BattleItemDrops.Drop.MODS_2, 25, BattleItemDrops.Drop.MODS_3, 15,
                BattleItemDrops.Drop.MODS_4, 4, BattleItemDrops.Drop.UNIQUE, 1, BattleItemDrops.Drop.HULL_UPGRADE, 10), counts(planned));
        assertEquals(95, planned.size());
    }

    @Test
    void aForcedEffectCountRollsExactlyThatManyModifiersAndSurvivesTheCargoRoundTrip() {
        ItemDefinition fullBridge = ItemDefinitions.get(FULL_BRIDGE);
        for (int effectCount = 1; effectCount <= 4; effectCount++) {
            for (long seed = 0; seed < 50; seed++) {
                ItemData rolled = ItemData.rolledWithEffectCount(fullBridge, seed, effectCount);

                assertEquals(effectCount, rolled.explicits().size());
                assertEquals(rolled.explicits(), ItemData.of(rolled.toSpecialItem()).explicits());
            }
        }
        assertTrue(ItemRoller.canRollEffectCount(ItemDefinitions.get(THIN_BRIDGE), 1));
        assertFalse(ItemRoller.canRollEffectCount(ItemDefinitions.get(THIN_BRIDGE), 2));
        assertFalse(ItemRoller.canRollEffectCount(fullBridge, 5));
        assertFalse(ItemRoller.canRollEffectCount(ItemDefinitions.get(HULL_RELIC), 1));
    }

    @Test
    void aBattleNeverRollsAnItemThatCannotHoldItsModifiersAndUniquesNeedTheirUnlock() {
        BattleItemDrops.Battle bigBattle = new BattleItemDrops.Battle(List.of(new BattleItemDrops.SourcePower(4000f, ItemChances.BASE)),
                "pirates");
        Random random = new Random(3L);
        int relics = 0;
        int lockedRelics = 0;
        for (int trial = 0; trial < 100; trial++) {
            BattleItemDrops.Loot loot = BattleItemDrops.roll(bigBattle, 1f, random, definition -> true);
            for (ItemData item : loot.items()) {
                relics += HULL_RELIC.equals(item.definitionId()) ? 1 : 0;
                assertFalse(THIN_BRIDGE.equals(item.definitionId()) && item.explicits().size() > 1, item.toString());
            }
            for (ItemData item : BattleItemDrops.roll(bigBattle, 1f, random, definition -> false).items()) {
                lockedRelics += HULL_RELIC.equals(item.definitionId()) ? 1 : 0;
            }
        }

        assertTrue(relics > 0);
        assertEquals(0, lockedRelics);
    }

    @Test
    void aBattleWithNoPowerOrADropMultiplierOfZeroDropsNothing() {
        BattleItemDrops.Battle battle = new BattleItemDrops.Battle(List.of(new BattleItemDrops.SourcePower(4000f, ItemChances.BASE)),
                null);

        assertEquals(BattleItemDrops.Loot.NONE, BattleItemDrops.roll(battle, 0f, new Random(1L), definition -> true));
        assertEquals(BattleItemDrops.Loot.NONE, BattleItemDrops.roll(new BattleItemDrops.Battle(List.of(), null), 1f,
                new Random(1L), definition -> true));
    }

    @Test
    void battleHullUpgradesComeInEveryShipHullSize() {
        BattleItemDrops.Battle battle = new BattleItemDrops.Battle(List.of(new BattleItemDrops.SourcePower(4000f, ItemChances.BASE)),
                "pirates");
        Random random = new Random(5L);
        Set<HullSize> sizes = EnumSet.noneOf(HullSize.class);
        for (int trial = 0; trial < 100; trial++) {
            BattleItemDrops.roll(battle, 1f, random, definition -> true).upgrades().forEach(upgrade -> sizes.add(upgrade.hullSize()));
        }

        assertEquals(EnumSet.copyOf(ShipHullSizes.ORDER), sizes);
    }

    @Test
    void rarityMultipliesOnlyTheRareRowsOfABattle() {
        List<BattleItemDrops.Planned> plain = BattleItemDrops.plan(List.of(new BattleItemDrops.SourcePower(1600f, ItemChances.BASE)),
                1f, List.of(), neverLucky());
        List<BattleItemDrops.Planned> rarer = BattleItemDrops.plan(List.of(new BattleItemDrops.SourcePower(1600f,
                ItemChances.BASE.withRarity(2f))), 1f, List.of(), neverLucky());

        assertEquals(counts(plain).get(BattleItemDrops.Drop.MODS_1), counts(rarer).get(BattleItemDrops.Drop.MODS_1));
        assertEquals(counts(plain).get(BattleItemDrops.Drop.HULL_UPGRADE), counts(rarer).get(BattleItemDrops.Drop.HULL_UPGRADE));
        assertEquals(2, counts(plain).get(BattleItemDrops.Drop.MODS_3));
        assertEquals(4, counts(rarer).get(BattleItemDrops.Drop.MODS_3));
        assertNull(counts(plain).get(BattleItemDrops.Drop.MODS_4));
        assertEquals(1, counts(rarer).get(BattleItemDrops.Drop.MODS_4));
    }

    @Test
    void siteRarityShiftsTheModifierCountTowardsRareButLeavesTheUsualRollAlone() {
        ItemDefinition fullBridge = ItemDefinitions.get(FULL_BRIDGE);
        int rareAtBase = 0;
        int rareWithRarity = 0;
        for (long seed = 0; seed < 4000; seed++) {
            assertEquals(ItemData.rolled(fullBridge, seed), ItemData.rolledWithRarity(fullBridge, seed, 1f));
            rareAtBase += ItemData.rolledWithRarity(fullBridge, seed, 1f).explicits().size() > 2 ? 1 : 0;
            rareWithRarity += ItemData.rolledWithRarity(fullBridge, seed, 2f).explicits().size() > 2 ? 1 : 0;
        }

        assertEquals(0.45, rareAtBase / 4000.0, 0.03);
        assertEquals(0.9 / 1.45, rareWithRarity / 4000.0, 0.03);
    }
}
