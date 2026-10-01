package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.skills.tags.WeaponKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcLayoutTest {

    private static final ShipProfile SHIELDED_BALLISTIC_CRUISER =
            new ShipProfile(HullSize.CRUISER, ShieldType.FRONT, 0, Set.of(WeaponKind.BALLISTIC), false, 0f, false);

    private static NpcLayout layoutRequiring(String... requires) {
        return new NpcLayout("layout", "Layout", "root", List.of(requires), "", List.of());
    }

    @Test
    void aLayoutWithoutRequirementsIsEligibleForAnyShip() {
        assertTrue(layoutRequiring().isEligible(SHIELDED_BALLISTIC_CRUISER));
    }

    @Test
    void aLayoutIsEligibleWhenEveryRequirementIsMet() {
        assertTrue(layoutRequiring("req_shields", "req_ballistic", "req_no_fighter_bays").isEligible(SHIELDED_BALLISTIC_CRUISER));
    }

    @Test
    void aLayoutIsIneligibleWhenAnyRequirementIsUnmet() {
        assertFalse(layoutRequiring("req_shields", "req_missile").isEligible(SHIELDED_BALLISTIC_CRUISER));
    }

    @Test
    void playerOnlyAndCampaignOnlyLayoutsAreNeverEligibleForNpcShips() {
        assertFalse(layoutRequiring("player_only").isEligible(SHIELDED_BALLISTIC_CRUISER));
        assertFalse(layoutRequiring("campaign_only").isEligible(SHIELDED_BALLISTIC_CRUISER));
    }

    @Test
    void listsAreDefensivelyCopied() {
        List<String> requires = new ArrayList<>(List.of("req_shields"));
        List<NpcLayoutEntry> entries = new ArrayList<>(List.of(new NpcLayoutEntry("a", null)));
        NpcLayout layout = new NpcLayout("layout", "Layout", "root", requires, "", entries);

        requires.add("req_missile");
        entries.clear();

        assertEquals(List.of("req_shields"), layout.requires());
        assertEquals(1, layout.entries().size());
    }

    @Test
    void nullListsBecomeEmpty() {
        NpcLayout layout = new NpcLayout("layout", "Layout", "root", null, "", null);

        assertTrue(layout.requires().isEmpty());
        assertTrue(layout.entries().isEmpty());
    }
}
