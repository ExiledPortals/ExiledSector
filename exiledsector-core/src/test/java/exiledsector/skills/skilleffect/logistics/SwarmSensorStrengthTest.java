package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.skilleffect.shipsystem.FragmentSwarms;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwarmSensorStrengthTest {

    private static FleetMemberAPI member(float percentPerSwarm, boolean hasSwarm, boolean mothballed) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.SWARM_SENSOR_STRENGTH_KEY, 0f)).thenReturn(percentPerSwarm);
        when(member.getVariant().hasHullMod(FragmentSwarms.HULL_MOD_ID)).thenReturn(hasSwarm);
        return member;
    }

    @Test
    void theFleetGainsTheCarriersPercentForEachActiveShipWithASwarm() {
        List<FleetMemberAPI> members = List.of(member(1f, false, false), member(0f, true, false), member(0f, true, false),
                member(0f, true, true));

        assertEquals(2f, FleetWideEffects.swarmSensorStrengthPercent(members));
    }

    @Test
    void severalCarriersCountOnceAtTheHighestValue() {
        List<FleetMemberAPI> members = List.of(member(1f, true, false), member(2f, true, false), member(0f, true, false));

        assertEquals(6f, FleetWideEffects.swarmSensorStrengthPercent(members));
    }

    @Test
    void withoutACarrierOrASwarmThereIsNoBonus() {
        assertEquals(0f, FleetWideEffects.swarmSensorStrengthPercent(List.of(member(0f, true, false))));
        assertEquals(0f, FleetWideEffects.swarmSensorStrengthPercent(List.of(member(1f, false, false))));
        assertEquals(0f, FleetWideEffects.swarmSensorStrengthPercent(List.of(member(1f, true, true))));
    }
}
