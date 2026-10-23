package exiledlib.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import exiledlib.skills.skilleffect.SkillEffect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScopedWeaponEffectTest {

    @Test
    void autofireAimAccuracyAddsTheMagnitudeAsAFraction() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat accuracy = mock(MutableStat.class);
        when(stats.getAutofireAimAccuracy()).thenReturn(accuracy);

        SkillEffect.byName("WEAPON_AUTOFIRE_ACCURACY_PERCENT").apply(stats, "mod_id", 50f);

        verify(accuracy).modifyFlat("mod_id", 0.5f);
    }

    @Test
    void missileGuidanceDescribesTheImprovedAlgorithmInsteadOfARawNumber() {
        assertEquals("Significantly improved missile guidance algorithm.",
                SkillEffect.byName("MISSILE_WEAPON_GUIDANCE_FLAT").description(1f).plain());
    }

    @Test
    void weaponTurnRateDescriptionsCoverAllWeapons() {
        assertEquals("50% increased weapon turn rate.", SkillEffect.byName("WEAPON_TURN_RATE_PERCENT").description(50f).plain());
        assertEquals("25% less weapon turn rate.", SkillEffect.byName("WEAPON_TURN_RATE_MULT").description(-25f).plain());
    }
}
