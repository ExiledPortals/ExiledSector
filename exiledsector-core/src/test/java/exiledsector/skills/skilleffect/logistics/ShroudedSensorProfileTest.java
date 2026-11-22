package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShroudedSensorProfileTest {

    private static FleetMemberAPI member(float percentPerShip, boolean mothballed) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.SHROUDED_SENSOR_PROFILE_KEY, 0f)).thenReturn(percentPerShip);
        return member;
    }

    private static Predicate<FleetMemberAPI> shroudedAre(FleetMemberAPI... shrouded) {
        return Set.of(shrouded)::contains;
    }

    @Test
    void theFleetLosesTheStrongestCarriersPercentForEachActiveShroudedShip() {
        FleetMemberAPI carrier = member(-2f, false);
        FleetMemberAPI strongerCarrier = member(-3f, false);
        FleetMemberAPI shrouded = member(0f, false);
        FleetMemberAPI mothballed = member(0f, true);

        assertEquals(-6f, FleetWideEffects.shroudedSensorProfilePercent(List.of(carrier, strongerCarrier, shrouded, mothballed),
                shroudedAre(carrier, shrouded, mothballed)));
    }

    @Test
    void theReductionStopsAtItsMaximum() {
        List<FleetMemberAPI> members = new ArrayList<>(List.of(member(-3f, false)));
        for (int ship = 0; ship < 12; ship++) {
            members.add(member(0f, false));
        }

        assertEquals(-FleetWideEffects.MAX_SHROUDED_SENSOR_PROFILE_REDUCTION,
                FleetWideEffects.shroudedSensorProfilePercent(members, member -> true));
    }

    @Test
    void withoutACarrierOrAShroudedShipThereIsNoReduction() {
        assertEquals(0f, FleetWideEffects.shroudedSensorProfilePercent(List.of(member(0f, false)), member -> true));
        assertEquals(0f, FleetWideEffects.shroudedSensorProfilePercent(List.of(member(-3f, false)), member -> false));
        assertEquals(0f, FleetWideEffects.shroudedSensorProfilePercent(List.of(member(-3f, true)), member -> true));
    }
}
