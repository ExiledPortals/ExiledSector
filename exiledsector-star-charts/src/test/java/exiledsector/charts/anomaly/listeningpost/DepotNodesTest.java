package exiledsector.charts.anomaly.listeningpost;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DepotNodesTest {

    private static final float EPSILON = 1e-4f;
    private static final Vector2f ENTRY = new Vector2f(9000f, 0f);

    private MockSector mockSector;
    private ChartRun run;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        run = new ChartRun(PostSystem.RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        ChartRuns.store(run);
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        mockSector.close();
    }

    private static ChartGenContext withLines(AppliedEffect... lines) {
        return ChartGenContext.of(List.of(lines));
    }

    private static AppliedEffect line(ChartModifier modifier, float magnitude) {
        return new AppliedEffect(modifier, magnitude);
    }

    private PostScript place(PostSystem postSystem, AppliedEffect... lines) {
        run.recordAtlasTreeLines(List.of(lines));
        PostScript script = PostSystem.placeIn(postSystem.site(withLines(lines), ENTRY, 4L), run);
        when(mockSector.playerFleet().getContainingLocation()).thenReturn(postSystem.system);
        return script;
    }

    private static void withMembers(CampaignFleetAPI fleet) {
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        List<FleetMemberAPI> members = new ArrayList<>();
        members.add(mock(FleetMemberAPI.class));
        when(fleetData.getMembersListCopy()).thenAnswer(invocation -> new ArrayList<>(members));
        doAnswer(invocation -> members.remove((FleetMemberAPI) invocation.getArgument(0))).when(fleetData).removeFleetMember(any());
        doAnswer(invocation -> members.add(invocation.getArgument(0))).when(fleetData).addFleetMember(any(FleetMemberAPI.class));
        when(fleet.getFleetData()).thenReturn(fleetData);
        doAnswer(invocation -> {
            when(fleet.isAlive()).thenReturn(false);
            return null;
        }).when(fleet).despawn(any(), any());
    }

    @Test
    void theTuningReadsEveryDepotLineAndCountsThePrototypesOnOffer() {
        DepotTuning none = DepotTuning.of(ChartGenContext.none());
        assertEquals(DepotTuning.NONE, none);
        assertEquals(VaultPrototypes.PICKS, none.prototypePicks(true));
        assertEquals(1, none.prototypePicks(false));

        DepotTuning tuned = DepotTuning.of(withLines(line(ChartModifier.CHART_LISTENING_POST_INSTANT_ALARM, 1f),
                line(ChartModifier.CHART_LISTENING_POST_UNSEEN_PROTOTYPES, 2f), line(ChartModifier.CHART_LISTENING_POST_PROTOTYPES, 1f),
                line(ChartModifier.CHART_LISTENING_POST_ARMED_STATION, 1f), line(ChartModifier.CHART_LISTENING_POST_STOLEN_CHARTS, 3f)));

        assertTrue(tuned.instantAlarm());
        assertTrue(tuned.armedPost());
        assertFalse(tuned.purgeHeld());
        assertEquals(3, tuned.maxStolenCharts());
        assertEquals(VaultPrototypes.PICKS + 3, tuned.prototypePicks(true));
        assertEquals(2, tuned.prototypePicks(false));
    }

    @Test
    void garrisonPicketsAreHalfTheBossFleet() {
        PostSystem plain = new PostSystem();
        place(plain);
        FleetParamsV3 plainPicket = plain.engine.fleetParams.get(0);

        PostSystem garrisoned = new PostSystem();
        place(garrisoned, line(ChartModifier.CHART_LISTENING_POST_GARRISON_PICKETS, 1f));
        FleetParamsV3 garrisonPicket = garrisoned.engine.fleetParams.get(0);

        assertEquals(FleetTypes.PATROL_SMALL, plainPicket.fleetType);
        assertEquals(FleetTypes.PATROL_LARGE, garrisonPicket.fleetType);
        assertTrue(garrisonPicket.combatPts > plainPicket.combatPts);
    }

    @Test
    void garrisonPicketSizeComesFromTheLargerOfTheBossAndAChartFleet() {
        exiledsector.charts.generation.ChartTier tier = exiledsector.charts.generation.ChartTiers.forTier(6);
        float expected = Math.max(tier.bossSize(), tier.fleetSize()) * ListeningPostRules.GARRISON_PICKET_BOSS_SHARE;

        assertEquals(expected, ListeningPostRules.picketSize(tier, true), EPSILON);
        assertEquals(tier.fleetSize() * ListeningPostRules.PICKET_SIZE_SHARE, ListeningPostRules.picketSize(tier, false), EPSILON);
    }

    @Test
    void hairTriggerRaisesTheAlarmTheMomentAPicketIdentifiesTheFleet() {
        PostSystem postSystem = new PostSystem();
        PostScript script = place(postSystem, line(ChartModifier.CHART_LISTENING_POST_INSTANT_ALARM, 1f));
        when(script.livingPickets().get(0).getVisibilityLevelOfPlayerFleet()).thenReturn(VisibilityLevel.COMPOSITION_DETAILS);

        script.check(mockSector.sector, run, ListeningPostRules.CHECK_DAYS);

        assertTrue(PostState.of(run).alarm());
    }

    @Test
    void huntingPicketsInterceptThePlayerAndLeaveThePostUnguarded() {
        PostSystem postSystem = new PostSystem();
        PostScript script = place(postSystem, line(ChartModifier.CHART_LISTENING_POST_HUNTING_PICKETS, 1f));
        List<CampaignFleetAPI> pickets = script.livingPickets();

        assertTrue(script.raiseAlarm(run));

        for (CampaignFleetAPI picket : pickets) {
            FakeChartContentEngine.Assignment assignment = postSystem.assignments(picket).get(0);
            assertEquals(FleetAssignment.INTERCEPT, assignment.assignment());
            assertSame(mockSector.playerFleet(), assignment.target());
            assertTrue(Pickets.isHunting(picket));
            assertTrue(picket.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE));
            assertTrue(picket.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_NO_JUMP));
        }
        assertTrue(script.hunted());
        assertFalse(script.defended());
        for (CampaignFleetAPI picket : pickets) {
            picket.setLocation(postSystem.post().getLocation().x, postSystem.post().getLocation().y);
        }
        script.check(mockSector.sector, run, ListeningPostRules.CHECK_DAYS);
        assertNull(script.guard());
        assertEquals(pickets.size(), script.livingPickets().size());
    }

    @Test
    void huntingPicketsSearchAroundThePostWhileThePlayerIsAway() {
        PostSystem postSystem = new PostSystem();
        PostScript script = place(postSystem, line(ChartModifier.CHART_LISTENING_POST_HUNTING_PICKETS, 1f));
        script.raiseAlarm(run);
        CampaignFleetAPI picket = script.livingPickets().get(0);
        when(mockSector.playerFleet().getContainingLocation()).thenReturn(null);
        picket.clearAssignments();

        script.check(mockSector.sector, run, ListeningPostRules.CHECK_DAYS);

        assertEquals(FleetAssignment.PATROL_SYSTEM, postSystem.assignments(picket).get(0).assignment());
    }

    @Test
    void aFortifiedDepotIsAnArmedStationThatHoldsTheVaultWhileItStands() {
        PostSystem postSystem = new PostSystem();
        PostScript script = place(postSystem, line(ChartModifier.CHART_LISTENING_POST_ARMED_STATION, 1f),
                line(ChartModifier.CHART_LISTENING_POST_PURGE_HELD, 1f));
        CampaignFleetAPI station = script.armedPost();
        script.livingPickets().forEach(DepotNodesTest::withMembers);
        station.setLocation(300f, 40f);

        assertNotNull(station);
        assertTrue(ArmedPost.isArmedPost(station));
        assertEquals(List.of(ArmedPost.fallbackVariant(4)), postSystem.engine.stationVariants);
        assertTrue(station.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_NO_JUMP));

        script.raiseAlarm(run);
        script.check(mockSector.sector, run, 1f);
        assertEquals(1f, PostState.of(run).vaultShare(), EPSILON);
        assertTrue(script.purgeHeld(run));
        assertSame(station, script.gatherDefenders());
        assertEquals(station.getLocation(), script.guard().getLocation());
        assertTrue(script.defended());

        when(station.isAlive()).thenReturn(false);
        script.check(mockSector.sector, run, 1f);
        assertEquals(1f - ListeningPostRules.PURGE_PER_DAY, PostState.of(run).vaultShare(), EPSILON);
    }

    @Test
    void anArmedStationWithoutThePurgeLineStillLetsTheVaultBurn() {
        PostSystem postSystem = new PostSystem();
        PostScript script = place(postSystem, line(ChartModifier.CHART_LISTENING_POST_ARMED_STATION, 1f));

        script.raiseAlarm(run);
        script.check(mockSector.sector, run, 1f);

        assertFalse(script.purgeHeld(run));
        assertEquals(1f - ListeningPostRules.PURGE_PER_DAY, PostState.of(run).vaultShare(), EPSILON);
    }

    @Test
    void fallbackStationsFollowTheBossStationTierBands() {
        assertEquals("station1_midline_Standard", ArmedPost.fallbackVariant(1));
        assertEquals("station1_midline_Standard", ArmedPost.fallbackVariant(5));
        assertEquals("station2_midline_Standard", ArmedPost.fallbackVariant(6));
        assertEquals("station3_midline_Standard", ArmedPost.fallbackVariant(11));
        assertEquals("station3_midline_Standard", ArmedPost.fallbackVariant(16));
    }

    @Test
    void theAlarmKeyNamesAFortifiedDepotWhenTheVaultIsHeld() {
        assertEquals("chart.anomaly.listening_post.dialog.purged", PostDialog.alarmKey(false, true));
        assertEquals("chart.anomaly.listening_post.dialog.fortified", PostDialog.alarmKey(true, true));
        assertEquals("chart.anomaly.listening_post.dialog.guarded", PostDialog.alarmKey(true, false));
    }
}
