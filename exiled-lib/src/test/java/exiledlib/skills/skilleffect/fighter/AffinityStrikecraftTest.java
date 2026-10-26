package exiledlib.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import exiledlib.skills.EffectContribution;
import exiledlib.skills.EffectTotals;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.special.SpecialSkillEffect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.eq;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AffinityStrikecraftTest {

    private static EffectContribution permanent(SkillTypeEffect... effects) {
        return EffectContribution.scaledPermanent("source", "source_", List.of(effects));
    }

    @Test
    void strikecraftGetTheShipsAffinityStepBonusesToHullAndFluxOnlyWhileGranted() {
        ShipAPI fighter = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat fluxCapacity = mock(MutableStat.class);
        when(fighter.getMutableStats()).thenReturn(stats);
        when(stats.getFluxCapacity()).thenReturn(fluxCapacity);
        when(stats.getArmorBonus()).thenReturn(mock(StatBonus.class));
        when(fighter.getMaxHitpoints()).thenReturn(100f);
        when(fighter.getHitpoints()).thenReturn(80f);
        EffectContribution highTech = permanent(new SkillTypeEffect(SpecialSkillEffect.HIGH_TECH_AFFINITY_FLAT, 5f),
                new SkillTypeEffect(SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT, 5f));
        List<EffectContribution> withoutGrant = EffectTotals.of(List.of(highTech), 1f).contributions();

        AffinityStrikecraft.apply(fighter, withoutGrant);
        verify(fighter, never()).setMaxHitpoints(anyFloat());
        verify(fluxCapacity, never()).modifyMult(anyString(), anyFloat());

        List<EffectContribution> granted = EffectTotals.of(List.of(highTech,
                permanent(new SkillTypeEffect(FighterSkillEffect.STRIKECRAFT_GAIN_AFFINITY_BONUSES, 1f))), 1f).contributions();
        assertTrue(AffinityStrikecraft.granted(granted));
        assertFalse(AffinityStrikecraft.granted(withoutGrant));

        AffinityStrikecraft.apply(fighter, granted);
        verify(fighter).setMaxHitpoints(eq(102f, 1e-3f));
        verify(fighter).setHitpoints(eq(81.6f, 1e-3f));
        verify(fluxCapacity).modifyMult(anyString(), eq(1.02f, 1e-4f));
    }
}
