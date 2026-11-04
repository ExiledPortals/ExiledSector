package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoveSlowBurnLevelTest {

    private static FleetMemberAPI member(float contribution, boolean mothballed) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.MOVE_SLOW_BURN_LEVEL_KEY, 0f)).thenReturn(contribution);
        return member;
    }

    @Test
    void eachActiveShipAddsItsContributionAndMothballedShipsAddNothing() {
        assertEquals(2f, FleetWideEffects.moveSlowBurnLevelBonus(List.of(member(1f, false), member(1f, false), member(1f, true), member(0f, false))));
    }

    @Test
    void theFleetBonusStopsAtTheMaximum() {
        List<FleetMemberAPI> members = List.of(member(1f, false), member(1f, false), member(1f, false), member(1f, false),
                member(1f, false), member(1f, false), member(1f, false), member(1f, false));

        assertEquals(FleetWideEffects.MAX_MOVE_SLOW_BURN_LEVEL_BONUS, FleetWideEffects.moveSlowBurnLevelBonus(members));
    }
}
