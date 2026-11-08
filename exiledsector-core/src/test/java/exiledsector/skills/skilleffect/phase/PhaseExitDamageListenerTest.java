package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhaseExitDamageListenerTest {

    private static final String AMBUSH_MOD_ID = "exiledSector_phaseExitWeaponDamage_afflictor";

    private static ShipAPI phaseShip(float damagePercent) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn("afflictor");
        when(ship.getMutableStats()).thenReturn(stats);
        when(ship.isAlive()).thenReturn(true);
        when(stats.getDynamic().getValue(PhaseExitDamageListener.DAMAGE_PERCENT_KEY, 0f)).thenReturn(damagePercent);
        return ship;
    }

    @Test
    void leavingPhaseGivesTheFullBonusAndTapersItAway() {
        ShipAPI ship = phaseShip(25f);
        MutableShipStatsAPI stats = ship.getMutableStats();
        PhaseExitDamageListener listener = new PhaseExitDamageListener(ship);

        when(ship.isPhased()).thenReturn(true);
        listener.advance(0.1f);
        verify(stats.getEnergyWeaponDamageMult(), never()).modifyPercent(anyString(), anyFloat());

        when(ship.isPhased()).thenReturn(false);
        listener.advance(0f);
        verify(stats.getEnergyWeaponDamageMult()).modifyPercent(AMBUSH_MOD_ID, 25f);
        verify(stats.getBallisticWeaponDamageMult()).modifyPercent(AMBUSH_MOD_ID, 25f);

        listener.advance(PhaseExitDamageListener.TAPER_SECONDS / 2f);
        verify(stats.getMissileWeaponDamageMult()).modifyPercent(AMBUSH_MOD_ID, 12.5f);

        listener.advance(PhaseExitDamageListener.TAPER_SECONDS / 2f);
        verify(stats.getEnergyWeaponDamageMult()).unmodify(AMBUSH_MOD_ID);
    }

    @Test
    void increasedDurationStretchesTheTaper() {
        ShipAPI ship = phaseShip(25f);
        MutableShipStatsAPI stats = ship.getMutableStats();
        when(stats.getDynamic().getValue(PhaseExitDamageListener.DURATION_PERCENT_KEY, 0f)).thenReturn(100f);
        PhaseExitDamageListener listener = new PhaseExitDamageListener(ship);
        when(ship.isPhased()).thenReturn(true);
        listener.advance(0.1f);
        when(ship.isPhased()).thenReturn(false);
        listener.advance(0f);

        listener.advance(PhaseExitDamageListener.TAPER_SECONDS);
        verify(stats.getEnergyWeaponDamageMult()).modifyPercent(AMBUSH_MOD_ID, 12.5f);
        verify(stats.getEnergyWeaponDamageMult(), never()).unmodify(AMBUSH_MOD_ID);

        listener.advance(PhaseExitDamageListener.TAPER_SECONDS);
        verify(stats.getEnergyWeaponDamageMult()).unmodify(AMBUSH_MOD_ID);
    }

    @Test
    void aDestroyedShipLosesTheBonusAtOnce() {
        ShipAPI ship = phaseShip(25f);
        PhaseExitDamageListener listener = new PhaseExitDamageListener(ship);
        when(ship.isPhased()).thenReturn(true);
        listener.advance(0.1f);
        when(ship.isPhased()).thenReturn(false);
        listener.advance(0f);

        when(ship.isAlive()).thenReturn(false);
        listener.advance(0.1f);
        verify(ship.getMutableStats().getEnergyWeaponDamageMult()).unmodify(AMBUSH_MOD_ID);
    }
}
