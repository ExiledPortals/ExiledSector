package exiledsector.skills.skilleffect.shipsystem;

import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect.SwarmMember;
import com.fs.starfarer.api.util.FaderUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FragmentCoverTest {

    private static RoilingSwarmEffect swarmWith(int fragments) {
        List<SwarmMember> members = new ArrayList<>();
        for (int i = 0; i < fragments; i++) {
            SwarmMember member = mock(SwarmMember.class);
            member.fader = new FaderUtil(1f, 1f);
            members.add(member);
        }
        RoilingSwarmEffect swarm = mock(RoilingSwarmEffect.class);
        when(swarm.getMembers()).thenReturn(members);
        doAnswer(call -> {
            int despawned = call.getArgument(0);
            members.subList(0, despawned).clear();
            return null;
        }).when(swarm).despawnMembers(anyInt());
        return swarm;
    }

    @Test
    void theCoveredAmountIsWhatTheReductionKeptOffThePaidAmount() {
        assertEquals(15f, FragmentCover.coveredAmount(85f, 15f), 0.001f);
        assertEquals(10f, FragmentCover.coveredAmount(90f, 10f), 0.001f);
        assertEquals(50f, FragmentCover.coveredAmount(50f, 50f), 0.001f);
        assertEquals(25f, FragmentCover.coveredAmount(75f, 25f), 0.001f);
    }

    @Test
    void noReductionCoversNothing() {
        assertEquals(0f, FragmentCover.coveredAmount(100f, 0f));
        assertEquals(0f, FragmentCover.coveredAmount(100f, 100f));
    }

    @Test
    void coverSavesUpAndSpendsOneFragmentPerFullShare() {
        RoilingSwarmEffect swarm = swarmWith(5);
        FragmentCover cover = new FragmentCover(30f);

        assertTrue(cover.charge(swarm, 20f, 50f));
        verify(swarm, never()).despawnMembers(anyInt());
        assertTrue(cover.charge(swarm, 45f, 50f));
        verify(swarm, times(2)).despawnMembers(1);
        assertEquals(3, swarm.getMembers().size());
    }

    @Test
    void coverStopsAndForgetsItsSavingsOnceTheSwarmRunsOut() {
        RoilingSwarmEffect swarm = swarmWith(1);
        FragmentCover cover = new FragmentCover(30f);

        assertFalse(cover.charge(swarm, 70f, 50f));
        assertTrue(swarm.getMembers().isEmpty());
        assertTrue(cover.charge(swarmWith(0), 29f, 50f));
    }
}
