package exiledsector.charts.anomaly.hypershunt;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.objective.ChartObjective;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HypershuntGuardsTest {

    private static final String RUN_ID = "run1";
    private static final long SEED = 42L;

    private MockSector mockSector;
    private FakeChartContentEngine engine;
    private StarSystemAPI system;
    private CustomCampaignEntityAPI hypershunt;
    private MemoryAPI hypershuntMemory;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        MemoryAPI globalMemory = FakeChartContentEngine.memory();
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(globalMemory);
        engine = FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing());
        system = mock(StarSystemAPI.class);
        hypershunt = engine.tagged(mock(CustomCampaignEntityAPI.class));
        hypershuntMemory = FakeChartContentEngine.memory();
        when(hypershunt.getMemoryWithoutUpdate()).thenReturn(hypershuntMemory);
        when(hypershunt.getContainingLocation()).thenReturn(system);
        when(hypershunt.getLocation()).thenReturn(new Vector2f(300f, 400f));
        HypershuntGuards.arm(hypershunt, SEED);
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        mockSector.close();
    }

    private static ChartRun run(int tier) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(tier, "pirates", ChartObjective.CLEAR, List.of(), null, 3, 2, List.of(CoronalHypershunt.ANOMALY_ID)));
        ChartRuns.store(run);
        return run;
    }

    @Test
    void theGuardsAreAnOfficeredOmegaFleetNeverWeakerThanVanillasDefenders() {
        HypershuntGuards.muster(hypershunt, run(1));

        assertEquals(1, engine.fleetParams.size());
        FleetParamsV3 params = engine.fleetParams.get(0);
        assertEquals(Factions.OMEGA, params.factionId);
        assertTrue(params.withOfficers);
        assertTrue(params.combatPts >= HypershuntGuards.VANILLA_DEFENDER_POINTS);
    }

    @Test
    void theGuardsGrowWithTheTier() {
        HypershuntGuards.muster(hypershunt, run(1));
        hypershuntMemory.unset(HypershuntGuards.DEFENDER_FLEET_KEY);
        HypershuntGuards.muster(hypershunt, run(15));

        assertTrue(engine.fleetParams.get(1).combatPts > engine.fleetParams.get(0).combatPts);
    }

    @Test
    void theGuardsDropOmegaWeaponsAndChartLoot() {
        CampaignFleetAPI guards = HypershuntGuards.muster(hypershunt, run(6));

        ArgumentCaptor<DropData> drops = ArgumentCaptor.forClass(DropData.class);
        verify(guards, atLeastOnce()).addDropRandom(drops.capture());
        List<String> groups = drops.getAllValues().stream().map(drop -> drop.group).toList();
        assertTrue(groups.containsAll(List.of("omega_weapons_small", "omega_weapons_medium")), groups.toString());
        assertEquals(6, guards.getMemoryWithoutUpdate().get(ChartIds.TIER_MEMORY_KEY));
        assertTrue(guards.getMemoryWithoutUpdate().getBoolean(ChartIds.fleetRunKey(RUN_ID)));
    }

    @Test
    void theGuardsSitInTheHypershuntsMemoryTheWayVanillaDefendersDo() {
        CampaignFleetAPI guards = HypershuntGuards.muster(hypershunt, run(4));

        assertSame(guards, hypershuntMemory.get(ChartIds.VANILLA_DEFENDER_FLEET_MEMORY_KEY));
        assertTrue(hypershuntMemory.getBoolean(HypershuntGuards.HAS_DEFENDERS_KEY));
        assertEquals(SEED, hypershuntMemory.get(MemFlags.SALVAGE_SEED));
        verify(guards).setContainingLocation(system);
        assertEquals(new Vector2f(300f, 400f), guards.getLocation());
    }

    @Test
    void storedGuardsComeBackInsteadOfANewFleet() {
        ChartRun run = run(4);
        CampaignFleetAPI first = HypershuntGuards.muster(hypershunt, run);

        assertSame(first, HypershuntGuards.muster(hypershunt, run));
        assertEquals(1, engine.fleets.size());
    }

    @Test
    void aBeatenHypershuntHasNoGuards() {
        hypershuntMemory.set(HypershuntGuards.DEFEATED_KEY, true);

        assertFalse(HypershuntGuards.guarded(hypershunt));
        assertNull(HypershuntGuards.muster(hypershunt, run(4)));
        assertNull(HypershuntGuards.muster(hypershunt, null));
        assertTrue(engine.fleetParams.isEmpty());
    }
}
