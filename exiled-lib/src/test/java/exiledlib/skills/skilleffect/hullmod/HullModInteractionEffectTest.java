package exiledlib.skills.skilleffect.hullmod;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HullModInteractionEffectTest {

    @Test
    void hullModInteractionsNeedTheHullModAndBuiltInOnesNeedItAsAnSMod() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(stats.getVariant()).thenReturn(variant);
        when(variant.hasHullMod("ballistic_rangefinder")).thenReturn(true);
        when(variant.hasHullMod("blast_doors")).thenReturn(true);
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>(List.of("additional_berthing")));

        HullModInteractionEffect.RANGEFINDER_BALLISTIC_WEAPON_DAMAGE_PERCENT.apply(stats, "frame", 5f);
        HullModInteractionEffect.ECM_BALLISTIC_WEAPON_RANGE_PERCENT.apply(stats, "frame", 5f);
        HullModInteractionEffect.BUILT_IN_BLAST_DOORS_HULL_PERCENT.apply(stats, "frame", 12.5f);
        HullModInteractionEffect.BUILT_IN_BERTHING_PEAK_CR_PERCENT.apply(stats, "frame", 12.5f);

        verify(stats.getBallisticWeaponDamageMult()).modifyPercent("frame", 5f);
        verify(stats.getBallisticWeaponRangeBonus()).unmodify("frame");
        verify(stats.getHullBonus()).unmodify("frame");
        verify(stats.getPeakCRDuration()).modifyPercent("frame", 12.5f);
    }

    @Test
    void builtInAuxiliaryThrustersAddTopSpeedOnlyDuringTheZeroFluxBoost() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getMutableStats()).thenReturn(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS));
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>(List.of("auxiliarythrusters")));
        when(ship.getVariant()).thenReturn(variant);
        when(ship.getFluxTracker()).thenReturn(flux);
        HullModInteractionEffect effect = HullModInteractionEffect.BUILT_IN_AUX_THRUSTERS_ZERO_FLUX_TOP_SPEED_PERCENT;

        assertTrue(effect.advancesInCombat());
        effect.advanceInCombat(ship, "frame", 7.5f);
        verify(ship.getMutableStats().getMaxSpeed()).unmodify("frame");
        when(flux.isEngineBoostActive()).thenReturn(true);
        effect.advanceInCombat(ship, "frame", 7.5f);
        verify(ship.getMutableStats().getMaxSpeed()).modifyPercent("frame", 7.5f);
    }
}
