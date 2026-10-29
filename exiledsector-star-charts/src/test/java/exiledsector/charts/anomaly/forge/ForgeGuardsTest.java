package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SDMParams;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SalvageDefenderModificationPlugin;
import exiledlib.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.objective.ChartObjective;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.run.cleanup.ChartDefenderTallies;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForgeGuardsTest {

    private static final String RUN_ID = "run1";
    private static final long SEED = 42L;
    private static final String DERELICT_TALLY_KEY = SalvageGenFromSeed.DEFEATED_DERELICT_STR;

    private MockSector mockSector;
    private FakeChartContentEngine engine;
    private StarSystemAPI system;
    private CustomCampaignEntityAPI forge;
    private MemoryAPI forgeMemory;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        MemoryAPI globalMemory = FakeChartContentEngine.memory();
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(globalMemory);
        engine = FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing());
        system = mock(StarSystemAPI.class);
        forge = engine.tagged(mock(CustomCampaignEntityAPI.class));
        forgeMemory = FakeChartContentEngine.memory();
        when(forge.getMemoryWithoutUpdate()).thenReturn(forgeMemory);
        when(forge.getContainingLocation()).thenReturn(system);
        when(forge.getLocation()).thenReturn(new Vector2f(300f, 400f));
        ForgeGuards.arm(forge, SEED);
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        mockSector.close();
    }

    private static ChartRun run(int tier) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(tier, "pirates", ChartObjective.CLEAR, List.of(), null, 3, 2));
        ChartRuns.store(run);
        return run;
    }

    private static float escortPoints(int tier) {
        ChartTier chartTier = ChartAnomalyFleets.tier(tier, ChartGenContext.none());
        return ChartAnomalyFleets.combat(Factions.DERELICT, "patrol", chartTier, chartTier.fleetSize() * ForgeRules.ESCORT_SIZE_SHARE).combatPoints();
    }

    @Test
    void theGuardsAreDerelictDronesUnderOneAlphaCoreGuardian() {
        CampaignFleetAPI guards = ForgeGuards.muster(forge, run(4));

        assertEquals(1, engine.fleetParams.size());
        FleetParamsV3 escort = engine.fleetParams.get(0);
        assertEquals(Factions.DERELICT, escort.factionId);
        assertEquals(ForgeRules.ESCORT_MAX_SHIP_SIZE, escort.maxShipSize);
        assertFalse(escort.withOfficers);
        assertEquals(escortPoints(4), escort.combatPts, 0.001f);
        assertEquals(List.of(new FakeChartContentEngine.Flagship(guards, ForgeGuards.GUARDIAN_VARIANT, Commodities.ALPHA_CORE)), engine.flagships);
    }

    @Test
    void theEscortGrowsWithTheTierWhileTheGuardianStaysAlone() {
        ForgeGuards.muster(forge, run(1));
        forgeMemory.unset(ForgeGuards.DEFENDER_FLEET_KEY);
        ForgeGuards.muster(forge, run(12));

        assertTrue(engine.fleetParams.get(1).combatPts > engine.fleetParams.get(0).combatPts);
        assertEquals(2, engine.flagships.size());
        assertTrue(engine.flagships.stream().allMatch(flagship -> ForgeGuards.GUARDIAN_VARIANT.equals(flagship.variantId())));
    }

    @Test
    void theGuardsSitInTheForgesMemoryTheWayVanillaDefendersDo() {
        CampaignFleetAPI guards = ForgeGuards.muster(forge, run(4));

        assertSame(guards, forgeMemory.get(ChartIds.VANILLA_DEFENDER_FLEET_MEMORY_KEY));
        assertTrue(forgeMemory.getBoolean(ForgeGuards.HAS_DEFENDERS_KEY));
        assertEquals(SEED, forgeMemory.get(MemFlags.SALVAGE_SEED));
        verify(guards).setContainingLocation(system);
        assertEquals(new Vector2f(300f, 400f), guards.getLocation());
    }

    @Test
    void theGuardsCarryTheChartTierForTheirSkillTrees() {
        CampaignFleetAPI guards = ForgeGuards.muster(forge, run(6));

        assertEquals(6, guards.getMemoryWithoutUpdate().get(ChartIds.TIER_MEMORY_KEY));
        assertTrue(guards.getMemoryWithoutUpdate().getBoolean(ChartIds.fleetRunKey(RUN_ID)));
        assertFalse(guards.getMemoryWithoutUpdate().contains(ChartIds.START_DP_MEMORY_KEY));
    }

    @Test
    void storedGuardsComeBackInsteadOfANewFleet() {
        ChartRun run = run(4);
        CampaignFleetAPI first = ForgeGuards.muster(forge, run);
        first.getMemoryWithoutUpdate().unset(ChartIds.TIER_MEMORY_KEY);

        CampaignFleetAPI second = ForgeGuards.muster(forge, run);

        assertSame(first, second);
        assertEquals(1, engine.fleets.size());
        assertEquals(4, second.getMemoryWithoutUpdate().get(ChartIds.TIER_MEMORY_KEY));
    }

    @Test
    void aBeatenForgeHasNoGuards() {
        forgeMemory.set(ForgeGuards.DEFEATED_KEY, true);

        assertFalse(ForgeGuards.guarded(forge));
        assertNull(ForgeGuards.muster(forge, run(4)));
        assertTrue(engine.fleetParams.isEmpty());
    }

    @Test
    void theGuardianStandsAloneWhenNoDronesFit() {
        engine.withEmptyFleets();

        CampaignFleetAPI guards = ForgeGuards.muster(forge, run(4));

        assertEquals(List.of(guards), engine.emptyFleetsCreated);
        assertEquals(1, engine.flagships.size());
    }

    @Test
    void withNeitherDronesNorAGuardianTheForgeStandsOpen() {
        engine.withEmptyFleets().withoutFlagships();

        assertNull(ForgeGuards.muster(forge, run(4)));
        assertFalse(forgeMemory.contains(ChartIds.VANILLA_DEFENDER_FLEET_MEMORY_KEY));
    }

    @Test
    void noRunMeansNoGuards() {
        assertNull(ForgeGuards.muster(forge, null));
    }

    @Test
    void beatingTheGuardsLeavesTheDerelictTallyWhereItWas() {
        MemoryAPI global = mockSector.sector.getMemoryWithoutUpdate();
        global.set(DERELICT_TALLY_KEY, 50f);
        when(system.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(true);
        SalvageDefenderModificationPlugin vanilla = mock(SalvageDefenderModificationPlugin.class);
        when(vanilla.getHandlingPriority(any())).thenReturn(1);
        doAnswer(invocation -> {
            global.set(DERELICT_TALLY_KEY, global.getFloat(DERELICT_TALLY_KEY) + 30f);
            return null;
        }).when(vanilla).reportDefeated(any(), any(), any());
        ChartDefenderTallies tallies = new ChartDefenderTallies();
        GenericPluginManagerAPI plugins = mock(GenericPluginManagerAPI.class);
        when(mockSector.sector.getGenericPlugins()).thenReturn(plugins);
        when(plugins.getPluginsOfClass(SalvageDefenderModificationPlugin.class)).thenReturn(List.of(vanilla, tallies));
        SDMParams params = new SDMParams();
        params.entity = forge;
        params.factionId = Factions.DERELICT;
        CampaignFleetAPI beaten = mock(CampaignFleetAPI.class);

        assertTrue(tallies.getHandlingPriority(params) > vanilla.getHandlingPriority(params));
        tallies.reportDefeated(params, forge, beaten);

        assertEquals(50f, global.get(DERELICT_TALLY_KEY));
        verify(vanilla).reportDefeated(params, forge, beaten);
    }
}
