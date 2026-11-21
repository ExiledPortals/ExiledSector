package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.MockSector;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.modifier.ItemModifiers;
import exiledsector.items.modifier.PoolEntry;
import exiledsector.items.modifier.RolledModifier;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BreachLootTest {

    private static final String BRIDGE = "test_bridge";
    private static final int SEEDS = 2000;

    private MockSector mockSector;

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        CampaignClockAPI clock = mock(CampaignClockAPI.class);
        when(clock.convertToDays(anyFloat())).thenAnswer(invocation -> (float) invocation.getArgument(0) / 10f);
        when(mockSector.sector.getClock()).thenReturn(clock);
        ItemModifiers.register(new JSONArray().put(modifierRow("T_ARMOR", "ARMOR_PERCENT:4:6")).put(modifierRow("T_VENTS", "VENT_RATE_PERCENT:10:20")));
        ItemDefinitions.register(List.of(definition(BRIDGE, ItemKind.BRIDGE, "T_ARMOR"), definition("test_reactor", ItemKind.REACTOR, "T_VENTS")));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        ItemModifiers.clear();
        mockSector.close();
    }

    private static JSONObject modifierRow(String id, String lines) throws Exception {
        return new JSONObject().put("id", id).put("affix", "prefix").put("scopes", "hull_item").put("lines", lines).put("name", id);
    }

    private static ItemDefinition definition(String id, ItemKind kind, String prefixId) {
        return new ItemDefinition(id, kind, id, ItemDefinition.FALLBACK_ICON, 1f, false, "", List.of(new PoolEntry(ItemModifiers.get(prefixId), 1f)),
                List.of(), "");
    }

    private static ItemData item(long seed) {
        return ItemData.rolled(ItemDefinitions.get(BRIDGE), seed);
    }

    private static boolean warped(ItemData item) {
        return item.explicits() != null && item.explicits().stream().anyMatch(RolledModifier::warped);
    }

    @Test
    void aboutThirtyPercentOfDropsAreWarpedTheSameWayEveryTime() {
        int warpedCount = 0;
        for (long seed = 0L; seed < SEEDS; seed++) {
            ItemData shaped = BreachLoot.warped(item(seed));
            assertEquals(shaped, BreachLoot.warped(item(seed)));
            warpedCount += warped(shaped) ? 1 : 0;
        }
        float share = warpedCount / (float) SEEDS;
        assertTrue(Math.abs(share - BreachRules.WARPED_CHANCE) < 0.04f, String.valueOf(share));
        assertNull(BreachLoot.warped(null));
    }

    @Test
    void aHigherChanceWarpsEveryItemTheBaseChanceDidAndAnAlreadyWarpedItemStaysAsItIs() {
        for (long seed = 0L; seed < 200L; seed++) {
            ItemData base = BreachLoot.warped(item(seed));
            ItemData raised = BreachLoot.warped(item(seed), BreachRules.battleWarpedChance(50f, 0f));
            if (warped(base)) {
                assertEquals(base, raised);
            }
            if (warped(raised)) {
                assertSame(raised, BreachLoot.warped(raised, 1f));
            }
            assertTrue(warped(BreachLoot.warped(item(seed), 1f)));
            assertFalse(warped(BreachLoot.warped(item(seed), 0f)));
        }
    }

    @Test
    void anomalousFleetsShareOfTheDefeatedDeploymentPointsCountsOnlyTheLatestBattle() {
        CampaignFleetAPI debris = mock(CampaignFleetAPI.class);
        CampaignFleetAPI pirates = mock(CampaignFleetAPI.class);
        FleetMemberAPI debrisShip = mock(FleetMemberAPI.class);
        when(debrisShip.getDeploymentPointsCost()).thenReturn(30f);
        FleetMemberAPI pirateShip = mock(FleetMemberAPI.class);
        when(pirateShip.getDeploymentPointsCost()).thenReturn(10f);
        Map<FleetMemberAPI, CampaignFleetAPI> sources = Map.of(debrisShip, debris, pirateShip, pirates);
        BattleEngagement engagement = new BattleEngagement(sources::get, 1f, 40f, List.of(debrisShip, pirateShip));
        BattleLoot first = new BattleLoot(new Random(1L), new Random(2L));
        BattleLoot second = new BattleLoot(new Random(1L), new Random(2L));

        BreachBattles.reset();
        assertEquals(0f, BreachBattles.anomalousShare());
        BreachBattles.record(first, engagement, fleet -> fleet == debris);
        assertEquals(0.75f, BreachBattles.anomalousShare(), 0.0001f);
        BreachBattles.record(first, engagement, fleet -> fleet == debris);
        assertEquals(0.75f, BreachBattles.anomalousShare(), 0.0001f);
        BreachBattles.record(second, engagement, fleet -> false);
        assertEquals(0f, BreachBattles.anomalousShare());
        BreachBattles.reset();
    }

    @Test
    void eventHorizonDerelictsAlwaysWarpTheirHullItemsAndOtherSitesNever() {
        MemoryAPI marked = mock(MemoryAPI.class);
        when(marked.getBoolean(EventHorizon.WARPED_KEY)).thenReturn(true);
        MemoryAPI plain = mock(MemoryAPI.class);

        for (long seed = 0L; seed < 20L; seed++) {
            assertTrue(warped(EventHorizon.onSiteDrop(item(seed), marked)));
            ItemData dropped = item(seed);
            assertSame(dropped, EventHorizon.onSiteDrop(dropped, plain));
            assertSame(dropped, EventHorizon.onSiteDrop(dropped, null));
        }
        assertNull(EventHorizon.onSiteDrop(null, marked));
        assertEquals(5000f + EventHorizon.EDGE_MARGIN, EventHorizon.radiusAround(300f, 5000f));
        assertEquals(300f + EventHorizon.EDGE_MARGIN, EventHorizon.radiusAround(300f, 0f));
    }

    @Test
    void onlyDropsWhileThePlayerFleetIsInsideTheOpenBreachCanBeWarped() {
        StarSystemAPI system = mock(StarSystemAPI.class);
        SectorEntityToken terrainEntity = mock(SectorEntityToken.class);
        when(terrainEntity.getLocation()).thenReturn(new Vector2f());
        when(terrainEntity.getContainingLocation()).thenReturn(system);
        BreachTerrainPlugin breach = new BreachTerrainPlugin();
        breach.init(SpatialBreach.TERRAIN_ID, terrainEntity,
                new BreachTerrainPlugin.Params("run1", 5, new BreachRules.Growth(2000f, 60f, 60f), List.of(), 1f, 9L));
        CampaignTerrainAPI terrain = mock(CampaignTerrainAPI.class);
        when(terrain.getPlugin()).thenReturn(breach);
        when(system.getTerrainCopy()).thenReturn(List.of(terrain));
        CampaignFleetAPI player = mockSector.playerFleet();
        Vector2f playerAt = new Vector2f(500f, 0f);
        when(player.getLocation()).thenReturn(playerAt);
        breach.elapse(30f);

        int changed = 0;
        for (long seed = 0L; seed < 50L; seed++) {
            ItemData dropped = item(seed);
            ItemData shaped = SpatialBreach.INSTANCE.shapeDrop(mockSector.sector, dropped, system);
            assertEquals(BreachLoot.warped(dropped), shaped);
            changed += warped(shaped) ? 1 : 0;
        }
        assertTrue(changed > 0);

        playerAt.set(5000f, 0f);
        for (long seed = 0L; seed < 50L; seed++) {
            ItemData dropped = item(seed);
            assertSame(dropped, SpatialBreach.INSTANCE.shapeDrop(mockSector.sector, dropped, system));
        }
    }
}
