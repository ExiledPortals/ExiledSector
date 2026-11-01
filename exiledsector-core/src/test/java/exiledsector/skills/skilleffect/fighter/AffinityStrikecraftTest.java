package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.EffectTotals;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.logistics.LogisticsSkillEffect;
import exiledsector.skills.skilleffect.special.SpecialSkillEffect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.eq;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AffinityStrikecraftTest {

    private static EffectContribution permanent(SkillTypeEffect... effects) {
        return EffectContribution.scaledPermanent("source", "source_", List.of(effects));
    }

    @Test
    void strikecraftGetEveryNonLogisticsAffinityBonusOnlyWhileGranted() {
        ShipAPI fighter = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        MutableStat fluxCapacity = new MutableStat(100f);
        MutableStat ballisticDamage = new MutableStat(1f);
        when(fighter.getMutableStats()).thenReturn(stats);
        when(fighter.getHullSpec()).thenReturn(hullSpec);
        when(hullSpec.getHitpoints()).thenReturn(100f);
        when(stats.getHullBonus()).thenReturn(new StatBonus());
        when(stats.getArmorBonus()).thenReturn(new StatBonus());
        when(stats.getFluxCapacity()).thenReturn(fluxCapacity);
        when(stats.getBallisticWeaponDamageMult()).thenReturn(ballisticDamage);
        when(fighter.getMaxHitpoints()).thenReturn(100f);
        when(fighter.getHitpoints()).thenReturn(80f);
        EffectContribution tree = permanent(new SkillTypeEffect(SpecialSkillEffect.HIGH_TECH_AFFINITY_FLAT, 5f),
                new SkillTypeEffect(SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT, 5f),
                new SkillTypeEffect(SpecialSkillEffect.BALLISTIC_WEAPON_DAMAGE_PERCENT_PER_LOW_TECH_AFFINITY, 3f));
        List<EffectContribution> withoutGrant = EffectTotals.of(List.of(tree), 1f).contributions();

        AffinityStrikecraft.apply(fighter, withoutGrant);
        verify(fighter, never()).setMaxHitpoints(anyFloat());
        assertEquals(100f, fluxCapacity.getModifiedValue(), 1e-3f);

        List<EffectContribution> granted = EffectTotals.of(List.of(tree,
                permanent(new SkillTypeEffect(FighterSkillEffect.STRIKECRAFT_GAIN_AFFINITY_BONUSES, 1f))), 1f).contributions();
        assertTrue(AffinityStrikecraft.granted(granted));
        assertFalse(AffinityStrikecraft.granted(withoutGrant));

        AffinityStrikecraft.apply(fighter, granted);
        verify(fighter).setMaxHitpoints(eq(102f, 1e-3f));
        verify(fighter).setHitpoints(eq(81.6f, 1e-3f));
        assertEquals(102f, fluxCapacity.getModifiedValue(), 1e-3f);
        assertEquals(1.03f, ballisticDamage.getModifiedValue(), 1e-4f);
        verify(stats, never()).getFuelMod();
    }

    @Test
    void logisticsAndFighterBonusesStayWithTheCarrier() {
        assertFalse(AffinityStrikecraft.carriesOver(LogisticsSkillEffect.FUEL_CAPACITY_MULT));
        assertFalse(AffinityStrikecraft.carriesOver(FighterSkillEffect.FIGHTER_WEAPON_DAMAGE_PERCENT));
        assertTrue(AffinityStrikecraft.carriesOver(SpecialSkillEffect.BALLISTIC_WEAPON_DAMAGE_PERCENT_PER_LOW_TECH_AFFINITY.affinityScaling().target()));
    }
}
