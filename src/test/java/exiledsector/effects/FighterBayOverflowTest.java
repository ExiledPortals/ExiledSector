package exiledsector.effects;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FighterBayOverflowTest {

    private final List<String> wings = new ArrayList<>();
    private final MutableStat bays = new MutableStat(0f);
    private FleetMemberAPI member;
    private ShipVariantAPI variant;
    private CargoAPI cargo;

    @BeforeEach
    void setUp() {
        member = mock(FleetMemberAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(member.getStats()).thenReturn(stats);
        when(stats.getNumFighterBays()).thenReturn(bays);
        variant = mock(ShipVariantAPI.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(variant.getWings()).thenReturn(wings);
        when(variant.getWingId(anyInt())).thenAnswer(call -> wings.get(call.<Integer>getArgument(0)));
        doAnswer(call -> wings.set(call.getArgument(0), call.getArgument(1))).when(variant).setWingId(anyInt(), org.mockito.ArgumentMatchers.any());
        cargo = mock(CargoAPI.class);
    }

    private void fitted(int bayCount, String... wingIds) {
        bays.setBaseValue(bayCount);
        wings.addAll(Arrays.asList(wingIds));
    }

    @Test
    void aWingInABayThatNoLongerExistsGoesBackToCargo() {
        fitted(2, "talon_wing", "broadsword_wing", "wasp_wing");

        List<String> returned = FighterBayOverflow.returnUnhousedWings(member, variant, cargo);

        assertEquals(List.of("wasp_wing"), returned);
        assertEquals(Arrays.asList("talon_wing", "broadsword_wing", null), wings);
        verify(cargo).addFighters("wasp_wing", 1);
    }

    @Test
    void emptyLostBaysAndWingsInRemainingBaysAreLeftAlone() {
        fitted(2, "talon_wing", null, null);

        assertTrue(FighterBayOverflow.returnUnhousedWings(member, variant, cargo).isEmpty());
        verify(cargo, never()).addFighters(anyString(), anyInt());
    }

    @Test
    void builtInWingsAreNeverStrippedEvenWhenTheShipLosesEveryBay() {
        fitted(0, "builtin_wing", "talon_wing");
        when(variant.getHullSpec().isBuiltInWing(0)).thenReturn(true);

        List<String> returned = FighterBayOverflow.returnUnhousedWings(member, variant, cargo);

        assertEquals(List.of("talon_wing"), returned);
        assertEquals(Arrays.asList("builtin_wing", null), wings);
    }

    @Test
    void withNoCargoToReturnItToNothingIsRemoved() {
        fitted(1, "talon_wing", "wasp_wing");

        assertTrue(FighterBayOverflow.returnUnhousedWings(member, variant, null).isEmpty());
        assertEquals(Set.of("talon_wing", "wasp_wing"), Set.copyOf(wings));
    }
}
