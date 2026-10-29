package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetLogisticsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BreachTerrainPluginTest {

    private static final BreachRules.Growth GROWTH = new BreachRules.Growth(2000f, 60f, 60f);

    private MockSector mockSector;
    private StarSystemAPI system;
    private SectorEntityToken terrainEntity;
    private final List<CampaignFleetAPI> fleets = new ArrayList<>();
    private final List<SectorEntityToken> contentSites = new ArrayList<>();
    private final List<SectorEntityToken> breachCaches = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        system = mock(StarSystemAPI.class);
        when(system.getFleets()).thenReturn(fleets);
        when(system.getEntitiesWithTag(ChartIds.CONTENT_TAG)).thenReturn(contentSites);
        when(system.getEntitiesWithTag(SpatialBreach.CACHE_TAG)).thenReturn(breachCaches);
        terrainEntity = mock(SectorEntityToken.class);
        when(terrainEntity.getLocation()).thenReturn(new Vector2f());
        when(terrainEntity.getContainingLocation()).thenReturn(system);
        CampaignClockAPI clock = mock(CampaignClockAPI.class);
        when(clock.convertToDays(anyFloat())).thenAnswer(invocation -> (float) invocation.getArgument(0) / 10f);
        when(mockSector.sector.getClock()).thenReturn(clock);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private BreachTerrainPlugin breach(List<BreachRules.CacheSite> caches) {
        BreachTerrainPlugin plugin = new BreachTerrainPlugin();
        plugin.init(SpatialBreach.TERRAIN_ID, terrainEntity, new BreachTerrainPlugin.Params("run1", 5, GROWTH, caches, 1.5f, 9L));
        return plugin;
    }

    private static MemoryAPI memory() {
        Map<String, Object> stored = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        doAnswer(invocation -> stored.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        when(memory.contains(anyString())).thenAnswer(invocation -> stored.containsKey((String) invocation.getArgument(0)));
        when(memory.getFloat(anyString())).thenAnswer(invocation -> stored.get((String) invocation.getArgument(0)) instanceof Number number
                ? number.floatValue() : 0f);
        return memory;
    }

    private CampaignFleetAPI fleetAt(float x, float y, boolean player) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getLocation()).thenReturn(new Vector2f(x, y));
        when(fleet.getStats()).thenReturn(mock(MutableFleetStatsAPI.class));
        when(fleet.isPlayerFleet()).thenReturn(player);
        fleets.add(fleet);
        return fleet;
    }

    @Test
    void theBreachStaysClosedUntilThePlayerFleetFliesOverItsCore() {
        BreachTerrainPlugin plugin = breach(List.of());
        CampaignFleetAPI player = mockSector.playerFleet();
        Vector2f playerAt = new Vector2f(3000f, 0f);
        when(player.getLocation()).thenReturn(playerAt);
        when(player.getContainingLocation()).thenReturn(system);
        when(player.getRadius()).thenReturn(50f);

        plugin.advance(1f);
        assertFalse(plugin.isOpened());
        assertEquals(0f, plugin.radius());
        assertFalse(plugin.containsPoint(new Vector2f(), 10f));

        playerAt.set(100f, 0f);
        plugin.advance(1f);
        assertTrue(plugin.isOpened());
        assertTrue(plugin.radius() > 0f);
        playerAt.set(5000f, 0f);
        float opening = plugin.radius();
        plugin.advance(1f);
        assertTrue(plugin.radius() > opening);
    }

    @Test
    void theBreachExpandsOverAMinuteThenCollapsesOverAMinuteAndTakesItsCoreWithIt() {
        BreachTerrainPlugin plugin = breach(List.of());
        SectorEntityToken core = mock(SectorEntityToken.class);
        when(system.getEntityById(SpatialBreach.coreId("run1"))).thenReturn(core);

        plugin.elapse(30f);
        assertEquals(1000f, plugin.radius(), 0.01f);
        plugin.elapse(30f);
        assertEquals(2000f, plugin.radius(), 0.01f);
        plugin.elapse(30f);
        assertEquals(1000f, plugin.radius(), 0.01f);
        assertFalse(plugin.isClosed());
        plugin.elapse(30f);
        assertTrue(plugin.isClosed());
        verify(system).removeEntity(terrainEntity);
        verify(system).removeEntity(core);
        assertEquals(1.5f, plugin.cacheItemMult());
        assertEquals("run1", plugin.runId());
        assertEquals(5, plugin.tier());
    }

    @Test
    void fleetsKeepTheirSensorRangeAndBurnLevelInside() {
        BreachTerrainPlugin plugin = breach(List.of());
        CampaignFleetAPI player = fleetAt(0f, 0f, true);
        CampaignFleetAPI enemy = fleetAt(0f, 0f, false);

        plugin.elapse(60f);
        plugin.tick(BreachTerrainPlugin.TICK_DAYS);

        verify(player.getStats(), never()).addTemporaryModMult(anyFloat(), anyString(), any(), anyFloat(), ArgumentMatchers.<StatBonus>any());
        verify(enemy.getStats(), never()).addTemporaryModMult(anyFloat(), anyString(), any(), anyFloat(), ArgumentMatchers.<StatBonus>any());
    }

    @Test
    void onlyThePlayerFleetUsesMoreSuppliesAndBurnsFuel() {
        BreachTerrainPlugin plugin = breach(List.of());
        CampaignFleetAPI player = fleetAt(0f, 0f, true);
        BuffManagerAPI buffs = mock(BuffManagerAPI.class);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getBuffManager()).thenReturn(buffs);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));
        when(player.getFleetData()).thenReturn(fleetData);
        FleetLogisticsAPI logistics = mock(FleetLogisticsAPI.class);
        when(logistics.getFuelCostPerLightYear()).thenReturn(10f);
        when(player.getLogistics()).thenReturn(logistics);
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getFuel()).thenReturn(100f);
        when(player.getCargo()).thenReturn(cargo);
        CampaignFleetAPI enemy = fleetAt(0f, 0f, false);

        plugin.elapse(60f);
        plugin.tick(1f);

        ArgumentCaptor<BuffManagerAPI.Buff> buff = ArgumentCaptor.forClass(BuffManagerAPI.Buff.class);
        verify(buffs).addBuffOnlyUpdateStat(buff.capture());
        assertEquals(BreachTerrainPlugin.SUPPLY_BUFF_ID, buff.getValue().getId());
        assertEquals(BreachRules.SUPPLY_USE_PERCENT, ((BreachSupplyBuff) buff.getValue()).percent());
        verify(cargo).removeFuel(10f * BreachRules.FUEL_LIGHT_YEARS_PER_DAY);
        verify(enemy, never()).getCargo();
        verify(enemy, never()).getFleetData();
    }

    @Test
    void sitesTheFieldGrowsOverBecomeBreachTouchedOnce() {
        BreachTerrainPlugin plugin = breach(List.of());
        MemoryAPI nearMemory = memory();
        SectorEntityToken near = mock(SectorEntityToken.class);
        when(near.getLocation()).thenReturn(new Vector2f(300f, 0f));
        when(near.getMemoryWithoutUpdate()).thenReturn(nearMemory);
        MemoryAPI farMemory = memory();
        SectorEntityToken far = mock(SectorEntityToken.class);
        when(far.getLocation()).thenReturn(new Vector2f(1500f, 0f));
        when(far.getMemoryWithoutUpdate()).thenReturn(farMemory);
        SectorEntityToken alreadyRare = mock(SectorEntityToken.class);
        when(alreadyRare.getLocation()).thenReturn(new Vector2f(100f, 0f));
        MemoryAPI rareMemory = memory();
        rareMemory.set(SpatialBreach.RARITY_KEY, 90f);
        when(alreadyRare.getMemoryWithoutUpdate()).thenReturn(rareMemory);
        contentSites.addAll(List.of(near, far, alreadyRare));

        plugin.elapse(20f);
        plugin.tick(BreachTerrainPlugin.TICK_DAYS);

        assertEquals(BreachRules.TOUCHED_RARITY_PERCENT, near.getMemoryWithoutUpdate().getFloat(SpatialBreach.RARITY_KEY));
        assertFalse(far.getMemoryWithoutUpdate().contains(SpatialBreach.RARITY_KEY));
        assertEquals(90f, rareMemory.getFloat(SpatialBreach.RARITY_KEY));
        plugin.elapse(40f);
        plugin.tick(BreachTerrainPlugin.TICK_DAYS);
        assertEquals(BreachRules.TOUCHED_RARITY_PERCENT, far.getMemoryWithoutUpdate().getFloat(SpatialBreach.RARITY_KEY));
    }

    @Test
    void cachesFormOnlyOnceTheFieldReachesThem() {
        BreachTerrainPlugin plugin = breach(List.of(new BreachRules.CacheSite(200f, 0f, 0.1f), new BreachRules.CacheSite(1800f, 0f, 0.9f)));

        plugin.elapse(20f);
        plugin.tick(BreachTerrainPlugin.TICK_DAYS);
        assertEquals(1, plugin.cachesFormed());
        plugin.elapse(40f);
        plugin.tick(BreachTerrainPlugin.TICK_DAYS);
        assertEquals(2, plugin.cachesFormed());
    }

    @Test
    void cachesVanishOnceTheCollapsingFieldPassesBackOverThem() {
        BreachTerrainPlugin plugin = breach(List.of());
        SectorEntityToken outer = mock(SectorEntityToken.class);
        when(outer.getLocation()).thenReturn(new Vector2f(1800f, 0f));
        SectorEntityToken inner = mock(SectorEntityToken.class);
        when(inner.getLocation()).thenReturn(new Vector2f(200f, 0f));
        breachCaches.addAll(List.of(outer, inner));

        plugin.elapse(50f);
        plugin.tick(BreachTerrainPlugin.TICK_DAYS);
        verify(system, never()).removeEntity(outer);
        plugin.elapse(40f);
        plugin.tick(BreachTerrainPlugin.TICK_DAYS);
        verify(system).removeEntity(outer);
        verify(system, never()).removeEntity(inner);
    }

    @Test
    void collapsingRemovesTheFieldItsCoreAndTheCachesLeftInsideWithNoLoot() {
        BreachTerrainPlugin plugin = breach(List.of());
        SectorEntityToken core = mock(SectorEntityToken.class);
        when(system.getEntityById(SpatialBreach.coreId("run1"))).thenReturn(core);
        SectorEntityToken leftover = mock(SectorEntityToken.class);
        breachCaches.add(leftover);
        CampaignFleetAPI inside = fleetAt(0f, 0f, false);
        CampaignFleetAPI player = mockSector.playerFleet();

        plugin.elapse(GROWTH.openSeconds() + GROWTH.collapseSeconds());

        assertTrue(plugin.isClosed());
        verify(system).removeEntity(leftover);
        verify(system).removeEntity(core);
        verify(system).removeEntity(terrainEntity);
        verify(player, never()).getCargo();
        assertFalse(plugin.containsPoint(new Vector2f(), 10f));
        plugin.advance(1f);
        plugin.collapse();
        verify(inside.getStats(), never()).addTemporaryModMult(anyFloat(), anyString(), any(), anyFloat(), ArgumentMatchers.<StatBonus>any());
        verify(system).removeEntity(core);
    }

    @Test
    void aCoreFromAnOlderSaveBecomesUnclickableOnTheFirstUpdate() {
        BreachTerrainPlugin plugin = breach(List.of());
        SectorEntityToken core = mock(SectorEntityToken.class);
        when(system.getEntityById(SpatialBreach.coreId("run1"))).thenReturn(core);
        CampaignFleetAPI player = mockSector.playerFleet();
        when(player.getLocation()).thenReturn(new Vector2f(5000f, 0f));

        plugin.advance(1f);
        plugin.advance(1f);

        verify(core).addTag(Tags.NON_CLICKABLE);
        when(core.hasTag(Tags.NON_CLICKABLE)).thenReturn(true);
        BreachTerrainPlugin reloaded = breach(List.of());
        reloaded.advance(1f);
        verify(core).addTag(Tags.NON_CLICKABLE);
    }

    @Test
    void cachesCarryTheBreachsItemQuantityAndAMissingValueCountsAsOne() {
        List<BreachRules.CacheSite> sites = List.of(new BreachRules.CacheSite(200f, 0f, 0.1f));
        MemoryAPI boostedMemory = memory();
        SectorEntityToken boosted = mock(SectorEntityToken.class);
        when(boosted.getMemoryWithoutUpdate()).thenReturn(boostedMemory);
        MemoryAPI plainMemory = memory();
        SectorEntityToken plain = mock(SectorEntityToken.class);
        when(plain.getMemoryWithoutUpdate()).thenReturn(plainMemory);
        BreachTerrainPlugin unboosted = new BreachTerrainPlugin();
        unboosted.init(SpatialBreach.TERRAIN_ID, terrainEntity, new BreachTerrainPlugin.Params("run1", 5, GROWTH, sites, 1f, 9L));

        breach(sites).markCache(boosted, 0);
        unboosted.markCache(plain, 0);

        verify(boosted).addTag(SpatialBreach.CACHE_TAG);
        assertEquals(1.5f, boosted.getMemoryWithoutUpdate().getFloat(SpatialBreach.ITEM_MULT_KEY));
        assertTrue(boosted.getMemoryWithoutUpdate().contains(SpatialBreach.RARITY_KEY));
        assertFalse(plain.getMemoryWithoutUpdate().contains(SpatialBreach.ITEM_MULT_KEY));
        assertEquals(1f, new BreachTerrainPlugin().cacheItemMult());
    }
}
