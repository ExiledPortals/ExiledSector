package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.MockSector;
import exiledsector.charts.MapMemory;
import exiledsector.charts.anomaly.LiveAnomalyRuns;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExcavationDeliveriesTest {

    private static FleetMemberAPI member(HullSize size, boolean civilian) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getHullSize()).thenReturn(size);
        when(member.getHullSpec()).thenReturn(hull);
        when(member.isCivilian()).thenReturn(civilian);
        return member;
    }

    private static SectorEntityToken planetWith(MemoryAPI memory) {
        SectorEntityToken planet = mock(SectorEntityToken.class);
        when(planet.getMemoryWithoutUpdate()).thenReturn(memory);
        return planet;
    }

    @Test
    void onlyTheConvoysWarshipFrigatesAndDestroyersGoDownTheShaft() {
        FleetMemberAPI flagship = member(HullSize.DESTROYER, false);
        FleetMemberAPI freighter = member(HullSize.FRIGATE, true);
        FleetMemberAPI frigate = member(HullSize.FRIGATE, false);
        FleetMemberAPI destroyer = member(HullSize.DESTROYER, false);
        FleetMemberAPI cruiser = member(HullSize.CRUISER, false);
        CampaignFleetAPI convoyFleet = mock(CampaignFleetAPI.class);
        FleetDataAPI convoyData = mock(FleetDataAPI.class);
        when(convoyFleet.getFleetData()).thenReturn(convoyData);
        when(convoyFleet.getFlagship()).thenReturn(flagship);
        when(convoyData.getMembersListCopy()).thenReturn(List.of(flagship, freighter, frigate, destroyer, cruiser));
        CampaignFleetAPI guardians = mock(CampaignFleetAPI.class);
        FleetDataAPI guardianData = mock(FleetDataAPI.class);
        when(guardians.getFleetData()).thenReturn(guardianData);

        assertEquals(List.of(frigate, destroyer), ExcavationDeliveries.joinShaftShips(convoyFleet, guardians, ExcavationRules.MAX_SHIP_SIZE));

        verify(convoyData).removeFleetMember(frigate);
        verify(guardianData).addFleetMember(destroyer);
        verify(guardianData, never()).addFleetMember(flagship);
        verify(guardianData, never()).addFleetMember(freighter);
        verify(guardianData, never()).addFleetMember(cruiser);
        verify(guardianData).sort();
        assertEquals(List.of(), ExcavationDeliveries.joinShaftShips(null, guardians, ExcavationRules.MAX_SHIP_SIZE));
    }

    @Test
    void aDeliveredSiteReadsItsRaidDifficultyLootBonusAndStoredCacheFromThePlanet() {
        MemoryAPI memory = mock(MemoryAPI.class);
        CargoAPI cache = mock(CargoAPI.class);
        when(memory.get(ExcavationDeliveries.RAID_DIFFICULTY_KEY)).thenReturn(100f);
        when(memory.get(ExcavationDeliveries.LOOT_KEY)).thenReturn(30f);
        when(memory.get(ExcavationDeliveries.CACHE_KEY)).thenReturn(cache);
        SectorEntityToken planet = planetWith(memory);

        assertEquals(2f, ExcavationDeliveries.raidDifficultyMult(planet), 0.001f);
        assertEquals(30f, ExcavationDeliveries.lootPercent(planet), 0.001f);
        assertSame(cache, ExcavationDeliveries.takeCache(planet));
        verify(memory).unset(ExcavationDeliveries.CACHE_KEY);
    }

    @Test
    void aConvoyDeliveryAddsToTheSitesInhibitorQuantity() {
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_EXCAVATION_LOOT_QUANTITY_PERCENT, 20f)));

        assertEquals(15f, ExcavationLoot.expectedInhibitors(10f, context, 30f, 1f), 0.001f);
        assertEquals(13f, ExcavationLoot.expectedInhibitors(10f, ChartGenContext.none(), 30f, 1f), 0.001f);
        assertEquals(ExcavationLoot.expectedInhibitors(10f, context, 1f), ExcavationLoot.expectedInhibitors(10f, context, 0f, 1f), 0.001f);
    }

    @Test
    void anUntouchedSiteKeepsItsUsualRaid() {
        SectorEntityToken planet = planetWith(mock(MemoryAPI.class));

        assertEquals(1f, ExcavationDeliveries.raidDifficultyMult(planet), 0.001f);
        assertEquals(1f, ExcavationDeliveries.raidDifficultyMult(null), 0.001f);
        assertEquals(0f, ExcavationDeliveries.lootPercent(planet), 0.001f);
        assertNull(ExcavationDeliveries.takeCache(planet));
        assertEquals(List.of(), ExcavationDeliveries.standingPlanetIds(null));
    }

    @Test
    void aFrigatesOnlyShaftTakesOnlyTheConvoysFrigates() {
        FleetMemberAPI flagship = member(HullSize.FRIGATE, false);
        FleetMemberAPI frigate = member(HullSize.FRIGATE, false);
        FleetMemberAPI destroyer = member(HullSize.DESTROYER, false);
        CampaignFleetAPI convoyFleet = mock(CampaignFleetAPI.class);
        FleetDataAPI convoyData = mock(FleetDataAPI.class);
        when(convoyFleet.getFleetData()).thenReturn(convoyData);
        when(convoyFleet.getFlagship()).thenReturn(flagship);
        when(convoyData.getMembersListCopy()).thenReturn(List.of(flagship, frigate, destroyer));
        CampaignFleetAPI guardians = mock(CampaignFleetAPI.class);
        when(guardians.getFleetData()).thenReturn(mock(FleetDataAPI.class));

        assertEquals(List.of(frigate), ExcavationDeliveries.joinShaftShips(convoyFleet, guardians, ExcavationRules.FRIGATES_ONLY_SHIP_SIZE));
    }

    @Test
    void aTwinShaftSiteGivesTheOpenShaftsGuardiansTheLootLineAndHardensTheCollapsedShaftsRaid() {
        try (MockSector ignored = new MockSector()) {
            ChartRun run = LiveAnomalyRuns.run(4, ExcavationSites.ANOMALY_ID);
            ExcavationStates.record(ExcavationStates.of(run), new ExcavationSite(0, "rock", ExcavationSite.Access.BOTH, 1L, false));
            MemoryAPI planetMemory = MapMemory.create();
            SectorEntityToken planet = planetWith(planetMemory);
            when(planet.getId()).thenReturn("rock");
            when(planet.getLocation()).thenReturn(new Vector2f());
            CampaignFleetAPI guardians = mock(CampaignFleetAPI.class);
            MemoryAPI guardianMemory = MapMemory.create();
            when(guardians.getMemoryWithoutUpdate()).thenReturn(guardianMemory);
            planetMemory.set(ExcavationFleets.SITE_FLEET_KEY, guardians);

            assertTrue(ExcavationDeliveries.accepts(run, planet));
            ExcavationDeliveries.deliverCache(run, planet, null, 100f, 30f);

            assertEquals(30f, ExcavationDeliveries.lootPercent(guardians), 0.001f);
            assertEquals(30f, ExcavationDeliveries.lootPercent(planet), 0.001f);
            assertEquals(2f, ExcavationDeliveries.raidDifficultyMult(planet), 0.001f);
        }
    }

    @Test
    void anOpenShaftDeliveryLeavesNoRaidToHarden() {
        try (MockSector ignored = new MockSector()) {
            ChartRun run = LiveAnomalyRuns.run(4, ExcavationSites.ANOMALY_ID);
            ExcavationStates.record(ExcavationStates.of(run), new ExcavationSite(0, "rock", ExcavationSite.Access.OPEN, 1L, false));
            MemoryAPI planetMemory = MapMemory.create();
            SectorEntityToken planet = planetWith(planetMemory);
            when(planet.getId()).thenReturn("rock");
            when(planet.getLocation()).thenReturn(new Vector2f());
            CampaignFleetAPI guardians = mock(CampaignFleetAPI.class);
            MemoryAPI guardianMemory = MapMemory.create();
            when(guardians.getMemoryWithoutUpdate()).thenReturn(guardianMemory);
            planetMemory.set(ExcavationFleets.SITE_FLEET_KEY, guardians);

            ExcavationDeliveries.deliverCache(run, planet, null, 100f, 30f);

            assertEquals(30f, ExcavationDeliveries.lootPercent(guardians), 0.001f);
            assertEquals(1f, ExcavationDeliveries.raidDifficultyMult(planet), 0.001f);
        }
    }

    @Test
    void aSecondCavernsFleetKeepsTheDeliveredSitesLootLine() {
        MemoryAPI planetMemory = MapMemory.create();
        planetMemory.set(ExcavationDeliveries.LOOT_KEY, 30f);
        CampaignFleetAPI secondCavern = mock(CampaignFleetAPI.class);
        MemoryAPI secondCavernMemory = MapMemory.create();
        when(secondCavern.getMemoryWithoutUpdate()).thenReturn(secondCavernMemory);
        SectorEntityToken planet = planetWith(planetMemory);
        SectorEntityToken undelivered = planetWith(MapMemory.create());

        assertSame(secondCavern, ExcavationDeliveries.withDeliveryLoot(planet, secondCavern));
        assertEquals(30f, ExcavationDeliveries.lootPercent(secondCavern), 0.001f);
        assertNull(ExcavationDeliveries.withDeliveryLoot(planet, null));
        assertFalse(ExcavationDeliveries.lootPercent(undelivered) > 0f);
    }
}
