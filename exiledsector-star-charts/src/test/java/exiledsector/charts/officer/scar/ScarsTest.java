package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.MapMemory;
import exiledsector.charts.officer.scar.ScarEvidence.FiredAs;
import exiledsector.effects.SkillTreeHullMod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
}
