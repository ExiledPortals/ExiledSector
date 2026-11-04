package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutomatedShieldDamageListenerTest {

    private static ShipAPI ship(String id, int owner, boolean automated) {
        ShipAPI ship = mock(ShipAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats()).thenReturn(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS));
        when(ship.getVariant()).thenReturn(variant);
        when(variant.hasHullMod(HullMods.AUTOMATED)).thenReturn(automated);
        return ship;
    }

    private static DamagingProjectileAPI shotFrom(ShipAPI source) {
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        when(projectile.getSource()).thenReturn(source);
        return projectile;
    }

    private static DamageAPI damage() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));
        return damage;
    }

    private static AutomatedShieldDamageListener listener(ShipAPI owner, float percent) {
        when(owner.getMutableStats().getDynamic().getValue(AutomatedShieldDamageListener.REDUCTION_PERCENT_KEY, 0f)).thenReturn(percent);
        return new AutomatedShieldDamageListener(owner);
    }

    @Test
    void shieldHitsFromEnemyAutomatedShipsDealLessDamage() {
        ShipAPI owner = ship("patched", 0, false);
        AutomatedShieldDamageListener listener = listener(owner, 25f);
        ShipAPI remnant = ship("remnant", 1, true);
        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getSource()).thenReturn(remnant);
        DamageAPI shot = damage();
        DamageAPI beamTick = damage();

        String modId = listener.modifyDamageTaken(shotFrom(remnant), owner, shot, new Vector2f(), true);
        listener.modifyDamageTaken(beam, owner, beamTick, new Vector2f(), true);

        assertEquals("exiledSector_automatedShieldDamage_patched", modId);
        verify(shot.getModifier()).modifyMult(modId, 0.75f);
        verify(beamTick.getModifier()).modifyMult(modId, 0.75f);
    }

    @Test
    void hullHitsCrewedAttackersAndAlliedDronesAreNotReduced() {
        ShipAPI owner = ship("patched", 0, false);
        AutomatedShieldDamageListener listener = listener(owner, 25f);
        DamageAPI hit = damage();

        assertNull(listener.modifyDamageTaken(shotFrom(ship("remnant", 1, true)), owner, hit, new Vector2f(), false));
        assertNull(listener.modifyDamageTaken(shotFrom(ship("crewed", 1, false)), owner, hit, new Vector2f(), true));
        assertNull(listener.modifyDamageTaken(shotFrom(ship("allied drone", 0, true)), owner, hit, new Vector2f(), true));
        assertNull(listener.modifyDamageTaken(null, owner, hit, new Vector2f(), true));
        verify(hit.getModifier(), never()).modifyMult(anyString(), anyFloat());
    }
}
