package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.MapMemory;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.scar.ScarEvidence.FiredAs;
import exiledsector.effects.SkillTreeHullMod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScarsTest {

    private final MemoryAPI memory = MapMemory.create();
    private final CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
    private final FleetMemberAPI flagship = mock(FleetMemberAPI.class);
    private final FleetMemberAPI escort = mock(FleetMemberAPI.class);
    private final ShipVariantAPI flagshipVariant = mock(ShipVariantAPI.class);
    private final ShipVariantAPI escortVariant = mock(ShipVariantAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        ScarFixtures.loadShipped();
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        when(fleet.getFleetData()).thenReturn(fleetData);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(escort, flagship));
        when(flagship.getId()).thenReturn("flagship-id");
        when(escort.getId()).thenReturn("escort-id");
        when(flagship.getVariant()).thenReturn(flagshipVariant);
        when(escort.getVariant()).thenReturn(escortVariant);
        when(flagshipVariant.getSource()).thenReturn(VariantSource.REFIT);
        when(escortVariant.getSource()).thenReturn(VariantSource.REFIT);
    }

    @Test
    void grantingScarsStoresThemAndInstallsTheTreeHullModOnTheFlagshipOnly() {
        Scars.grant(fleet, "flagship-id", Map.of("rammed", 1));
        assertEquals(Map.of("rammed", 1), Scars.scarsOf(fleet));
        assertEquals("flagship-id", Scars.flagshipIdOf(fleet));
        verify(flagshipVariant).addMod(SkillTreeHullMod.ID);
        verify(escortVariant, never()).addMod(SkillTreeHullMod.ID);
    }

    @Test
    void grantingNoScarsTracksTheFlagshipWithoutAHullMod() {
        Scars.grant(fleet, "flagship-id", Map.of());
        assertEquals("flagship-id", Scars.flagshipIdOf(fleet));
        verify(flagshipVariant, never()).addMod(SkillTreeHullMod.ID);
    }

    @Test
    void withScarAddsOneStackWithoutChangingTheOriginal() {
        Map<String, Integer> original = Map.of("rammed", 1);
        assertEquals(Map.of("rammed", 2), Scars.withScar(original, "rammed"));
        assertEquals(Map.of("rammed", 1, "kited", 1), Scars.withScar(original, "kited"));
        assertEquals(Map.of("rammed", 1), original);
    }

    @Test
    void aBattleWithoutEvidenceGivesTheFallback() {
        assertEquals(ScarTable.FALLBACK_SCAR_ID, Scars.pickAfterBattle("never-fought", Map.of(), ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void publishedEvidenceIsTakenOnce() {
        ScarEvidence evidence = new ScarEvidence();
        evidence.addFired(1000f, EnumSet.of(FiredAs.PROJECTILE, FiredAs.ENERGY));
        ScarBattleResults.publish("flagship-id", evidence);
        assertEquals("sun_scorched", Scars.pickAfterBattle("flagship-id", Map.of(), ScarFixtures.SHIELDED_WARSHIP));
        assertEquals(ScarTable.FALLBACK_SCAR_ID, Scars.pickAfterBattle("flagship-id", Map.of(), ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void aLaterEngagementAddsToTheEvidenceStillWaitingFromEarlierOnes() {
        try {
            ScarEvidence first = ScarBattleResults.pendingOrNew("flagship-id");
            first.addFired(600f, EnumSet.of(FiredAs.PROJECTILE, FiredAs.ENERGY));
            ScarBattleResults.publish("flagship-id", first);

            ScarEvidence later = ScarBattleResults.pendingOrNew("flagship-id");
            later.addFired(400f, EnumSet.of(FiredAs.PROJECTILE, FiredAs.ENERGY));

            assertSame(first, later);
            assertEquals(1f, later.share(FiredAs.ENERGY));
            assertSame(later, ScarBattleResults.take("flagship-id"));
            assertNotSame(later, ScarBattleResults.pendingOrNew("flagship-id"));
        } finally {
            ScarBattleResults.clear();
        }
    }

    @Test
    void onlyTheNewestBattlesAreKept() {
        try {
            for (int battle = 0; battle <= ScarBattleResults.MAX_KEPT; battle++) {
                ScarBattleResults.publish("flagship-" + battle, new ScarEvidence());
            }
            assertNull(ScarBattleResults.of("flagship-0"));
            assertNotNull(ScarBattleResults.of("flagship-" + ScarBattleResults.MAX_KEPT));
        } finally {
            ScarBattleResults.clear();
        }
    }

    @Test
    void runScarsJoinTheOfficersScarsOnTheirFlagshipWithoutEnteringTheirRecordOrLevel() {
        Scars.grant(fleet, "flagship-id", Map.of("rammed", 1));
        int officerLevels = OfficerFleets.flagshipExtraLevels(memory, flagship);

        Scars.grantRunScars(fleet, "flagship-id", Map.of("rammed", 1, "kited", 2));

        assertEquals("flagship-id", Scars.flagshipIdOf(fleet));
        assertEquals(Map.of("rammed", 1), Scars.scarsOf(fleet));
        assertEquals(Map.of("rammed", 2, "kited", 2), ScarMemory.scarsOn(memory, "flagship-id"));
        assertEquals(officerLevels, OfficerFleets.flagshipExtraLevels(memory, flagship));
        assertEquals(Map.of("rammed", 2, "kited", 2), ScarEffectSource.scarsOf(memberOf(flagship)));
        assertEquals(Map.of(), ScarEffectSource.scarsOf(memberOf(escort)));
    }

    @Test
    void runScarsOnAFleetWithoutAScarredFlagshipLandOnTheGivenOneAndRefreshItsStats() {
        Scars.grantRunScars(fleet, "flagship-id", Map.of("kited", 1));

        assertNull(Scars.flagshipIdOf(fleet));
        assertEquals("flagship-id", ScarMemory.runFlagshipId(memory));
        assertEquals(Map.of(), Scars.scarsOf(fleet));
        assertEquals(Map.of("kited", 1), ScarEffectSource.scarsOf(memberOf(flagship)));
        assertEquals(Map.of(), ScarEffectSource.scarsOf(memberOf(escort)));
        assertEquals(0, OfficerFleets.flagshipExtraLevels(memory, flagship));
        verify(flagshipVariant).addMod(SkillTreeHullMod.ID);
        verify(escortVariant, never()).addMod(SkillTreeHullMod.ID);
        verify(fleet.getFleetData()).setSyncNeeded();
        verify(fleet.getFleetData()).syncIfNeeded();
    }

    @Test
    void scarsWithEffectsLeaveOutTheFallbackAndThoseWithNothingForTheHull() {
        List<String> shielded = Scars.withEffectsOn(ScarFixtures.SHIELDED_WARSHIP);

        assertFalse(shielded.contains(ScarTable.FALLBACK_SCAR_ID));
        assertFalse(shielded.contains("humbled"));
        assertTrue(shielded.contains("flak_scarred"));
        assertTrue(shielded.stream().allMatch(scarId -> !ScarTable.row(scarId).fittingEffects(ScarFixtures.SHIELDED_WARSHIP).isEmpty()));
        assertTrue(Scars.withEffectsOn(null).containsAll(shielded));
    }

    private FleetMemberAPI memberOf(FleetMemberAPI member) {
        FleetDataAPI fleetData = fleet.getFleetData();
        when(member.getFleetDataForStats()).thenReturn(fleetData);
        when(fleetData.getFleet()).thenReturn(fleet);
        return member;
    }
}
