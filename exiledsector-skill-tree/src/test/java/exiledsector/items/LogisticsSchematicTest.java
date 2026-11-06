package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.FleetLootRules;
import exiledsector.loot.LootCurve;
import exiledsector.skills.LogisticsNodes;
import exiledsector.skills.ShipSkillData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogisticsSchematicTest {

    private static CargoAPI cargoHolding(float count) {
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getQuantity(eq(CargoAPI.CargoItemType.SPECIAL), any())).thenReturn(count);
        return cargo;
    }

    @Test
    void usingOneTakesItFromCargoAndRaisesTheShipsCap() {
        CargoAPI cargo = cargoHolding(2f);
        ShipSkillData shipData = new ShipSkillData();

        assertTrue(LogisticsSchematic.useOn(cargo, shipData));

        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, LogisticsSchematic.INSTANCE.toSpecialItem(), 1f);
        assertEquals(1, shipData.getExtraLogisticsSlots());
        assertEquals(LogisticsNodes.DEFAULT_CAP + 1, LogisticsNodes.cap(shipData));
        assertFalse(shipData.isBlank());
    }

    @Test
    void withNoneInCargoNothingChanges() {
        CargoAPI cargo = cargoHolding(0f);
        ShipSkillData shipData = new ShipSkillData();

        assertFalse(LogisticsSchematic.useOn(cargo, shipData));
        assertFalse(LogisticsSchematic.useOn(null, shipData));

        verify(cargo, never()).removeItems(any(), any(), eq(1f));
        assertEquals(0, shipData.getExtraLogisticsSlots());
    }

    @Test
    void siteFindsMatchTheMilitaryRefitSchematicShare() {
        assertEquals(0.1f, LogisticsSchematicDrops.matchedTierShare(), 1e-6f);
        assertEquals(0.05f, LogisticsSchematicDrops.siteFindChance(0.5f, 1f), 1e-6f);
    }

    @Test
    void battleDropsScaleWithDeploymentPointsAndRegularFleetsGetTheirShare() {
        BattleLootRates.Rate rate = new BattleLootRates.Rate(LogisticsSchematicDrops.RATE_ID, 0.032f, LootCurve.LINEAR, 0.25f, 1f);
        List<LogisticsSchematicDrops.SourceLoss> regular = List.of(new LogisticsSchematicDrops.SourceLoss(1000f, 1f, FleetLootRules.REGULAR));
        List<LogisticsSchematicDrops.SourceLoss> chart = List.of(new LogisticsSchematicDrops.SourceLoss(1000f, 1f, FleetLootRules.full(null)));

        assertEquals(0.08f, LogisticsSchematicDrops.expectedBattleDrops(regular, rate, 1f), 1e-5f);
        assertEquals(0.32f, LogisticsSchematicDrops.expectedBattleDrops(chart, rate, 1f), 1e-5f);
        assertEquals(0f, LogisticsSchematicDrops.expectedBattleDrops(regular, null, 1f));
        assertEquals(0, LogisticsSchematicDrops.rollBattle(List.of(), 1f, new Random(1)));
    }

    @Test
    void theBattleItemDropSliderScalesBattleDrops() {
        BattleLootRates.Rate rate = new BattleLootRates.Rate(LogisticsSchematicDrops.RATE_ID, 0.032f, LootCurve.LINEAR, 0.25f, 1f);
        List<LogisticsSchematicDrops.SourceLoss> regular = List.of(new LogisticsSchematicDrops.SourceLoss(1000f, 1f, FleetLootRules.REGULAR));

        assertEquals(0.16f, LogisticsSchematicDrops.expectedBattleDrops(regular, rate, 2f), 1e-5f);
        assertEquals(0f, LogisticsSchematicDrops.expectedBattleDrops(regular, rate, 0f));
    }
}
