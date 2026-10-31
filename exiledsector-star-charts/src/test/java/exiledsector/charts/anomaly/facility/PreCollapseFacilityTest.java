package exiledsector.charts.anomaly.facility;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PreCollapseFacilityTest {

    private static final String RUN_ID = "run1";

    private MockSector mockSector;
    private StarSystemAPI system;
    private final List<PlanetAPI> planets = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        system = mock(StarSystemAPI.class);
        when(system.getPlanets()).thenReturn(planets);
        when(system.getId()).thenReturn("charted space");
        when(system.getOptionalUniqueId()).thenReturn(ChartIds.systemId(RUN_ID));
    }

    @AfterEach
    void tearDown() {
        ChartAnomalies.resetForTests();
        mockSector.close();
    }

    private PlanetAPI planet(String id, boolean star, boolean gasGiant) {
        PlanetAPI planet = mock(PlanetAPI.class);
        when(planet.getId()).thenReturn(id);
        when(planet.getName()).thenReturn("Planet " + id);
        when(planet.isStar()).thenReturn(star);
        when(planet.isGasGiant()).thenReturn(gasGiant);
        when(planet.getContainingLocation()).thenReturn(system);
        when(system.getEntityById(id)).thenReturn(planet);
        planets.add(planet);
        return planet;
    }

    private AnomalyContentSite site(long seed) {
        return new AnomalyContentSite(system, RUN_ID, 4, "pirates", ChartGenContext.none(), List.of(), new Vector2f(), spot -> true,
                salt -> new Random(seed ^ salt));
    }

    private static ChartRun run() {
        return new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
    }

    @Test
    void theAnomalyIsBuiltInAndNeedsVaultsOfKnowledge() throws Exception {
        assertSame(PreCollapseFacility.INSTANCE, ChartAnomalies.byId(PreCollapseFacility.ANOMALY_ID));
        assertEquals(List.of(), ChartAnomalies.register(ChartTables.csvRows(ChartAnomalies.PATH)));

        ChartAnomalies.Definition definition = ChartAnomalies.definition(PreCollapseFacility.ANOMALY_ID);
        assertEquals(VaultsOfKnowledgeCompat.MOD_ID, definition.requiredMod());
        assertTrue(definition.insertable());
        assertFalse(ChartAnomalies.usable(PreCollapseFacility.ANOMALY_ID));
    }

    @Test
    void theFacilityGoesOnASolidPlanetNoOtherAnomalyHolds() {
        planet("sun", true, false);
        planet("giant", false, true);
        PlanetAPI dug = planet("dug", false, false);
        PlanetAPI rock = planet("rock", false, false);
        for (long seed = 0; seed < 20; seed++) {
            ChartRun run = run();
            AnomalyContentSite site = site(seed);
            site.claim(dug);

            assertEquals("rock", PreCollapseFacility.place(site, run).getId());
            assertTrue(site.isClaimed(rock));
            assertEquals("rock", FacilityStates.planetId(run));
            assertEquals(FacilityStates.Stage.GUARDED, FacilityStates.stage(run));
        }
    }

    @Test
    void gasGiantsHoldTheFacilityOnlyWhenNoSolidPlanetIsLeft() {
        planet("sun", true, false);
        planet("giant", false, true);

        assertEquals(List.of("giant"), PreCollapseFacility.candidates(site(1L)).stream().map(PlanetAPI::getId).toList());
        planets.remove(1);
        assertNull(PreCollapseFacility.place(site(1L), run()));
    }

    @Test
    void generatingWithoutARunToRecordInFails() {
        planet("rock", false, false);

        assertThrows(IllegalStateException.class, () -> PreCollapseFacility.INSTANCE.generate(site(1L)));
    }

    @Test
    void theStagesOnlyMoveForward() {
        ChartRun run = run();
        assertNull(FacilityStates.stage(run));
        assertFalse(FacilityStates.standing(run));
        FacilityStates.record(run, "rock");

        assertFalse(FacilityStates.advance(run, FacilityStates.Stage.OPEN, FacilityStates.Stage.SALVAGED));
        assertTrue(FacilityStates.advance(run, FacilityStates.Stage.GUARDED, FacilityStates.Stage.OPEN));
        assertTrue(FacilityStates.standing(run));
        assertTrue(FacilityStates.advance(run, FacilityStates.Stage.OPEN, FacilityStates.Stage.SALVAGED));
        assertFalse(FacilityStates.advance(run, FacilityStates.Stage.OPEN, FacilityStates.Stage.SALVAGED));
        assertFalse(FacilityStates.standing(run));
        assertTrue(FacilityStates.holds(run, "rock"));
        assertFalse(FacilityStates.holds(run, "stone"));
    }

    @Test
    void theDialogOpensOnTheFacilityPlanetUntilItIsSalvaged() {
        PlanetAPI rock = planet("rock", false, false);
        PlanetAPI stone = planet("stone", false, false);
        ChartRun run = run();
        ChartRuns.store(run);
        FacilityStates.record(run, "rock");

        AnomalyRun anomalyRun = new AnomalyRun(run, system);

        assertTrue(FacilityDialog.wantsToHandle(run, rock));
        assertFalse(FacilityDialog.wantsToHandle(run, stone));
        assertFalse(FacilityDialog.wantsToHandle(run, mock(CampaignFleetAPI.class)));
        String intel = PreCollapseFacility.INSTANCE.intelText(anomalyRun);
        assertTrue(intel.contains("Planet rock"), intel);

        FacilityStates.advance(run, FacilityStates.Stage.GUARDED, FacilityStates.Stage.OPEN);
        FacilityStates.advance(run, FacilityStates.Stage.OPEN, FacilityStates.Stage.SALVAGED);
        assertFalse(FacilityDialog.wantsToHandle(run, rock));
        assertNull(PreCollapseFacility.INSTANCE.intelText(anomalyRun));
    }

    @Test
    void theGuardsNeverFallBelowAoTDsFleetButGrowPastItWithTier() {
        ChartAnomalyFleets.Spec small = new ChartAnomalyFleets.Spec("remnant", "patrolLarge", 75f, 0f, 0f, 0.6f, 1, 2, 1);
        ChartAnomalyFleets.Spec floored = small.atLeast(160f);
        assertEquals(160f, floored.combatPoints());
        assertEquals(small.quality(), floored.quality());
        assertEquals(small.officerCountBonus(), floored.officerCountBonus());

        ChartAnomalyFleets.Spec large = new ChartAnomalyFleets.Spec("remnant", "patrolLarge", 450f, 0f, 0f, 0.9f, 3, 4, 2);
        assertSame(large, large.atLeast(180f));
    }

    @Test
    void databankRollsGrowWithTier() {
        assertEquals(FacilityRules.DATABANK_ROLLS, FacilityRules.databankRolls(ChartTiers.LOWEST_TIER));
        assertTrue(FacilityRules.databankRolls(ChartTiers.HIGHEST_TIER) > FacilityRules.databankRolls(ChartTiers.LOWEST_TIER));
        assertEquals(VaultsOfKnowledgeCompat.DATABANK_DROP_GROUP, FacilityLoot.databankDrops(5).get(0).group);
        assertEquals(FacilityRules.databankRolls(5), FacilityLoot.databankDrops(5).get(0).chances);
    }
}
