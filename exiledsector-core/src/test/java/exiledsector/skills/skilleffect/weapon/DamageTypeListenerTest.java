package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.AdditionalMatchers;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DamageTypeListenerTest {

    private static final String MOD_ID = "exiledSector_damageType_ship";

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();

    @BeforeEach
    void setUp() {
        List<Object> registered = new ArrayList<>();
        CombatListenerManagerAPI listeners = mock(CombatListenerManagerAPI.class);
        when(combat.engine().getListenerManager()).thenReturn(listeners);
        doAnswer(call -> registered.add(call.getArgument(0))).when(listeners).addListener(any());
        when(listeners.getListeners(HitDamageRestorer.class)).thenAnswer(call -> List.copyOf(registered));
    }

    private static ShipAPI ship() {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn("ship");
        when(ship.getMutableStats().getDynamic().getValue(DamageTypeListener.KINETIC_PERCENT_KEY, 0f)).thenReturn(15f);
        when(ship.getMutableStats().getDynamic().getValue(DamageTypeListener.FRAGMENTATION_PERCENT_KEY, 0f)).thenReturn(30f);
        when(ship.getMutableStats().getDynamic().getValue(DamageTypeListener.FRAGMENTATION_MORE_PERCENT_KEY, 0f)).thenReturn(-40f);
        return ship;
    }

    private static DamageAPI damage(DamageType type) {
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);
        when(damage.getType()).thenReturn(type);
        return damage;
    }

    @Test
    void eachDamageTypeGetsItsOwnBonus() {
        DamageTypeListener listener = new DamageTypeListener(ship());
        DamageAPI kinetic = damage(DamageType.KINETIC);
        DamageAPI fragmentation = damage(DamageType.FRAGMENTATION);

        assertEquals(MOD_ID, listener.modifyDamageDealt(null, null, kinetic, null, true));
        assertEquals(MOD_ID, listener.modifyDamageDealt(null, null, fragmentation, null, false));

        verify(kinetic.getModifier()).modifyPercent(MOD_ID, 15f);
        verify(fragmentation.getModifier()).modifyPercent(MOD_ID, 30f);
    }

    @Test
    void lessFragmentationDamageMultipliesOnlyFragmentationHits() {
        DamageTypeListener listener = new DamageTypeListener(ship());
        DamageAPI kinetic = damage(DamageType.KINETIC);
        DamageAPI fragmentation = damage(DamageType.FRAGMENTATION);

        listener.modifyDamageDealt(null, null, kinetic, null, true);
        listener.modifyDamageDealt(null, null, fragmentation, null, false);

        verify(kinetic.getModifier(), never()).modifyMult(anyString(), anyFloat());
        verify(fragmentation.getModifier()).modifyMult(eq(MOD_ID), AdditionalMatchers.eq(0.6f, 1e-5f));
    }

    @Test
    void fragmentationHitsRetypedForTheHitStillDealLessDamage() {
        DamageTypeListener listener = new DamageTypeListener(ship());
        DamageAPI fragmentation = damage(DamageType.FRAGMENTATION);
        HitDamageRestorer.retypeForThisHit(fragmentation, DamageType.HIGH_EXPLOSIVE, mock(ShipAPI.class));
        when(fragmentation.getType()).thenReturn(DamageType.HIGH_EXPLOSIVE);

        assertEquals(MOD_ID, listener.modifyDamageDealt(null, null, fragmentation, null, false));

        verify(fragmentation.getModifier()).modifyMult(eq(MOD_ID), AdditionalMatchers.eq(0.6f, 1e-5f));
        verify(fragmentation.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void typesWithoutABonusAreLeftAlone() {
        DamageTypeListener listener = new DamageTypeListener(ship());
        DamageAPI energy = damage(DamageType.ENERGY);
        DamageAPI untyped = damage(null);

        assertNull(listener.modifyDamageDealt(null, null, energy, null, true));
        assertNull(listener.modifyDamageDealt(null, null, untyped, null, true));
        verify(energy.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }
}
