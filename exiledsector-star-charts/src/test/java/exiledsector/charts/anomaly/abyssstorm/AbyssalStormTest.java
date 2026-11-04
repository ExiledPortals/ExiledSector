package exiledsector.charts.anomaly.abyssstorm;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import exiledsector.MockSector;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.layout.abyss.AbyssalFogTerrainPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbyssalStormTest {

    private static final float FRAME = 0.05f;

    @AfterEach
    void tearDown() {
        ChartTables.resetAnomalies();
    }

    private static AtlasTraits traitsIn(String regionId) {
        return new AtlasTraits(new AtlasCell(1, 1), 20f, regionId, "rat_abyssals", "t1", "abyssal_depths", Set.of(), List.of(), null, null);
    }

    @Test
    void theShippedRowIsAnInsertableStormThatOnlyFitsTheAbyssalRift() throws Exception {
        ChartTables.loadAnomalies();

        assertSame(AbyssalStorm.INSTANCE, ChartAnomalies.byId(AbyssalStorm.ANOMALY_ID));
        ChartAnomalies.Definition definition = ChartAnomalies.definition(AbyssalStorm.ANOMALY_ID);
        assertTrue(definition.insertable());
        assertTrue(definition.biomes().isEmpty());
        assertFalse(definition.effects().isEmpty());
        assertTrue(AbyssalStorm.INSTANCE.fitsAt(traitsIn(AbyssalStorm.BIOME_ID)));
        assertFalse(AbyssalStorm.INSTANCE.fitsAt(traitsIn("normal")));
    }

    @Test
    void aStormBreaksWithinTheLongestCalmAndFlashesOnlyWhileItLasts() {
        StormCycle cycle = new StormCycle(new Random(4L));
        float seconds = 0f;
        float firstStorm = -1f;
        float stormSeconds = 0f;
        int strikes = 0;
        while (seconds < StormCycle.MAX_CALM_SECONDS + StormCycle.MAX_STORM_SECONDS + 5f) {
            cycle.advance(FRAME, new Random((long) (seconds * 1000f)));
            seconds += FRAME;
            if (cycle.storming()) {
                stormSeconds += FRAME;
                if (firstStorm < 0f) {
                    firstStorm = seconds;
                }
            } else if (firstStorm < 0f) {
                assertEquals(0f, cycle.brightness(), "a flash before the first storm");
            }
            if (cycle.struckThisFrame()) {
                assertTrue(cycle.storming(), "a strike outside a storm");
                strikes++;
            }
        }

        assertTrue(firstStorm > 0f && firstStorm <= StormCycle.MAX_CALM_SECONDS + FRAME, String.valueOf(firstStorm));
        assertTrue(stormSeconds >= StormCycle.MIN_STORM_SECONDS - FRAME && stormSeconds <= StormCycle.MAX_STORM_SECONDS + FRAME,
                String.valueOf(stormSeconds));
        assertTrue(strikes > 0);
    }

    @Test
    void theStormExposesFleetsOutsideTheFogAndLeavesHiddenOnesAlone() {
        try (MockSector ignored = new MockSector()) {
            LocationAPI location = mock(LocationAPI.class);
            AbyssalFogTerrainPlugin fog = mock(AbyssalFogTerrainPlugin.class);
            CampaignTerrainAPI fogTerrain = mock(CampaignTerrainAPI.class);
            when(fogTerrain.getPlugin()).thenReturn(fog);
            when(location.getTerrainCopy()).thenReturn(List.of(fogTerrain));
            CampaignFleetAPI exposed = fleet();
            CampaignFleetAPI hidden = fleet();
            when(fog.containsEntity(hidden)).thenReturn(true);
            when(location.getFleets()).thenReturn(List.of(exposed, hidden));

            AbyssalStormTerrainPlugin.expose(location, 1f);

            MutableFleetStatsAPI exposedStats = exposed.getStats();
            StatBonus exposedRange = exposedStats.getDetectedRangeMod();
            verify(exposedStats).addTemporaryModMult(eq(AbyssalStormTerrainPlugin.MOD_DAYS), eq(AbyssalStormTerrainPlugin.MOD_ID), anyString(), eq(2.5f),
                    eq(exposedRange));
            verify(exposedStats).addTemporaryModFlat(eq(AbyssalStormTerrainPlugin.MOD_DAYS), eq(AbyssalStormTerrainPlugin.MOD_ID), anyString(),
                    eq(150f), eq(exposedRange));
            MutableFleetStatsAPI hiddenStats = hidden.getStats();
            StatBonus hiddenRange = hiddenStats.getDetectedRangeMod();
            verify(hiddenStats, never()).addTemporaryModMult(anyFloat(), anyString(), anyString(), anyFloat(), eq(hiddenRange));
        }
    }

    private static CampaignFleetAPI fleet() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        MutableFleetStatsAPI stats = mock(MutableFleetStatsAPI.class);
        when(stats.getDetectedRangeMod()).thenReturn(mock(StatBonus.class));
        when(fleet.getStats()).thenReturn(stats);
        return fleet;
    }
}
