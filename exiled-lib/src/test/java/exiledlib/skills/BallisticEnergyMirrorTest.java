package exiledlib.skills;

import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.StatMode;
import exiledlib.skills.skilleffect.weapon.ScopedWeaponEffect;
import exiledlib.skills.skilleffect.weapon.WeaponScope;
import exiledlib.skills.skilleffect.weapon.WeaponSkillEffect;
import exiledlib.skills.skilleffect.weapon.WeaponStatFamily;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallisticEnergyMirrorTest {

    private static final SkillTypeEffect FLAG = new SkillTypeEffect(WeaponSkillEffect.BALLISTIC_COUNTS_AS_ENERGY, 0f);

    private static SkillEffect weapon(WeaponStatFamily family, WeaponScope scope, StatMode mode) {
        return ScopedWeaponEffect.find(family, scope, mode);
    }

    private static SkillEffect damage(WeaponScope scope) {
        return weapon(WeaponStatFamily.DAMAGE, scope, StatMode.PERCENT);
    }

    private static SkillEffect range(WeaponScope scope) {
        return weapon(WeaponStatFamily.RANGE, scope, StatMode.PERCENT);
    }

    private static EffectContribution node(String id, SkillTypeEffect... effects) {
        return EffectContribution.scaledPermanent(id, id + "_", List.of(effects));
    }

    private static EffectContribution temporaryNode(String id, float seconds, SkillTypeEffect... effects) {
        return new EffectContribution(id, id + "_", List.of(effects), seconds, null, true, true);
    }

    private static Map<SkillEffect, Float> permanentTotals(List<EffectContribution> contributions) {
        return EffectTotals.of(contributions, 1f).groups().get(0).totals();
    }

    @Test
    void withoutTheFlagNothingIsMirrored() {
        List<EffectContribution> contributions = List.of(node("energy", new SkillTypeEffect(damage(WeaponScope.ENERGY), 10f)));

        assertSame(contributions, BallisticEnergyMirror.withMirroredBonuses(contributions));
    }

    @Test
    void energyAndNonBeamEnergyBonusesAlsoReachBallisticsOnTopOfTheirOwn() {
        Map<SkillEffect, Float> totals = permanentTotals(List.of(
                node("fafnir", FLAG),
                node("energy", new SkillTypeEffect(damage(WeaponScope.ENERGY), 10f),
                        new SkillTypeEffect(range(WeaponScope.NON_BEAM_ENERGY), 5f)),
                node("ballistic", new SkillTypeEffect(damage(WeaponScope.BALLISTIC), 7f))));

        assertEquals(17f, totals.get(damage(WeaponScope.BALLISTIC)), 0.001f);
        assertEquals(5f, totals.get(range(WeaponScope.BALLISTIC)), 0.001f);
        assertEquals(10f, totals.get(damage(WeaponScope.ENERGY)), 0.001f);
        assertEquals(5f, totals.get(range(WeaponScope.NON_BEAM_ENERGY)), 0.001f);
    }

    @Test
    void beamOnlyAndAllWeaponBonusesAreNotMirrored() {
        Map<SkillEffect, Float> totals = permanentTotals(List.of(
                node("fafnir", FLAG),
                node("beam", new SkillTypeEffect(damage(WeaponScope.BEAM), 10f)),
                node("all", new SkillTypeEffect(damage(WeaponScope.ALL), 10f))));

        assertFalse(totals.containsKey(damage(WeaponScope.BALLISTIC)));
    }

    @Test
    void mirroredMultipliersPoolWithTheBallisticOnes() {
        SkillEffect energyMult = weapon(WeaponStatFamily.DAMAGE, WeaponScope.ENERGY, StatMode.MULT);
        SkillEffect ballisticMult = weapon(WeaponStatFamily.DAMAGE, WeaponScope.BALLISTIC, StatMode.MULT);

        EffectTotals totals = EffectTotals.of(List.of(node("fafnir", FLAG), node("energy", new SkillTypeEffect(energyMult, 10f)),
                node("ballistic", new SkillTypeEffect(ballisticMult, 10f))), 1f);

        assertEquals(totals.pooledMultipliers().get(energyMult) * 2f, totals.pooledMultipliers().get(ballisticMult), 0.001f);
    }

    @Test
    void temporaryEnergyBonusesStayTemporaryButATemporaryFlagMirrorsNothing() {
        List<EffectContribution> mirrored = BallisticEnergyMirror.withMirroredBonuses(List.of(node("fafnir", FLAG),
                temporaryNode("burst", 8f, new SkillTypeEffect(damage(WeaponScope.ENERGY), 10f))));

        EffectContribution copy = mirrored.get(mirrored.size() - 1);
        assertEquals(3, mirrored.size());
        assertEquals(8f, copy.temporarySeconds());
        assertEquals(List.of(new SkillTypeEffect(damage(WeaponScope.BALLISTIC), 10f)), copy.effects());
        assertTrue(copy.origin() instanceof BallisticEnergyMirror.Origin);
        assertFalse(copy.countsTowardBonusScale());

        List<EffectContribution> temporaryFlag = List.of(temporaryNode("fafnir", 8f, FLAG),
                node("energy", new SkillTypeEffect(damage(WeaponScope.ENERGY), 10f)));
        assertSame(temporaryFlag, BallisticEnergyMirror.withMirroredBonuses(temporaryFlag));
    }

    @Test
    void mirroredBonusesFromDifferentSourcesGetDistinctModIdPrefixes() {
        List<EffectContribution> mirrored = BallisticEnergyMirror.withMirroredBonuses(List.of(node("fafnir", FLAG),
                EffectContribution.scaledPermanent("a", "same_", List.of(new SkillTypeEffect(damage(WeaponScope.ENERGY), 10f))),
                EffectContribution.scaledPermanent("b", "same_", List.of(new SkillTypeEffect(damage(WeaponScope.NON_BEAM_ENERGY), 5f)))));

        assertEquals(5, mirrored.size());
        assertFalse(mirrored.get(3).modIdPrefix().equals(mirrored.get(4).modIdPrefix()));
        assertTrue(mirrored.get(3).modIdPrefix().startsWith(BallisticEnergyMirror.MOD_ID_PREFIX));
    }

    @Test
    void mirroredRangeCountsTowardsTheBallisticDiminishingReturnsCurve() {
        Map<DiminishedStat, Integer> thresholds = new EnumMap<>(DiminishedStat.class);
        for (DiminishedStat stat : DiminishedStat.values()) {
            thresholds.put(stat, 50);
        }
        EffectTotals totals = EffectTotals.of(List.of(node("fafnir", FLAG),
                        node("energy", new SkillTypeEffect(range(WeaponScope.ENERGY), 40f)),
                        node("ballistic", new SkillTypeEffect(range(WeaponScope.BALLISTIC), 40f))),
                1f, new StatBaseline(1000f, 8000f, 10000f, 500f, 50f), new DiminishingReturns.Config(50, thresholds));

        Map<SkillEffect, Float> permanent = totals.groups().get(0).totals();
        assertEquals(DiminishingReturns.diminished(80f, 50f, 0.5f), permanent.get(range(WeaponScope.BALLISTIC)), 0.001f);
        assertEquals(40f, permanent.get(range(WeaponScope.ENERGY)), 0.001f);
    }
}
