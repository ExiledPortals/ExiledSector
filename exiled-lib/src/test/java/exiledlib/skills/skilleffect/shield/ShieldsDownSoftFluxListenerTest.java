package exiledlib.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShieldsDownSoftFluxListenerTest {

    private ShipAPI ship;
    private ShieldAPI shield;
    private FluxTrackerAPI flux;
    private DamageTakenModifier listener;

    @BeforeEach
    void setUp() {
        ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(ShieldsDownSoftFluxListener.CONVERTED_PERCENT_KEY, 0f)).thenReturn(30f);
        shield = mock(ShieldAPI.class);
        when(shield.isOff()).thenReturn(true);
        when(ship.getShield()).thenReturn(shield);
        flux = mock(FluxTrackerAPI.class);
        when(flux.getMaxFlux()).thenReturn(1000f);
        when(ship.getFluxTracker()).thenReturn(flux);
        ShieldSkillEffect.SHIELDS_DOWN_HULL_DAMAGE_TO_SOFT_FLUX_PERCENT.applyAfterShipCreation(ship, "mod_id", 30f);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ship).addListener(captor.capture());
        listener = (DamageTakenModifier) captor.getValue();
    }

    private static DamageAPI hit(MutableStat modifier, float damage) {
        DamageAPI hit = mock(DamageAPI.class);
        when(hit.getModifier()).thenReturn(modifier);
        when(hit.getDamage()).thenReturn(damage);
        return hit;
    }

    @Test
    void armorAndHullHitsWhileShieldsAreDownTurnTheStatedShareIntoSoftFlux() {
        MutableStat modifier = new MutableStat(1f);

        String modifierId = listener.modifyDamageTaken(new Object(), ship, hit(modifier, 100f), new Vector2f(), false);

        assertNotNull(modifierId);
        assertEquals(0.7f, modifier.getModifiedValue(), 1e-6f);
        verify(flux).increaseFlux(AdditionalMatchers.eq(30f, 1e-3f), eq(false));
    }

    @Test
    void onlyAsMuchAsFitsBeforeFluxIsFullIsConverted() {
        when(flux.getCurrFlux()).thenReturn(990f);
        MutableStat modifier = new MutableStat(1f);

        listener.modifyDamageTaken(new Object(), ship, hit(modifier, 100f), new Vector2f(), false);

        assertEquals(0.9f, modifier.getModifiedValue(), 1e-6f);
        verify(flux).increaseFlux(AdditionalMatchers.eq(10f, 1e-3f), eq(false));
    }

    @Test
    void nothingIsConvertedWithShieldsUpOnShieldHitsOrAtFullFlux() {
        DamageAPI hit = hit(new MutableStat(1f), 100f);

        assertNull(listener.modifyDamageTaken(new Object(), ship, hit, new Vector2f(), true));
        when(shield.isOff()).thenReturn(false);
        assertNull(listener.modifyDamageTaken(new Object(), ship, hit, new Vector2f(), false));
        when(shield.isOff()).thenReturn(true);
        when(flux.getCurrFlux()).thenReturn(1000f);
        assertNull(listener.modifyDamageTaken(new Object(), ship, hit, new Vector2f(), false));

        verify(flux, never()).increaseFlux(anyFloat(), anyBoolean());
    }

    @Test
    void theEffectNeedsShieldsAndExcludesShieldShunt() {
        ShieldSkillEffect veil = ShieldSkillEffect.SHIELDS_DOWN_HULL_DAMAGE_TO_SOFT_FLUX_PERCENT;

        assertEquals(List.of(HullMods.SHIELD_SHUNT), veil.exclusiveHullModIds());
        assertNull(veil.blockAllocationReason(null, ShieldAPI.ShieldType.OMNI));
        assertNotNull(veil.blockAllocationReason(null, ShieldAPI.ShieldType.NONE));
    }
}
