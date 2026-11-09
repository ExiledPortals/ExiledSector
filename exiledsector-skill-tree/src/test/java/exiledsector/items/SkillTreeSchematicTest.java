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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillTreeSchematicTest {

    private static CargoAPI cargoHolding(float count) {
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getQuantity(eq(CargoAPI.CargoItemType.SPECIAL), any())).thenReturn(count);
        return cargo;
    }

    private static ShipSkillData withIntegrations(String... nodeIds) {
        ShipSkillData shipData = new ShipSkillData();
        for (String nodeId : nodeIds) {
            shipData.integrate(nodeId);
        }
        return shipData;
    }

    @Test
    void usingALogisticsSchematicTakesItFromCargoAndRaisesTheShipsCap() {
        CargoAPI cargo = cargoHolding(2f);
        ShipSkillData shipData = new ShipSkillData();

        assertTrue(LogisticsSchematic.useOn(cargo, shipData));

        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, LogisticsSchematic.INSTANCE.toSpecialItem(), 1f);
        assertEquals(1, shipData.getExtraLogisticsSlots());
        assertEquals(LogisticsNodes.DEFAULT_CAP + 1, LogisticsNodes.cap(shipData));
        assertFalse(shipData.isBlank());
    }

    @Test
    void withNoLogisticsSchematicInCargoNothingChanges() {
        CargoAPI cargo = cargoHolding(0f);
        ShipSkillData shipData = new ShipSkillData();

        assertFalse(LogisticsSchematic.useOn(cargo, shipData));
        assertFalse(LogisticsSchematic.useOn(null, shipData));

        verify(cargo, never()).removeItems(any(), any(), eq(1f));
        assertEquals(0, shipData.getExtraLogisticsSlots());
    }

    @Test
    void aDeintegrationSchematicRemovesEveryIntegrationAndRefundsWhatTheyCost() {
        CargoAPI cargo = cargoHolding(1f);
        ShipSkillData shipData = withIntegrations("a_1", "b_1", "c_1");
        assertEquals(6, DeintegrationSchematic.refundFor(shipData));

        assertEquals(6, DeintegrationSchematic.useOn(cargo, shipData));

        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, DeintegrationSchematic.INSTANCE.toSpecialItem(), 1f);
        assertEquals(Set.of(), shipData.getIntegratedNodeIds());
        assertEquals(0, DeintegrationSchematic.refundFor(shipData));
    }

    @Test
    void aDeintegrationSchematicIsKeptWhenTheShipHasNoIntegrationsOrNoneIsHeld() {
        CargoAPI emptyCargo = cargoHolding(0f);
        CargoAPI cargo = cargoHolding(1f);
        ShipSkillData integrated = withIntegrations("a_1");

        assertEquals(0, DeintegrationSchematic.useOn(cargo, new ShipSkillData()));
        assertEquals(0, DeintegrationSchematic.useOn(emptyCargo, integrated));
        assertEquals(0, DeintegrationSchematic.useOn(null, integrated));

        verify(cargo, never()).removeItems(any(), any(), eq(1f));
        assertEquals(Set.of("a_1"), integrated.getIntegratedNodeIds());
    }

    @Test
    void siteFindsMatchTheMilitaryRefitSchematicShare() {
        assertEquals(0.1f, SkillTreeSchematicDrops.matchedTierShare(), 1e-6f);
        assertEquals(0.05f, SkillTreeSchematicDrops.siteFindChance(0.5f, 1f), 1e-6f);
    }

    @Test
    void battleDropsScaleWithDeploymentPointsAndRegularFleetsGetTheirShare() {
        BattleLootRates.Rate rate = new BattleLootRates.Rate(SkillTreeSchematicDrops.LOGISTICS.rateId(), 0.032f, LootCurve.LINEAR, 0.25f, 1f);
        List<SkillTreeSchematicDrops.SourceLoss> regular = List.of(new SkillTreeSchematicDrops.SourceLoss(1000f, 1f, FleetLootRules.REGULAR));
        List<SkillTreeSchematicDrops.SourceLoss> chart = List.of(new SkillTreeSchematicDrops.SourceLoss(1000f, 1f, FleetLootRules.full(null)));

        assertEquals(0.08f, SkillTreeSchematicDrops.expectedBattleDrops(regular, rate, 1f), 1e-5f);
        assertEquals(0.32f, SkillTreeSchematicDrops.expectedBattleDrops(chart, rate, 1f), 1e-5f);
        assertEquals(0f, SkillTreeSchematicDrops.expectedBattleDrops(regular, null, 1f));
        for (SkillTreeSchematicDrops drop : SkillTreeSchematicDrops.values()) {
            assertEquals(0, drop.rollBattle(List.of(), 1f, new Random(1)));
        }
    }

    @Test
    void theBattleItemDropSliderScalesBattleDrops() {
        BattleLootRates.Rate rate = new BattleLootRates.Rate(SkillTreeSchematicDrops.DEINTEGRATION.rateId(), 0.032f, LootCurve.LINEAR, 0.25f, 1f);
        List<SkillTreeSchematicDrops.SourceLoss> regular = List.of(new SkillTreeSchematicDrops.SourceLoss(1000f, 1f, FleetLootRules.REGULAR));

        assertEquals(0.16f, SkillTreeSchematicDrops.expectedBattleDrops(regular, rate, 2f), 1e-5f);
        assertEquals(0f, SkillTreeSchematicDrops.expectedBattleDrops(regular, rate, 0f));
    }
}
