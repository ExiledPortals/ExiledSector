package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MothershipUplinkTest {

    private static final String MOD_ID = "uplink";

    private static MutableShipStatsAPI stats() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat ballisticDamage = new MutableStat(1f);
        MutableStat energyDamage = new MutableStat(1f);
        MutableStat missileDamage = new MutableStat(1f);
        MutableStat beamDamage = new MutableStat(1f);
        MutableStat ballisticRateOfFire = new MutableStat(1f);
        MutableStat energyRateOfFire = new MutableStat(1f);
        MutableStat missileRateOfFire = new MutableStat(1f);
        when(stats.getBallisticWeaponDamageMult()).thenReturn(ballisticDamage);
        when(stats.getEnergyWeaponDamageMult()).thenReturn(energyDamage);
        when(stats.getMissileWeaponDamageMult()).thenReturn(missileDamage);
        when(stats.getBeamWeaponDamageMult()).thenReturn(beamDamage);
        when(stats.getBallisticRoFMult()).thenReturn(ballisticRateOfFire);
        when(stats.getEnergyRoFMult()).thenReturn(energyRateOfFire);
        when(stats.getMissileRoFMult()).thenReturn(missileRateOfFire);
        return stats;
    }

    @Test
    void strikecraftGainTheShareOfEachOfTheCarriersPercentBonuses() {
        MutableShipStatsAPI carrier = stats();
        carrier.getBallisticWeaponDamageMult().modifyPercent("tree", 20f);
        carrier.getBallisticWeaponDamageMult().modifyPercent("officer", 10f);
        carrier.getEnergyRoFMult().modifyPercent("tree", 40f);
        carrier.getBeamWeaponDamageMult().modifyPercent("tree", 8f);
        MutableShipStatsAPI fighter = stats();

        MothershipUplink.share(carrier, fighter, MOD_ID, 25f);

        assertEquals(7.5f, fighter.getBallisticWeaponDamageMult().getPercentMod(), 0.001f);
        assertEquals(2f, fighter.getBeamWeaponDamageMult().getPercentMod(), 0.001f);
        assertEquals(10f, fighter.getEnergyRoFMult().getPercentMod(), 0.001f);
        assertEquals(0f, fighter.getMissileWeaponDamageMult().getPercentMod(), 0.001f);
    }

    @Test
    void aCarrierPenaltyIsNotShared() {
        MutableShipStatsAPI carrier = stats();
        carrier.getMissileRoFMult().modifyPercent("dmod", -30f);
        MutableShipStatsAPI fighter = stats();

        MothershipUplink.share(carrier, fighter, MOD_ID, 50f);

        assertEquals(0f, fighter.getMissileRoFMult().getPercentMod(), 0.001f);
    }
}
