package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.skilleffect.defense.DefenseSkillEffect;
import exiledsector.skills.skilleffect.shield.ShieldSkillEffect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatedStatEffectTest {

    private static MutableShipStatsAPI captainedBy(PersonAPI captain) {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        when(member.getCaptain()).thenReturn(captain);
        return stats;
    }

    private static PersonAPI person(boolean isDefault) {
        PersonAPI person = mock(PersonAPI.class);
        when(person.isDefault()).thenReturn(isDefault);
        return person;
    }

    private static MutableShipStatsAPI hullWithShieldDamage(boolean phase, MutableStat shieldDamage) {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(stats.getVariant()).thenReturn(variant);
        when(variant.getHullSpec()).thenReturn(hullSpec);
        when(hullSpec.isPhase()).thenReturn(phase);
        when(stats.getShieldDamageTakenMult()).thenReturn(shieldDamage);
        when(stats.getArmorDamageTakenMult()).thenReturn(new MutableStat(1f));
        when(stats.getHullDamageTakenMult()).thenReturn(new MutableStat(1f));
        return stats;
    }

    @Test
    void aShipHasNoCaptainWithoutAnOfficerOrWithTheDefaultOne() {
        assertTrue(ShipCaptain.absent(captainedBy(null)));
        assertTrue(ShipCaptain.absent(captainedBy(person(true))));
        assertFalse(ShipCaptain.absent(captainedBy(person(false))));
        assertTrue(ShipCaptain.absent(mock(MutableShipStatsAPI.class)));
    }

    @Test
    void theLiveShipsCaptainWinsOverTheFleetMembers() {
        MutableShipStatsAPI stats = captainedBy(person(true));
        ShipAPI ship = mock(ShipAPI.class);
        PersonAPI officer = person(false);
        when(stats.getEntity()).thenReturn(ship);
        when(ship.getCaptain()).thenReturn(officer);

        assertFalse(ShipCaptain.absent(stats));
    }

    @Test
    void captainlessEffectsApplyOnlyWithoutACaptain() {
        MutableStat shieldDamage = new MutableStat(1f);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        when(stats.getShieldDamageTakenMult()).thenReturn(shieldDamage);
        PersonAPI officer = person(false);
        when(member.getCaptain()).thenReturn(officer);

        ShieldSkillEffect.SHIELD_DAMAGE_TAKEN_WITHOUT_CAPTAIN_MULT.apply(stats, "mod_id", -15f);
        assertEquals(1f, shieldDamage.getModifiedValue(), 0.001f);

        when(member.getCaptain()).thenReturn(null);
        ShieldSkillEffect.SHIELD_DAMAGE_TAKEN_WITHOUT_CAPTAIN_MULT.apply(stats, "mod_id", -15f);
        assertEquals(0.85f, shieldDamage.getModifiedValue(), 0.001f);
    }

    @Test
    void phaseCloakEffectsSkipPhaseHulls() {
        MutableStat phaseShield = new MutableStat(1f);
        DefenseSkillEffect.DAMAGE_TAKEN_WITHOUT_PHASE_CLOAK_MULT.apply(hullWithShieldDamage(true, phaseShield), "mod_id", -10f);
        assertEquals(1f, phaseShield.getModifiedValue(), 0.001f);

        MutableStat plainShield = new MutableStat(1f);
        DefenseSkillEffect.DAMAGE_TAKEN_WITHOUT_PHASE_CLOAK_MULT.apply(hullWithShieldDamage(false, plainShield), "mod_id", -10f);
        assertEquals(0.9f, plainShield.getModifiedValue(), 0.001f);
    }

    @Test
    void describesTheConditionAfterTheStat() {
        assertEquals("15% less damage taken by shields while this ship has no captain.",
                ShieldSkillEffect.SHIELD_DAMAGE_TAKEN_WITHOUT_CAPTAIN_MULT.description(-15f).plain());
        assertEquals("10% less damage taken if this ship has no phase cloak.",
                DefenseSkillEffect.DAMAGE_TAKEN_WITHOUT_PHASE_CLOAK_MULT.description(-10f).plain());
    }
}
