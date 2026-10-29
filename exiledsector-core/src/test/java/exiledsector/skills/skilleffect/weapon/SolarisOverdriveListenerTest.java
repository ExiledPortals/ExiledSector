package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolarisOverdriveListenerTest {

    private static ShipAPI ship(String id, Map<String, Float> magnitudes) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getMutableStats()).thenReturn(stats);
        magnitudes.forEach((key, value) -> when(stats.getDynamic().getValue(key, 0f)).thenReturn(value));
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }

    @Test
    void solarisOverdriveFreesWeaponFluxAndSpeedsUpWeaponsAfterTheSystemStartsThenWaitsOutItsCooldown() {
        ShipSystemAPI system = mock(ShipSystemAPI.class);
        ShipAPI ship = ship("aracari", Map.of(SolarisOverdriveListener.SECONDS_KEY, 4f,
                SolarisOverdriveListener.PROJECTILE_SPEED_PERCENT_KEY, 50f, SolarisOverdriveListener.RATE_OF_FIRE_PERCENT_KEY, 25f));
        when(ship.getSystem()).thenReturn(system);
        SolarisOverdriveListener listener = new SolarisOverdriveListener(ship);
        String modId = "exiledLib_solarisOverdrive_aracari";

        listener.advance(0.1f);
        assertFalse(listener.overdriven());
        when(system.isActive()).thenReturn(true);
        listener.advance(0.1f);
        assertTrue(listener.overdriven());
        verify(ship.getMutableStats().getBallisticWeaponFluxCostMod()).modifyMult(modId, 0f);
        verify(ship.getMutableStats().getEnergyWeaponFluxCostMod()).modifyMult(modId, 0f);
        verify(ship.getMutableStats().getBallisticProjectileSpeedMult()).modifyPercent(modId, 50f);
        verify(ship.getMutableStats().getBallisticRoFMult()).modifyPercent(modId, 25f);
        verify(ship.getMutableStats().getEnergyRoFMult()).modifyPercent(modId, 25f);
        verify(ship.getMutableStats().getMissileRoFMult()).modifyPercent(modId, 25f);

        listener.advance(4f);
        assertFalse(listener.overdriven());
        verify(ship.getMutableStats().getBallisticWeaponFluxCostMod()).unmodify(modId);
        verify(ship.getMutableStats().getBallisticProjectileSpeedMult()).unmodify(modId);
        verify(ship.getMutableStats().getBallisticRoFMult()).unmodify(modId);

        when(system.isActive()).thenReturn(false);
        listener.advance(SolarisOverdriveListener.COOLDOWN_SECONDS - 5f);
        when(system.isActive()).thenReturn(true);
        listener.advance(0.1f);
        assertFalse(listener.overdriven());
        verify(ship.getMutableStats().getBallisticWeaponFluxCostMod(), times(1)).modifyMult(modId, 0f);

        when(system.isActive()).thenReturn(false);
        listener.advance(1f);
        when(system.isActive()).thenReturn(true);
        listener.advance(0.1f);
        assertTrue(listener.overdriven());
        verify(ship.getMutableStats().getBallisticWeaponFluxCostMod(), times(2)).modifyMult(modId, 0f);
    }
}
