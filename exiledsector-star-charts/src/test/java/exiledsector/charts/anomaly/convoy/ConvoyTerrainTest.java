package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import exiledsector.charts.anomaly.stormfront.StormFrontTerrainPlugin;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConvoyTerrainTest {

    private ConvoySystem convoySystem;
    private ConvoyState convoy;
    private final StatBonus burn = new StatBonus();
    private NebulaTerrainPlugin nebula;

    @BeforeEach
    void setUp() {
        convoySystem = new ConvoySystem(4);
        SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));
        convoy = convoySystem.script().convoys().get(0);
        CampaignFleetAPI fleet = convoySystem.fleet(convoy);
        MutableFleetStatsAPI stats = mock(MutableFleetStatsAPI.class);
        when(stats.getFleetwideMaxBurnMod()).thenReturn(burn);
        when(fleet.getStats()).thenReturn(stats);
        nebula = mock(NebulaTerrainPlugin.class);
        CampaignTerrainAPI nebulaTerrain = mock(CampaignTerrainAPI.class);
        when(nebulaTerrain.getPlugin()).thenReturn(nebula);
        CampaignTerrainAPI corona = mock(CampaignTerrainAPI.class);
        StarCoronaTerrainPlugin coronaPlugin = mock(StarCoronaTerrainPlugin.class);
        when(coronaPlugin.containsEntity(any())).thenReturn(true);
        when(corona.getPlugin()).thenReturn(coronaPlugin);
        convoySystem.terrain.addAll(List.of(nebulaTerrain, corona));
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
    }

    private Float burnMult() {
        return burn.getMultBonus(ConvoyTerrain.BURN_MOD_ID) == null ? null : burn.getMultBonus(ConvoyTerrain.BURN_MOD_ID).getValue();
    }

    @Test
    void theConvoySlowsInANebulaAndPicksUpSpeedOnceOut() {
        convoySystem.script().slowInTerrain(convoy, ConvoyTerrain.slowingTerrain(convoySystem.system));
        assertNull(burnMult(), "open space and coronae leave the convoy at full burn");

        when(nebula.containsEntity(any())).thenReturn(true);
        convoySystem.script().slowInTerrain(convoy, ConvoyTerrain.slowingTerrain(convoySystem.system));
        assertEquals(ConvoyRules.TERRAIN_BURN, burnMult(), 0.001f);

        when(nebula.containsEntity(any())).thenReturn(false);
        convoySystem.script().slowInTerrain(convoy, ConvoyTerrain.slowingTerrain(convoySystem.system));
        assertNull(burnMult(), "leaving the nebula lifts the slowdown");
    }

    @Test
    void onlyAsteroidsNebulaeAndSlowingAnomaliesSlowTheConvoy() {
        assertTrue(ConvoyTerrain.slowsConvoys(nebula));
        assertTrue(ConvoyTerrain.slowsConvoys(new StormFrontTerrainPlugin()));
        assertFalse(ConvoyTerrain.slowsConvoys(mock(StarCoronaTerrainPlugin.class)));
    }
}
