package exiledsector.skills.skilleffect.shipsystem;

import com.fs.starfarer.api.combat.ShipVariantAPI;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FragmentSwarmsTest {

    private static SwarmMember fragment(boolean fadingOut) {
        SwarmMember member = mock(SwarmMember.class);
        member.fader = new FaderUtil(1f, 1f);
        if (fadingOut) {
            member.fader.fadeOut();
        }
        return member;
    }

    private static RoilingSwarmEffect swarm(int maintained, int active, int fadingOut) {
        List<SwarmMember> members = new ArrayList<>();
        for (int i = 0; i < active; i++) {
            members.add(fragment(false));
        }
        for (int i = 0; i < fadingOut; i++) {
            members.add(fragment(true));
        }
        RoilingSwarmEffect swarm = mock(RoilingSwarmEffect.class);
        when(swarm.getMembers()).thenReturn(members);
        when(swarm.getNumMembersToMaintain()).thenReturn(maintained);
        return swarm;
    }

    @Test
    void fragmentsFadingOutCannotBeSpent() {
        RoilingSwarmEffect swarm = swarm(20, 2, 3);

        assertTrue(FragmentSwarms.hasFragments(swarm, 2));
        assertFalse(FragmentSwarms.hasFragments(swarm, 3));
        assertFalse(FragmentSwarms.hasFragments(null, 1));
    }

    @Test
    void spendingTakesFragmentsOnlyWhenTheSwarmHasEnough() {
        RoilingSwarmEffect swarm = swarm(20, 4, 0);

        assertTrue(FragmentSwarms.spend(swarm, 3));
        verify(swarm).despawnMembers(3);

        RoilingSwarmEffect thin = swarm(20, 2, 0);
        assertFalse(FragmentSwarms.spend(thin, 3));
        verify(thin, never()).despawnMembers(anyInt());
    }

    @Test
    void regrowingNeverFillsTheSwarmAboveItsMaintainedSize() {
        RoilingSwarmEffect swarm = swarm(10, 8, 0);

        assertEquals(2, FragmentSwarms.regrow(swarm, 5));
        verify(swarm).addMembers(2);

        RoilingSwarmEffect full = swarm(10, 10, 0);
        assertEquals(0, FragmentSwarms.regrow(full, 5));
        verify(full, never()).addMembers(anyInt());
    }

    @Test
    void fullnessCountsEveryMemberAgainstTheMaintainedSize() {
        assertTrue(FragmentSwarms.isFullTo(swarm(10, 9, 0), 0.9f));
        assertFalse(FragmentSwarms.isFullTo(swarm(10, 8, 0), 0.9f));
        assertTrue(FragmentSwarms.isFull(swarm(10, 9, 1)));
        assertFalse(FragmentSwarms.isFull(null));
    }

    @Test
    void aShipHasASwarmWhenItsVariantCarriesTheFragmentSwarmHullMod() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.hasHullMod(FragmentSwarms.HULL_MOD_ID)).thenReturn(true);

        assertTrue(FragmentSwarms.hasSwarmHullMod(variant));
        assertFalse(FragmentSwarms.hasSwarmHullMod(mock(ShipVariantAPI.class)));
        assertFalse(FragmentSwarms.hasSwarmHullMod(null));
    }
}
