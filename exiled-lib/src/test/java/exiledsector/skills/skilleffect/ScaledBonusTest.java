package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScaledBonusTest {

    private static final String PER_UNIT_KEY = "exiledSector_test_perUnit";
    private static final String MOD_ID = "exiledSector_test";
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
    void removingABonusTargetUnmodifiesItsStat() {
        StatBonus missileSpeed = mock(StatBonus.class);
        when(stats.getMissileMaxSpeedBonus()).thenReturn(missileSpeed);

        StatTarget.liveBonus(MutableShipStatsAPI::getMissileMaxSpeedBonus).remove(stats, MOD_ID);

        verify(missileSpeed).unmodify(MOD_ID);
    }
}
