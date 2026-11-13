package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShieldArmorShareListenerTest {

    private final MutableStat shield = new MutableStat(1f);
    private final MutableStat armor = spy(new MutableStat(1f));

    private ShipAPI ship(float shieldDamageTakenMult) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn("ship");
        when(ship.getMutableStats().getDynamic().getValue(ShieldArmorShareListener.EFFECTIVENESS_PERCENT_KEY, 0f)).thenReturn(50f);
        when(ship.getMutableStats().getShieldDamageTakenMult()).thenReturn(shield);
        when(ship.getMutableStats().getArmorDamageTakenMult()).thenReturn(armor);
        shield.modifyMult("shieldSkill", shieldDamageTakenMult);
        return ship;
    }

    @Test
    void armorTakesTheGivenShareOfTheShieldDamageChange() {
        assertEquals(0.8f, ShieldArmorShareListener.armorDamageTakenMult(0.6f, 50f), 0.001f);
        assertEquals(1.2f, ShieldArmorShareListener.armorDamageTakenMult(1.4f, 50f), 0.001f);
        assertEquals(1f, ShieldArmorShareListener.armorDamageTakenMult(0.6f, 0f), 0.001f);
        assertEquals(0f, ShieldArmorShareListener.armorDamageTakenMult(-1f, 100f), 0.001f);
    }

    @Test
    void onlyShieldSpecificModifiersCount() {
        shield.modifyPercent("shieldPercent", -20f);
        shield.modifyFlat("shieldFlat", 0.1f);
        shield.modifyMult("shieldMult", 0.5f);
        shield.modifyMult("allDamage", 0.5f);
        armor.modifyMult("allDamage", 0.5f);

        assertEquals(0.45f, ShieldArmorShareListener.shieldOnlyDamageTakenMult(shield, armor), 0.001f);
    }

    @Test
    void theArmorMultFollowsTheShieldStatAndClearsWhenItReturnsToNormal() {
        ShipAPI ship = ship(0.6f);
        ShieldArmorShareListener listener = new ShieldArmorShareListener(ship);

        listener.advance(0.3f);
        listener.advance(0.3f);
        verify(armor, times(1)).modifyMult("exiledSector_shieldArmorShare_ship", 0.8f);

        shield.unmodifyMult("shieldSkill");
        listener.advance(0.3f);
        verify(armor).unmodifyMult("exiledSector_shieldArmorShare_ship");
    }

    @Test
    void checksWaitForTheInterval() {
        ShipAPI ship = ship(0.6f);
        ShieldArmorShareListener listener = new ShieldArmorShareListener(ship);

        listener.advance(0.3f);
        shield.unmodifyMult("shieldSkill");
        listener.advance(0.1f);

        verify(armor, times(1)).modifyMult(anyString(), anyFloat());
        verify(armor, never()).unmodifyMult(anyString());
    }
}
