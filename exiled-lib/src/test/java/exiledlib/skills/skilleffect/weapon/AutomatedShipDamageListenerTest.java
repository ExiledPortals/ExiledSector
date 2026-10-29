package exiledlib.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
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

class AutomatedShipDamageListenerTest {

    private static ShipAPI ship(String id, int owner, boolean automated) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats()).thenReturn(stats);
        when(ship.getVariant()).thenReturn(variant);
        when(variant.hasHullMod(HullMods.AUTOMATED)).thenReturn(automated);
        return ship;
    }

    private static DamageAPI damage() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));
        return damage;
    }

    private static AutomatedShipDamageListener listener(float percent) {
        ShipAPI owner = ship("pather", 0, false);
        when(owner.getMutableStats().getDynamic().getValue(AutomatedShipDamageListener.DAMAGE_PERCENT_KEY, 0f)).thenReturn(percent);
        return new AutomatedShipDamageListener(owner);
    }

    @Test
    void hitsOnEnemyAutomatedShipsDealTheBonusDamage() {
        DamageAPI hit = damage();

        String modId = listener(40f).modifyDamageDealt(null, ship("remnant", 1, true), hit, new Vector2f(), false);

        assertEquals("exiledLib_automatedShipDamage_pather", modId);
        verify(hit.getModifier()).modifyPercent(modId, 40f);
    }

    @Test
    void automatedTaggedHullsCountAsAutomatedShips() {
        ShipAPI moddedDrone = ship("modded drone", 1, false);
        when(moddedDrone.getVariant().getHullSpec()).thenReturn(mock(ShipHullSpecAPI.class));
        when(moddedDrone.getVariant().getHullSpec().hasTag(HullMods.AUTOMATED)).thenReturn(true);
        DamageAPI hit = damage();

        String modId = listener(40f).modifyDamageDealt(null, moddedDrone, hit, new Vector2f(), false);

        verify(hit.getModifier()).modifyPercent(modId, 40f);
    }

    @Test
    void crewedTargetsAlliesAndNonShipsGetNoBonus() {
        AutomatedShipDamageListener listener = listener(40f);
        DamageAPI hit = damage();

        assertNull(listener.modifyDamageDealt(null, ship("crewed", 1, false), hit, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship("allied drone", 0, true), hit, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship("neutral", 100, true), hit, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, null, hit, new Vector2f(), false));
        verify(hit.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void aShipWithoutTheEffectLeavesTheHitAlone() {
        DamageAPI hit = damage();

        assertNull(listener(0f).modifyDamageDealt(null, ship("remnant", 1, true), hit, new Vector2f(), false));
        verify(hit.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }
}
