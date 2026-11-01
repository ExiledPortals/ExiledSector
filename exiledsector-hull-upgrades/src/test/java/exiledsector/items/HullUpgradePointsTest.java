package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.tags.ShipProfile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HullUpgradePointsTest {

    private final ItemStore store = new ItemStore();

    @AfterEach
    void tearDown() {
        HullUpgradesSetting.setPointsModeForTests(HullUpgradesSetting.PointsMode.REFIT_SCHEMATICS);
        ItemDrops.clear();
    }

    private static ShipProfile fit() {
        return new ShipProfile(HullSize.CRUISER, ShieldType.FRONT, 0, Set.of(), false, 1000f, false);
    }

    private static ShipSkillData shipWithStoredPoints(int storedPoints) {
        ShipSkillData shipData = new ShipSkillData();
        shipData.setStoredHullUpgradePoints(storedPoints);
        return shipData;
    }

    private static CargoStackAPI stack(SpecialItemData special, float size) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(special);
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    @Test
    void refitSchematicModeGivesEveryShipAtLeastOnePointAndOtherwiseItsStoredPoints() {
        assertEquals(1, HullUpgradePoints.of(new ShipSkillData()));
        assertEquals(1, HullUpgradePoints.of(shipWithStoredPoints(1)));
        assertEquals(3, HullUpgradePoints.of(shipWithStoredPoints(3)));
        assertEquals(5, HullUpgradePoints.of(shipWithStoredPoints(5)));
        assertEquals(5, HullUpgradePoints.maxPoints());
    }

    @Test
    void starChartProgressCountsTheTierFiveTenAndFifteenClears() {
        assertEquals(1, HullUpgradePoints.chartPoints(0));
        assertEquals(1, HullUpgradePoints.chartPoints(4));
        assertEquals(2, HullUpgradePoints.chartPoints(5));
        assertEquals(2, HullUpgradePoints.chartPoints(9));
        assertEquals(3, HullUpgradePoints.chartPoints(10));
        assertEquals(4, HullUpgradePoints.chartPoints(15));
        assertEquals(5, HullUpgradePoints.nextChartTier(0));
        assertEquals(10, HullUpgradePoints.nextChartTier(5));
        assertEquals(15, HullUpgradePoints.nextChartTier(14));
        assertEquals(HullUpgradePoints.NO_NEXT_CHART_TIER, HullUpgradePoints.nextChartTier(15));
    }

    @Test
    void starChartProgressIgnoresStoredPointsAndKeepsThemForSwitchingBack() {
        ShipSkillData shipData = shipWithStoredPoints(4);
        HullUpgradesSetting.setPointsModeForTests(HullUpgradesSetting.PointsMode.STAR_CHART_PROGRESS);

        assertFalse(HullUpgradePoints.chartsEnabled());
        assertEquals(1, HullUpgradePoints.of(shipData));
        assertEquals(4, HullUpgradePoints.maxPoints());

        HullUpgradesSetting.setPointsModeForTests(HullUpgradesSetting.PointsMode.REFIT_SCHEMATICS);
        assertEquals(4, HullUpgradePoints.of(shipData));
    }

    @Test
    void anNpcBuildHasNoPoints() {
        ShipSkillData npcShip = new ShipSkillData();
        npcShip.markNpcBuild();

        assertEquals(0, HullUpgradePoints.of(npcShip));
        assertEquals(0, HullUpgradePoints.unspent(npcShip));
        assertEquals(HullUpgradeRules.UpgradeBlock.NOT_UPGRADABLE, HullUpgradeRules.upgradeBlock(npcShip, RefitSchematic.SCRAPPED, 1));
        assertEquals(HullUpgradeRules.UpgradeBlock.NOT_UPGRADABLE, HullUpgradeRules.upgradeBlock(npcShip, RefitSchematic.DOMAIN, 1));
    }

    @Test
    void eachSchematicTakesAnyShipBelowItsTierStraightToItAndIsUsedUp() {
        for (RefitSchematic schematic : RefitSchematic.values()) {
            for (int startPoints = HullUpgradePoints.STARTING_POINTS; startPoints < schematic.points(); startPoints++) {
                String scenario = schematic.name() + " on " + startPoints;
                store.addSchematics(schematic, 1);
                ShipSkillData shipData = shipWithStoredPoints(startPoints);

                assertNull(HullUpgradeRules.upgradeBlock(shipData, schematic, store.schematicCount(schematic)), scenario);
                assertTrue(HullUpgradeRules.applySchematic(shipData, schematic, store), scenario);
                assertEquals(schematic.points(), HullUpgradePoints.of(shipData), scenario);
                assertEquals(0, store.schematicCount(schematic), scenario);
            }
        }
    }

    @Test
    void aSchematicSkipsTheTiersBetweenAndLowerTiersStopWorkingAfterwards() {
        for (RefitSchematic schematic : RefitSchematic.values()) {
            store.addSchematics(schematic, 1);
        }
        ShipSkillData shipData = new ShipSkillData();

        assertTrue(HullUpgradeRules.applySchematic(shipData, RefitSchematic.MILITARY, store));
        assertEquals(4, HullUpgradePoints.of(shipData));
        assertEquals(HullUpgradeRules.UpgradeBlock.AT_OR_ABOVE_TIER, HullUpgradeRules.upgradeBlock(shipData, RefitSchematic.SCRAPPED, 1));
        assertEquals(HullUpgradeRules.UpgradeBlock.AT_OR_ABOVE_TIER, HullUpgradeRules.upgradeBlock(shipData, RefitSchematic.STANDARD, 1));
        assertTrue(HullUpgradeRules.applySchematic(shipData, RefitSchematic.DOMAIN, store));

        assertEquals(5, shipData.getStoredHullUpgradePoints());
        assertEquals(1, store.schematicCount(RefitSchematic.SCRAPPED));
        assertEquals(1, store.schematicCount(RefitSchematic.STANDARD));
        assertEquals(0, store.schematicCount(RefitSchematic.MILITARY));
        assertEquals(0, store.schematicCount(RefitSchematic.DOMAIN));
    }

    @Test
    void aSchematicIsRefusedOnAShipAtOrAboveItsTierAndIsKept() {
        for (RefitSchematic schematic : RefitSchematic.values()) {
            store.addSchematics(schematic, 1);
        }
        for (RefitSchematic schematic : RefitSchematic.values()) {
            for (int startPoints = schematic.points(); startPoints < HullUpgradePoints.MOST_POINTS; startPoints++) {
                String scenario = schematic.name() + " on " + startPoints;
                ShipSkillData shipData = shipWithStoredPoints(startPoints);

                assertEquals(HullUpgradeRules.UpgradeBlock.AT_OR_ABOVE_TIER, HullUpgradeRules.upgradeBlock(shipData, schematic, 1), scenario);
                assertFalse(HullUpgradeRules.applySchematic(shipData, schematic, store), scenario);
                assertEquals(startPoints, HullUpgradePoints.of(shipData), scenario);
            }
            ShipSkillData fullShip = shipWithStoredPoints(HullUpgradePoints.MOST_POINTS);
            assertEquals(HullUpgradeRules.UpgradeBlock.AT_MAX_POINTS, HullUpgradeRules.upgradeBlock(fullShip, schematic, 1), schematic.name());
            assertFalse(HullUpgradeRules.applySchematic(fullShip, schematic, store), schematic.name());
        }
        assertEquals(HullUpgradeRules.UpgradeBlock.NONE_IN_STORAGE, HullUpgradeRules.upgradeBlock(new ShipSkillData(), RefitSchematic.SCRAPPED, 0));
        for (RefitSchematic schematic : RefitSchematic.values()) {
            assertEquals(1, store.schematicCount(schematic), schematic.name());
        }
    }

    @Test
    void noSchematicAppliesWhileStarChartProgressIsOn() {
        store.addSchematics(RefitSchematic.SCRAPPED, 1);
        ShipSkillData shipData = new ShipSkillData();
        HullUpgradesSetting.setPointsModeForTests(HullUpgradesSetting.PointsMode.STAR_CHART_PROGRESS);

        assertEquals(HullUpgradeRules.UpgradeBlock.STAR_CHART_PROGRESS, HullUpgradeRules.upgradeBlock(shipData, RefitSchematic.SCRAPPED, 1));
        assertFalse(HullUpgradeRules.applySchematic(shipData, RefitSchematic.SCRAPPED, store));
        assertEquals(1, store.schematicCount(RefitSchematic.SCRAPPED));
        assertEquals(0, shipData.getStoredHullUpgradePoints());
    }

    @Test
    void noSchematicDropsFromSalvageOrTechMiningWhileStarChartProgressIsOn() throws Exception {
        ItemDrops.register(new JSONArray().put(new JSONObject().put("site", "station").put("chances", "1").put("uniqueChance", "0")));
        HullUpgradePoints.install();
        Random random = new Random(7L);

        assertEquals(1, HullItemDrops.rollSectionItemLoot("station", random, 1f).upgrades().size());
        HullUpgradesSetting.setPointsModeForTests(HullUpgradesSetting.PointsMode.STAR_CHART_PROGRESS);
        for (int trial = 0; trial < 20; trial++) {
            assertEquals(List.of(), HullItemDrops.rollSectionItemLoot("station", random, 1f).upgrades());
        }
    }

    @Test
    void switchingModesNeverLocksASectionButAnOverBudgetShipCannotUnlockMore() {
        ShipSkillData shipData = shipWithStoredPoints(3);
        assertTrue(HullUpgradeRules.unlock(shipData, ShipSection.BRIDGE, fit()));
        assertTrue(HullUpgradeRules.unlock(shipData, ShipSection.REACTOR, fit()));
        assertTrue(HullUpgradeRules.unlock(shipData, ShipSection.ENGINE_ROOM, fit()));

        HullUpgradesSetting.setPointsModeForTests(HullUpgradesSetting.PointsMode.STAR_CHART_PROGRESS);

        assertEquals(List.of("bridge", "reactor", "engine_room"), shipData.getUnlockedSocketTypeIds());
        assertEquals(0, HullUpgradePoints.unspent(shipData));
        assertEquals(HullUpgradeRules.UnlockBlock.NO_POINTS, HullUpgradeRules.unlockBlock(shipData, ShipSection.ARMOR_PLATING, fit()));
        assertFalse(HullUpgradeRules.unlock(shipData, ShipSection.ARMOR_PLATING, fit()));
        assertTrue(HullUpgradeRules.lock(shipData, ShipSection.REACTOR));
        assertTrue(HullUpgradeRules.lock(shipData, ShipSection.ENGINE_ROOM));
        assertEquals(HullUpgradeRules.UnlockBlock.NO_POINTS, HullUpgradeRules.unlockBlock(shipData, ShipSection.ARMOR_PLATING, fit()));
        assertTrue(HullUpgradeRules.lock(shipData, ShipSection.BRIDGE));
        assertTrue(HullUpgradeRules.unlock(shipData, ShipSection.ARMOR_PLATING, fit()));
    }

    @Test
    void theMigrationKeepsShipPointsAndTurnsEachOldUpgradeIntoOneStandardSchematicOnce() {
        ShipSkillData oldShip = shipWithStoredPoints(3);
        store.addLegacyHullUpgrades("CRUISER", 2);
        store.addLegacyHullUpgrades("FRIGATE", 1);
        store.addSchematics(RefitSchematic.MILITARY, 1);
        CargoStackAPI legacyStack = stack(new SpecialItemData(RefitSchematic.ITEM_ID, "DESTROYER"), 2f);
        CargoStackAPI currentStack = stack(RefitSchematic.SCRAPPED.toSpecialItem(), 1f);
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>(List.of(legacyStack, currentStack)));
        List<CargoAPI> cargoes = new ArrayList<>();
        cargoes.add(cargo);
        cargoes.add(null);

        HullUpgradeMigration.run(store, cargoes);

        assertEquals(3, HullUpgradePoints.of(oldShip));
        assertEquals(Map.of(RefitSchematic.STANDARD, 3, RefitSchematic.MILITARY, 1), store.schematicCounts());
        verify(cargo).removeStack(legacyStack);
        verify(cargo, never()).removeStack(currentStack);
        verify(cargo).addSpecial(RefitSchematic.STANDARD.toSpecialItem(), 2f);

        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>(List.of(currentStack)));
        HullUpgradeMigration.run(store, cargoes);
        assertEquals(Map.of(RefitSchematic.STANDARD, 3, RefitSchematic.MILITARY, 1), store.schematicCounts());
        assertEquals(0, store.convertLegacyHullUpgrades());
    }

    @Test
    void theSettingReadsBothModesAndFallsBackToRefitSchematics() {
        assertEquals(HullUpgradesSetting.PointsMode.STAR_CHART_PROGRESS, HullUpgradesSetting.parsePointsMode(HullUpgradesSetting.STAR_CHART_PROGRESS));
        assertEquals(HullUpgradesSetting.PointsMode.REFIT_SCHEMATICS, HullUpgradesSetting.parsePointsMode(HullUpgradesSetting.REFIT_SCHEMATICS));
        assertEquals(HullUpgradesSetting.PointsMode.REFIT_SCHEMATICS, HullUpgradesSetting.parsePointsMode(null));
        assertEquals(HullUpgradesSetting.PointsMode.REFIT_SCHEMATICS, HullUpgradesSetting.parsePointsMode("something else"));
    }

    @Test
    void aRaisedChartClearOrAModeSwitchMovesThePointsRevision() {
        int startRevision = HullUpgradePoints.revision();
        HullUpgradePoints.chartTierRaised(null, 4, 5);
        int afterClear = HullUpgradePoints.revision();
        HullUpgradesSetting.setPointsModeForTests(HullUpgradesSetting.PointsMode.STAR_CHART_PROGRESS);

        assertNotEquals(startRevision, afterClear);
        assertNotEquals(afterClear, HullUpgradePoints.revision());
    }

    @Test
    void schematicsRollTheirTierByDropWeight() {
        Random random = new Random(11L);
        int[] tierCounts = new int[RefitSchematic.values().length];
        int rolls = 20000;
        for (int i = 0; i < rolls; i++) {
            tierCounts[RefitSchematic.roll(random).ordinal()]++;
        }

        assertEquals(0.60, tierCounts[RefitSchematic.SCRAPPED.ordinal()] / (double) rolls, 0.015);
        assertEquals(0.28, tierCounts[RefitSchematic.STANDARD.ordinal()] / (double) rolls, 0.015);
        assertEquals(0.10, tierCounts[RefitSchematic.MILITARY.ordinal()] / (double) rolls, 0.01);
        assertEquals(0.02, tierCounts[RefitSchematic.DOMAIN.ordinal()] / (double) rolls, 0.005);
    }
}
