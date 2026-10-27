package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConvoyTerrainTest {

    private static final float VANILLA_BATTLE_JOIN_RANGE = 500f;

    private ConvoySystem convoySystem;
    private ConvoyState convoy;
    private final Map<ConvoyRole, StatBonus> burns = new EnumMap<>(ConvoyRole.class);
    private NebulaTerrainPlugin nebula;

    @BeforeEach
    void setUp() {
        convoySystem = new ConvoySystem(4);
        SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));
        convoy = convoySystem.script().convoys().get(0);
        for (ConvoyRole role : ConvoyRole.values()) {
            CampaignFleetAPI fleet = convoySystem.fleet(convoy, role);
            StatBonus burn = new StatBonus();
            MutableFleetStatsAPI stats = mock(MutableFleetStatsAPI.class);
            when(stats.getFleetwideMaxBurnMod()).thenReturn(burn);
            when(fleet.getStats()).thenReturn(stats);
            FleetDataAPI data = mock(FleetDataAPI.class);
            when(data.getMinBurnLevelUnmodified()).thenReturn(role == ConvoyRole.FREIGHTERS ? 9f : 8f);
            when(fleet.getFleetData()).thenReturn(data);
            burns.put(role, burn);
        }
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

    private Float burnMult(ConvoyRole role) {
        return burns.get(role).getMultBonus(ConvoyTerrain.BURN_MOD_ID) == null ? null : burns.get(role).getMultBonus(ConvoyTerrain.BURN_MOD_ID).getValue();
    }

    @Test
    void inOpenSpaceTheFreightersNeverOutrunTheirSlowestEscort() {
        convoySystem.script().keepFormation(convoy, ConvoyTerrain.slowingTerrain(convoySystem.system));

        assertEquals(8f / 9f, burnMult(ConvoyRole.FREIGHTERS), 0.001f);
        assertNull(burnMult(ConvoyRole.SCREEN));
        assertNull(burnMult(ConvoyRole.ESCORT));
    }

    @Test
    void inANebulaEachFleetSlowsByADifferentAmountSoTheConvoyDriftsApart() {
        when(nebula.containsEntity(any())).thenReturn(true);

        convoySystem.script().keepFormation(convoy, ConvoyTerrain.slowingTerrain(convoySystem.system));

        assertEquals(ConvoyRules.FREIGHTER_TERRAIN_BURN, burnMult(ConvoyRole.FREIGHTERS), 0.001f);
        assertEquals(ConvoyRules.SCREEN_TERRAIN_BURN, burnMult(ConvoyRole.SCREEN), 0.001f);
        assertEquals(ConvoyRules.ESCORT_TERRAIN_BURN, burnMult(ConvoyRole.ESCORT), 0.001f);
        assertTrue(ConvoyRules.ESCORT_TERRAIN_BURN < ConvoyRules.FREIGHTER_TERRAIN_BURN, "the close escort falls behind in terrain");

        when(nebula.containsEntity(any())).thenReturn(false);
        convoySystem.script().keepFormation(convoy, ConvoyTerrain.slowingTerrain(convoySystem.system));
        assertNull(burnMult(ConvoyRole.ESCORT), "leaving the nebula lifts the slowdown");
    }

    @Test
    void onceAttackedTheFreightersRunFlatOut() {
        convoySystem.script().turnHostile(convoy);

        convoySystem.script().keepFormation(convoy, ConvoyTerrain.slowingTerrain(convoySystem.system));

        assertNull(burnMult(ConvoyRole.FREIGHTERS));
    }

    @Test
    void onlyAsteroidsAndNebulaeSplitTheConvoyAndTheScreenRidesBeyondBattleRange() {
        assertTrue(ConvoyTerrain.slowsConvoys(nebula));
        assertFalse(ConvoyTerrain.slowsConvoys(mock(StarCoronaTerrainPlugin.class)));
        assertEquals(1f, ConvoyTerrain.formationMult(8f, 9f));
        assertTrue(ConvoyRules.SCREEN_LEAD > VANILLA_BATTLE_JOIN_RANGE, "hitting the freighters in open space leaves the screen out");
    }
}
