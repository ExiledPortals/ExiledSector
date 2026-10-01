package exiledsector.skills.tags;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShipProfileTest {

    private static FleetMemberAPI member(HullSize hullSize, ShieldType shieldType, int fighterBays,
                                         ShipVariantAPI variant, boolean flagship) {
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullSize()).thenReturn(hullSize);
        when(hullSpec.getShieldType()).thenReturn(shieldType);
        when(hullSpec.getFighterBays()).thenReturn(fighterBays);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getHullSpec()).thenReturn(hullSpec);
        when(member.getVariant()).thenReturn(variant);
        when(member.isFlagship()).thenReturn(flagship);
        return member;
    }

    private static WeaponSpecAPI weapon(WeaponType type, boolean beam) {
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        when(spec.getType()).thenReturn(type);
        when(spec.isBeam()).thenReturn(beam);
        return spec;
    }

    private static WeaponSpecAPI beam(WeaponType type, AIHints first, AIHints... rest) {
        WeaponSpecAPI spec = weapon(type, true);
        when(spec.getAIHints()).thenReturn(EnumSet.of(first, rest));
        return spec;
    }

    private static ShipVariantAPI variant(WeaponSpecAPI... specs) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        List<String> slotIds = new ArrayList<>();
        for (int i = 0; i < specs.length; i++) {
            String slotId = "WS000" + i;
            slotIds.add(slotId);
            when(variant.getWeaponSpec(slotId)).thenReturn(specs[i]);
        }
        when(variant.getFittedWeaponSlots()).thenReturn(slotIds);
        return variant;
    }

    private static FleetMemberAPI withFlightDecks(FleetMemberAPI member, int flightDecks) {
        when(member.getStats()).thenReturn(mock(MutableShipStatsAPI.class));
        when(member.getNumFlightDecks()).thenReturn(flightDecks);
        return member;
    }

    private static Set<WeaponKind> kindsOf(ShipVariantAPI variant) {
        return ShipProfile.of(member(HullSize.CRUISER, ShieldType.FRONT, 0, variant, false)).weaponKinds();
    }

    @Test
    void readsHullSizeAndShieldTypeFromTheHullSpecAndFighterBaysAndFlagshipFromTheMember() {
        ShipProfile profile = ShipProfile.of(withFlightDecks(member(HullSize.CAPITAL_SHIP, ShieldType.PHASE, 3, variant(), true), 3));

        assertEquals(HullSize.CAPITAL_SHIP, profile.hullSize());
        assertEquals(ShieldType.PHASE, profile.shieldType());
        assertEquals(3, profile.fighterBays());
        assertTrue(profile.flagship());
        assertTrue(profile.weaponKinds().isEmpty());
    }

    @Test
    void phaseAndBaseArmorComeFromTheHullSpecNotTheDefenceType() {
        FleetMemberAPI damperShip = member(HullSize.FRIGATE, ShieldType.PHASE, 0, variant(), false);
        when(damperShip.getHullSpec().getArmorRating()).thenReturn(600f);
        FleetMemberAPI phaseShip = member(HullSize.FRIGATE, ShieldType.PHASE, 0, variant(), false);
        when(phaseShip.getHullSpec().isPhase()).thenReturn(true);

        ShipProfile damper = ShipProfile.of(damperShip);

        assertFalse(damper.phaseHull());
        assertEquals(600f, damper.baseArmor());
        assertTrue(ShipProfile.of(phaseShip).phaseHull());
    }

    @Test
    void fighterBaysAddedByHullmodsCountEvenWhenTheHullHasNone() {
        ShipProfile profile = ShipProfile.of(withFlightDecks(member(HullSize.CRUISER, ShieldType.FRONT, 0, variant(), false), 1));

        assertEquals(1, profile.fighterBays());
        assertTrue(NodeRequirements.isSatisfiedBy(List.of("req_fighter_bays"), profile));
    }

    @Test
    void fighterBaysRemovedByHullmodsAreNotCounted() {
        ShipProfile profile = ShipProfile.of(withFlightDecks(member(HullSize.CRUISER, ShieldType.FRONT, 2, variant(), false), 0));

        assertEquals(0, profile.fighterBays());
    }

    @Test
    void fighterBaysFallBackToTheHullSpecWhenTheMemberHasNoStats() {
        assertEquals(2, ShipProfile.of(member(HullSize.CRUISER, ShieldType.FRONT, 2, variant(), false)).fighterBays());
    }

    @Test
    void aNonFlagshipIsReportedAsSuch() {
        assertFalse(ShipProfile.of(member(HullSize.FRIGATE, ShieldType.NONE, 0, variant(), false)).flagship());
    }

    @Test
    void collectsEveryFittedWeaponKind() {
        ShipVariantAPI variant = variant(weapon(WeaponType.BALLISTIC, false), weapon(WeaponType.MISSILE, false),
                weapon(WeaponType.ENERGY, true), weapon(WeaponType.ENERGY, false));

        assertEquals(EnumSet.allOf(WeaponKind.class), kindsOf(variant));
    }

    @Test
    void anEnergyBeamCountsAsEnergyAndAnOffensiveBeamButNotNonBeamEnergy() {
        assertEquals(EnumSet.of(WeaponKind.ENERGY, WeaponKind.BEAM, WeaponKind.OFFENSIVE_BEAM),
                kindsOf(variant(weapon(WeaponType.ENERGY, true))));
    }

    @Test
    void aPointDefenseBeamCountsAsABeamButNotAnOffensiveBeam() {
        assertEquals(EnumSet.of(WeaponKind.ENERGY, WeaponKind.BEAM),
                kindsOf(variant(beam(WeaponType.ENERGY, AIHints.PD), beam(WeaponType.ENERGY, AIHints.PD_ONLY, AIHints.ANTI_FTR))));
    }

    @Test
    void aBeamThatCanAlsoShootMissilesIsStillAnOffensiveBeam() {
        assertEquals(EnumSet.of(WeaponKind.ENERGY, WeaponKind.BEAM, WeaponKind.OFFENSIVE_BEAM),
                kindsOf(variant(beam(WeaponType.ENERGY, AIHints.PD_ALSO, AIHints.USE_VS_FRIGATES))));
    }

    @Test
    void anOffensiveBeamAlongsidePointDefenseBeamsMakesTheShipAnOffensiveBeamShip() {
        ShipVariantAPI variant = variant(beam(WeaponType.ENERGY, AIHints.PD), beam(WeaponType.ENERGY, AIHints.STRIKE));

        assertTrue(kindsOf(variant).contains(WeaponKind.OFFENSIVE_BEAM));
    }

    @Test
    void aNonBeamEnergyWeaponCountsAsEnergyAndNonBeamEnergy() {
        assertEquals(EnumSet.of(WeaponKind.ENERGY, WeaponKind.NON_BEAM_ENERGY),
                kindsOf(variant(weapon(WeaponType.ENERGY, false))));
    }

    @Test
    void aBeamOfAnotherWeaponTypeStillCountsAsABeam() {
        assertEquals(EnumSet.of(WeaponKind.BALLISTIC, WeaponKind.BEAM, WeaponKind.OFFENSIVE_BEAM),
                kindsOf(variant(weapon(WeaponType.BALLISTIC, true))));
    }

    @Test
    void ignoresNonCombatWeaponTypesAndSlotsWithoutASpec() {
        ShipVariantAPI variant = variant(weapon(WeaponType.DECORATIVE, false), weapon(WeaponType.SYSTEM, false),
                weapon(WeaponType.DECORATIVE, true), weapon(WeaponType.SYSTEM, true), null);

        assertTrue(kindsOf(variant).isEmpty());
    }

    @Test
    void aMissingVariantMeansNoWeaponKinds() {
        assertTrue(kindsOf(null).isEmpty());
    }

    @Test
    void weaponKindsAreNeverNullAndCannotBeModified() {
        ShipProfile profile = new ShipProfile(HullSize.FRIGATE, ShieldType.NONE, 0, null, false, 0f, false);

        assertTrue(profile.weaponKinds().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> profile.weaponKinds().add(WeaponKind.MISSILE));
    }
}
