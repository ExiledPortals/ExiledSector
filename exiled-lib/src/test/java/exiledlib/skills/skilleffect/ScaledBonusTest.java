package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledlib.skills.skilleffect.weapon.WeaponScope;
import exiledlib.skills.skilleffect.weapon.WeaponStatFamily;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScaledBonusTest {

    private static final String PER_UNIT_KEY = "exiledLib_test_perUnit";
    private static final String MOD_ID = "exiledLib_test";
    private static final ScaledBonus BONUS = new ScaledBonus(
            ScaledBonus.percent(StatTarget.liveStat(MutableShipStatsAPI::getMaxSpeed), PER_UNIT_KEY),
            ScaledBonus.percent(WeaponStatFamily.DAMAGE.target(WeaponScope.NON_BEAM_ENERGY)));

    private MutableShipStatsAPI stats;
    private MutableStat maxSpeed;
    private MutableStat energyDamage;
    private MutableStat beamDamage;

    @BeforeEach
    void setUp() {
        stats = mock(MutableShipStatsAPI.class);
        maxSpeed = mock(MutableStat.class);
        energyDamage = mock(MutableStat.class);
        beamDamage = mock(MutableStat.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(dynamic.getValue(PER_UNIT_KEY, 0f)).thenReturn(4f);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(stats.getMaxSpeed()).thenReturn(maxSpeed);
        when(stats.getEnergyWeaponDamageMult()).thenReturn(energyDamage);
        when(stats.getBeamWeaponDamageMult()).thenReturn(beamDamage);
    }

    @Test
    void eachPartIsItsPerUnitMagnitudeTimesTheScale() {
        BONUS.apply(stats, MOD_ID, 3f);

        verify(maxSpeed).modifyPercent(MOD_ID, 12f);
        verify(energyDamage).modifyPercent(MOD_ID, 3f);
        verify(beamDamage).modifyPercent(MOD_ID + StatTarget.Compensated.OFFSET_SUFFIX, -3f);
    }

    @Test
    void aScaleOfZeroRemovesEveryPartIncludingCompensatingOffsets() {
        BONUS.apply(stats, MOD_ID, 0f);

        verify(maxSpeed).unmodify(MOD_ID);
        verify(energyDamage).unmodify(MOD_ID);
        verify(beamDamage).unmodify(MOD_ID + StatTarget.Compensated.OFFSET_SUFFIX);
        verify(maxSpeed, never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void aTrackerReappliesOnlyWhenTheScaleChanges() {
        ScaledBonus.Tracker tracker = BONUS.tracker();

        boolean first = tracker.apply(stats, MOD_ID, 2f);
        boolean repeat = tracker.apply(stats, MOD_ID, 2f);
        boolean changed = tracker.apply(stats, MOD_ID, 3f);

        assertTrue(first);
        assertFalse(repeat);
        assertTrue(changed);
        verify(maxSpeed).modifyPercent(MOD_ID, 8f);
        verify(maxSpeed).modifyPercent(MOD_ID, 12f);
        assertEquals(3f, tracker.appliedScale());
    }

    @Test
    void aTrackerStartsAtZeroSoAZeroScaleChangesNothing() {
        assertFalse(BONUS.tracker().apply(stats, MOD_ID, 0f));
        verify(maxSpeed, never()).unmodify(anyString());
    }

    @Test
    void removingABonusTargetUnmodifiesItsStat() {
        StatBonus missileSpeed = mock(StatBonus.class);
        when(stats.getMissileMaxSpeedBonus()).thenReturn(missileSpeed);

        StatTarget.liveBonus(MutableShipStatsAPI::getMissileMaxSpeedBonus).remove(stats, MOD_ID);

        verify(missileSpeed).unmodify(MOD_ID);
    }
}
