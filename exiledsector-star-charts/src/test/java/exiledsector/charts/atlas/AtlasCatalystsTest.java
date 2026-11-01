package exiledsector.charts.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import exiledsector.MockSector;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.facility.PreCollapseFacility;
import exiledsector.charts.anomaly.hypershunt.CoronalHypershunt;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AtlasCatalystsTest {

    private static final String FACILITY_SURVEY = "exiledSector_facility_survey";
    private static final String HYPERSHUNT_SURVEY = "exiledSector_hypershunt_survey";

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadAnomalies();
    }

    @AfterEach
    void tearDown() {
        AtlasCatalysts.resetForTests();
        ChartTables.resetAnomalies();
    }

    @Test
    void theShippedTableLoadsEveryCatalystWhoseAnomalyCanBeInsertedAndSkipsTheRest() throws Exception {
        List<String> problems = AtlasCatalysts.register(ChartTables.csvRows(AtlasCatalysts.PATH));

        assertEquals(List.of(), problems);
        assertEquals(Set.of("spatial_breach", "asteroid_storm", "magnetic_storm", "stellar_flares", "anomalous_debris", "excavation_sites",
                "relay_network", "supply_convoy", "storm_front", "derelict_forge", "listening_post", "faction_skirmish", "powerful_boss"), Set.copyOf(AtlasCatalysts.all().stream().filter(catalyst -> catalyst.effect() == AtlasCatalysts.Effect.ANOMALY)
                .map(AtlasCatalysts.Catalyst::anomalyId).toList()));
        AtlasCatalysts.Catalyst redeployment = AtlasCatalysts.byItemId("exiledSector_redeployment_orders");
        assertNotNull(redeployment);
        assertEquals(AtlasCatalysts.Effect.REDEPLOY, redeployment.effect());
        assertEquals(AtlasCatalysts.Family.ORDERS, redeployment.family());
        assertEquals("graphics/icons/catalysts/redeployment_orders.png", redeployment.iconPath());
        JSONArray specialItems = ChartTables.csvRows("data/campaign/special_items.csv");
        Set<String> specialItemIds = new HashSet<>();
        for (int index = 0; index < specialItems.length(); index++) {
            specialItemIds.add(specialItems.getJSONObject(index).optString("id"));
        }
        for (AtlasCatalysts.Catalyst catalyst : AtlasCatalysts.all()) {
            assertTrue(specialItemIds.contains(catalyst.itemId()), catalyst.itemId() + " needs a special_items.csv row");
        }
        AtlasCatalysts.Catalyst breach = AtlasCatalysts.byItemId("exiledSector_breach_terminus");
        assertNotNull(breach);
        assertEquals(AtlasCatalysts.Family.TERMINUS, breach.family());
        assertEquals(ChartAnomalies.definition("spatial_breach").icon(), breach.iconPath());
        assertEquals(AtlasCatalysts.Family.SURVEY, AtlasCatalysts.byItemId("exiledSector_relay_survey").family());
        AtlasCatalysts.Catalyst picket = AtlasCatalysts.byItemId("exiledSector_picket_orders");
        assertNotNull(picket);
        assertEquals(AtlasCatalysts.Family.ORDERS, picket.family());
        assertEquals("listening_post", picket.anomalyId());
        assertEquals(ChartAnomalies.definition("listening_post").icon(), picket.iconPath());
        AtlasCatalysts.Catalyst flagship = AtlasCatalysts.byItemId("exiledSector_flagship_orders");
        assertNotNull(flagship);
        assertEquals(AtlasCatalysts.Family.ORDERS, flagship.family());
        assertEquals("powerful_boss", flagship.anomalyId());
        assertEquals("graphics/icons/anomalies/powerful_boss.png", flagship.iconPath());
    }

    @Test
    void theFacilitySurveyLoadsOnlyWithVaultsOfKnowledgeAndNeverDrops() throws Exception {
        assertEquals(List.of(), AtlasCatalysts.register(ChartTables.csvRows(AtlasCatalysts.PATH)));
        assertNull(AtlasCatalysts.byItemId(FACILITY_SURVEY));

        SettingsAPI settings = mock(SettingsAPI.class);
        ModManagerAPI modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(modManager.isModEnabled(VaultsOfKnowledgeCompat.MOD_ID)).thenReturn(true);
        try (MockSector mockSector = new MockSector()) {
            mockSector.global.when(Global::getSettings).thenReturn(settings);

            assertEquals(List.of(), AtlasCatalysts.register(ChartTables.csvRows(AtlasCatalysts.PATH)));
            AtlasCatalysts.Catalyst survey = AtlasCatalysts.byItemId(FACILITY_SURVEY);
            assertNotNull(survey);
            assertEquals(PreCollapseFacility.ANOMALY_ID, survey.anomalyId());
            assertEquals(0f, survey.weight());
            Random random = new Random(5L);
            for (int roll = 0; roll < 2000; roll++) {
                AtlasCatalysts.Catalyst dropped = AtlasCatalysts.pickDrop(ChartTiers.HIGHEST_TIER, random);
                assertTrue(dropped == null || !FACILITY_SURVEY.equals(dropped.itemId()));
            }
        }
    }

    @Test
    void theHypershuntSurveyForcesItsStarAnomalyAndNeverDrops() throws Exception {
        assertEquals(List.of(), AtlasCatalysts.register(ChartTables.csvRows(AtlasCatalysts.PATH)));

        AtlasCatalysts.Catalyst survey = AtlasCatalysts.byItemId(HYPERSHUNT_SURVEY);
        assertNotNull(survey);
        assertEquals(AtlasCatalysts.Effect.STAR, survey.effect());
        assertEquals(AtlasCatalysts.Family.SURVEY, survey.family());
        assertEquals(CoronalHypershunt.ANOMALY_ID, survey.anomalyId());
        assertEquals(ChartAnomalies.definition(CoronalHypershunt.ANOMALY_ID).icon(), survey.iconPath());
        assertEquals(0f, survey.weight());
        Random random = new Random(7L);
        for (int roll = 0; roll < 2000; roll++) {
            AtlasCatalysts.Catalyst dropped = AtlasCatalysts.pickDrop(ChartTiers.HIGHEST_TIER, random);
            assertTrue(dropped == null || !HYPERSHUNT_SURVEY.equals(dropped.itemId()));
        }
    }

    @Test
    void aStarCatalystNeedsAStarAnomalyAndNeverDrops() throws Exception {
        JSONArray rows = new JSONArray()
                .put(new JSONObject().put("id", "test_not_star").put("anomaly", "spatial_breach").put("family", "terminus").put("effect", "star"))
                .put(new JSONObject().put("id", "test_star").put("anomaly", CoronalHypershunt.ANOMALY_ID).put("family", "survey").put("effect", "star")
                        .put("weight", 1));

        List<String> problems = AtlasCatalysts.register(rows);

        assertEquals(List.of("test_not_star skipped: anomaly 'spatial_breach' does not replace the star",
                "test_star weight = '1.0 (a star catalyst never drops)'"), problems);
        assertEquals(0f, AtlasCatalysts.byItemId("test_star").weight());
        assertNull(AtlasCatalysts.byItemId("test_not_star"));
    }

    @Test
    void aRowWithAnUnknownFamilyIsSkipped() throws Exception {
        JSONArray rows = new JSONArray().put(new JSONObject().put("id", "test_catalyst").put("anomaly", "spatial_breach").put("family", "relic"));

        List<String> problems = AtlasCatalysts.register(rows);

        assertEquals(List.of("test_catalyst skipped: family 'relic' is not terminus, survey or orders"), problems);
        assertNull(AtlasCatalysts.byItemId("test_catalyst"));
    }

    @Test
    void aRowWhoseAnomalyHasNoIconIsSkipped() throws Exception {
        ChartAnomalies.useForTests(List.of(new ChartAnomalies.Definition("spatial_breach", 1f, 1, 15, "", "", true, Set.of(), false, List.of())));
        JSONArray rows = new JSONArray().put(new JSONObject().put("id", "test_catalyst").put("anomaly", "spatial_breach").put("family", "terminus"));

        List<String> problems = AtlasCatalysts.register(rows);

        assertEquals(List.of("test_catalyst skipped: anomaly 'spatial_breach' has no icon"), problems);
        assertNull(AtlasCatalysts.byItemId("test_catalyst"));
    }

    @Test
    void aRedeployCatalystNeedsAnIconAndAnUnknownEffectIsSkipped() throws Exception {
        JSONArray rows = new JSONArray()
                .put(new JSONObject().put("id", "test_iconless").put("family", "orders").put("effect", "redeploy"))
                .put(new JSONObject().put("id", "test_unknown").put("anomaly", "spatial_breach").put("family", "terminus").put("effect", "teleport"))
                .put(new JSONObject().put("id", "test_redeploy").put("family", "orders").put("effect", "redeploy").put("icon", "graphics/test.png"));

        List<String> problems = AtlasCatalysts.register(rows);

        assertEquals(List.of("test_iconless skipped: a redeploy catalyst needs an icon",
                "test_unknown skipped: effect 'teleport' is not anomaly, star or redeploy"), problems);
        assertEquals(List.of("test_redeploy"), AtlasCatalysts.all().stream().map(AtlasCatalysts.Catalyst::itemId).toList());
    }
}
