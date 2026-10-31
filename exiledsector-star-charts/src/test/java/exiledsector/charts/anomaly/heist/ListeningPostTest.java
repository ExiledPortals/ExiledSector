package exiledsector.charts.anomaly.heist;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartFleetVisibility;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

class ListeningPostTest {

    private MockSector mockSector;
    private PostSystem postSystem;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        postSystem = new PostSystem();
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        mockSector.close();
    }

    private static ChartRun run() {
        return new ChartRun(PostSystem.RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
    }

    @Test
    void theAnomalyIsBuiltInAndInsertable() throws Exception {
        assertSame(ListeningPost.INSTANCE, ChartAnomalies.byId(ListeningPost.ANOMALY_ID));
        ChartAnomalies.register(ChartTables.csvRows(ChartAnomalies.PATH));
        assertTrue(ChartAnomalies.definition(ListeningPost.ANOMALY_ID).insertable());
    }

    @Test
    void thePostSitsAwayFromTheWormholeBehindARingOfPickets() {
        Vector2f entry = new Vector2f(9000f, 0f);
        for (long seed = 0; seed < 20; seed++) {
            postSystem.scripts.clear();
            ChartRun run = run();

            PostScript script = PostSystem.placeIn(postSystem.site(ChartGenContext.none(), entry, seed), run);

            SectorEntityToken post = postSystem.post();
            assertNotNull(post);
            assertTrue(ListeningPost.isPost(post));
            assertTrue(post.hasTag(ChartIds.runTag(PostSystem.RUN_ID)));
            float radius = post.getLocation().length();
            assertTrue(radius >= HeistRules.POST_MIN_RADIUS && radius <= HeistRules.POST_MAX_RADIUS, String.valueOf(radius));
            List<CampaignFleetAPI> pickets = script.livingPickets();
            assertTrue(pickets.size() >= HeistRules.MIN_PICKETS && pickets.size() <= HeistRules.MAX_PICKETS, String.valueOf(pickets.size()));
            for (CampaignFleetAPI picket : pickets) {
                float gap = Vector2f.sub(picket.getLocation(), post.getLocation(), new Vector2f()).length();
                assertTrue(Math.abs(gap - HeistRules.RING_RADIUS) <= HeistRules.RING_JITTER + 1f, String.valueOf(gap));
            }
            PostState state = PostState.of(run);
            assertTrue(state.quiet());
            assertEquals(1f, state.vaultShare());
            assertEquals(0f, state.suspicion());
        }
    }

    @Test
    void thePostPrefersAnOrbitFarFromTheWormhole() {
        Vector2f entry = new Vector2f(5000f, 0f);
        int far = 0;
        for (long seed = 0; seed < 40; seed++) {
            postSystem.scripts.clear();
            PostSystem.placeIn(postSystem.site(ChartGenContext.none(), entry, seed), run());
            if (Vector2f.sub(postSystem.post().getLocation(), entry, new Vector2f()).length() >= HeistRules.POST_MIN_ENTRY_GAP) {
                far++;
            }
        }
        assertTrue(far >= 38, String.valueOf(far));
    }

    @Test
    void picketsHoldStationQuietlyAndShowOnTheMap() {
        PostSystem.placeIn(postSystem.site(ChartGenContext.none(), new Vector2f(9000f, 0f), 3L), run());

        for (CampaignFleetAPI picket : postSystem.pickets()) {
            MemoryAPI memory = picket.getMemoryWithoutUpdate();
            assertTrue(memory.getBoolean(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE));
            assertFalse(memory.getBoolean(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE));
            assertFalse(memory.getBoolean(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT));
            assertTrue(memory.getBoolean(MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED));
            assertTrue(Pickets.isPicket(picket));
            List<FakeChartContentEngine.Assignment> assignments = postSystem.assignments(picket);
            assertEquals(1, assignments.size());
            assertEquals(FleetAssignment.ORBIT_PASSIVE, assignments.get(0).assignment());
            verify(picket).setExtendedDetectedAtRange(ChartFleetVisibility.REVEAL_EXTENDED_RANGE);
        }
    }

    @Test
    void thePostAndItsPicketsFlyTheSystemsFactionHoweverTheAnomalyArrived() {
        PostSystem.placeIn(postSystem.site(ChartGenContext.none(), new Vector2f(9000f, 0f), 5L), run());

        verify(postSystem.system).addCustomEntity(ListeningPost.postId(PostSystem.RUN_ID), "Listening Post", ListeningPost.ENTITY_TYPE, "pirates");
        assertFalse(postSystem.engine.fleetParams.isEmpty());
        postSystem.engine.fleetParams.forEach(params -> assertEquals("pirates", params.factionId));
    }

    @Test
    void thePicketsLineAddsPickets() {
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_LISTENING_POST_PICKETS, 2f)));
        for (long seed = 0; seed < 10; seed++) {
            postSystem.scripts.clear();
            PostScript script = PostSystem.placeIn(postSystem.site(context, new Vector2f(9000f, 0f), seed), run());
            int count = script.livingPickets().size();
            assertTrue(count >= HeistRules.MIN_PICKETS + 2 && count <= HeistRules.MAX_PICKETS + 2, String.valueOf(count));
        }
    }

    @Test
    void generatingAddsTheScriptAndKeepsTheAnomalyActiveUntilTheVaultIsTaken() {
        ChartRun run = run();
        AnomalyContentSite site = postSystem.site(ChartGenContext.none(), new Vector2f(9000f, 0f), 1L);

        ListeningPost.INSTANCE.generate(site);
        site.commitStates(run);

        AnomalyRun anomalyRun = new AnomalyRun(run, postSystem.system);
        assertTrue(ListeningPost.INSTANCE.intelText(anomalyRun).contains("Suspicion: 0%"));
        PostState.record(run, PostState.of(run).asTaken());
        assertNull(ListeningPost.INSTANCE.intelText(anomalyRun));
    }

    @Test
    void stateSurvivesEncodingAndRejectsBrokenText() {
        PostState state = new PostState(0.375f, true, 0.5f, false, Long.MIN_VALUE);

        assertEquals(state, PostState.decode(state.encode()));
        assertNull(PostState.decode("0.5,1,0.5"));
        assertNull(PostState.decode("x,1,0.5,0,1"));
        assertTrue(new PostState(0f, false, 1f, true, 1L).clean());
        assertFalse(new PostState(0f, true, 1f, true, 1L).clean());
    }
}
