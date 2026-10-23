package exiledlib.skills;

import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.defense.DefenseSkillEffect;
import exiledlib.skills.skilleffect.flux.FluxSkillEffect;
import exiledlib.skills.skilleffect.logistics.LogisticsSkillEffect;
import exiledlib.skills.skilleffect.special.SpecialSkillEffect;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AffinityStepsTest {

    private static final SkillEffect LOW_TECH = SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT;
    private static final SkillEffect HIGH_TECH = SpecialSkillEffect.HIGH_TECH_AFFINITY_FLAT;
    private static final SkillEffect MIDLINE = SpecialSkillEffect.MIDLINE_AFFINITY_FLAT;

    private static Map<SkillEffect, Float> bonuses(SkillEffect affinity, float total) {
        Map<SkillEffect, Float> bonuses = new LinkedHashMap<>();
        AffinitySteps.bonuses(Map.of(affinity, total)).forEach(bonus -> bonuses.put(bonus.effect(), bonus.magnitude()));
        return bonuses;
    }

    private static EffectContribution node(SkillEffect affinity, float amount) {
        return EffectContribution.scaledPermanent("node", "node_", List.of(new SkillTypeEffect(affinity, amount)));
    }

    @Test
    void lowTechCountsWholeStepsOfFiveAndTen() {
        assertEquals(Map.of(), bonuses(LOW_TECH, 0f));
        assertEquals(Map.of(), bonuses(LOW_TECH, 4f));
        assertEquals(Map.of(DefenseSkillEffect.HULL_MULT, 2f, LogisticsSkillEffect.CARGO_CAPACITY_MULT, 1f,
                LogisticsSkillEffect.FUEL_CAPACITY_MULT, 1f, LogisticsSkillEffect.CREW_CAPACITY_MULT, 1f), bonuses(LOW_TECH, 5f));
        assertEquals(bonuses(LOW_TECH, 5f), bonuses(LOW_TECH, 9f));
        assertEquals(Map.of(DefenseSkillEffect.HULL_MULT, 4f, DefenseSkillEffect.ARMOR_MULT, 2f, LogisticsSkillEffect.CARGO_CAPACITY_MULT, 2f,
                LogisticsSkillEffect.FUEL_CAPACITY_MULT, 2f, LogisticsSkillEffect.CREW_CAPACITY_MULT, 2f), bonuses(LOW_TECH, 10f));
        assertEquals(bonuses(LOW_TECH, 10f), bonuses(LOW_TECH, 14f));
        assertEquals(Map.of(DefenseSkillEffect.HULL_MULT, 40f, DefenseSkillEffect.ARMOR_MULT, 20f, LogisticsSkillEffect.CARGO_CAPACITY_MULT, 20f,
                LogisticsSkillEffect.FUEL_CAPACITY_MULT, 20f, LogisticsSkillEffect.CREW_CAPACITY_MULT, 20f), bonuses(LOW_TECH, 100f));
    }

    @Test
    void highTechAndMidlineFollowTheirOwnTables() {
        assertEquals(Map.of(FluxSkillEffect.FLUX_CAPACITY_MULT, 4f, FluxSkillEffect.FLUX_DISSIPATION_MULT, 2f,
                LogisticsSkillEffect.CARGO_CAPACITY_MULT, 2f, LogisticsSkillEffect.FUEL_CAPACITY_MULT, 2f, LogisticsSkillEffect.CREW_CAPACITY_MULT, 2f),
                bonuses(HIGH_TECH, 14f));
        assertEquals(Map.of(FluxSkillEffect.FLUX_CAPACITY_MULT, 2f, DefenseSkillEffect.HULL_MULT, 2f,
                LogisticsSkillEffect.CARGO_CAPACITY_MULT, 1f, LogisticsSkillEffect.FUEL_CAPACITY_MULT, 1f, LogisticsSkillEffect.CREW_CAPACITY_MULT, 1f),
                bonuses(MIDLINE, 9f));
        assertEquals(Map.of(FluxSkillEffect.FLUX_CAPACITY_MULT, 4f, DefenseSkillEffect.HULL_MULT, 4f, DefenseSkillEffect.ARMOR_MULT, 1f,
                FluxSkillEffect.FLUX_DISSIPATION_MULT, 1f, LogisticsSkillEffect.CARGO_CAPACITY_MULT, 2f, LogisticsSkillEffect.FUEL_CAPACITY_MULT, 2f,
                LogisticsSkillEffect.CREW_CAPACITY_MULT, 2f), bonuses(MIDLINE, 10f));
    }

    @Test
    void affinityFromSeveralSourcesIsSummedBeforeStepping() {
        List<EffectContribution> twoNodes = List.of(node(LOW_TECH, 5f), node(LOW_TECH, 5f));
        List<EffectContribution> oneSource = List.of(node(LOW_TECH, 10f));

        assertEquals(Map.of(LOW_TECH, 10f), AffinitySteps.totals(twoNodes));
        assertEquals(AffinitySteps.bonuses(AffinitySteps.totals(oneSource)), AffinitySteps.bonuses(AffinitySteps.totals(twoNodes)));
    }

    @Test
    void differentAffinitiesStackTheirSharedBonuses() {
        Map<SkillEffect, Float> totals = new LinkedHashMap<>();
        totals.put(LOW_TECH, 5f);
        totals.put(HIGH_TECH, 5f);
        Map<SkillEffect, Float> bonuses = new LinkedHashMap<>();
        AffinitySteps.bonuses(totals).forEach(bonus -> bonuses.put(bonus.effect(), bonus.magnitude()));

        assertEquals(2f, bonuses.get(LogisticsSkillEffect.CARGO_CAPACITY_MULT));
    }

    @Test
    void effectTotalsSeeTheSteppedBonusesNextToTheAffinityItself() {
        EffectTotals totals = EffectTotals.of(List.of(node(LOW_TECH, 10f), node(HIGH_TECH, 4f)), 1f);
        Map<SkillEffect, Float> permanent = totals.groups().get(0).totals();

        assertEquals(10f, permanent.get(LOW_TECH));
        assertEquals(4f, permanent.get(HIGH_TECH));
        assertEquals(4f, totals.pooledMultipliers().get(DefenseSkillEffect.HULL_MULT));
        assertEquals(2f, totals.pooledMultipliers().get(DefenseSkillEffect.ARMOR_MULT));
        assertNull(totals.pooledMultipliers().get(FluxSkillEffect.FLUX_CAPACITY_MULT));
        assertTrue(totals.contributions().stream().anyMatch(contribution -> contribution.origin() instanceof AffinitySteps.Origin));
    }

    @Test
    void temporaryAffinityAndShipsWithoutAffinityAddNothing() {
        EffectContribution temporary = new EffectContribution("node", "node_", List.of(new SkillTypeEffect(LOW_TECH, 10f)), 30f, null, true, true);

        assertEquals(Map.of(), AffinitySteps.totals(List.of(temporary)));
        assertFalse(EffectTotals.of(List.of(temporary), 1f).contributions().stream()
                .anyMatch(contribution -> contribution.origin() instanceof AffinitySteps.Origin));
    }

    @Test
    void affinityItselfAppliesNoStatAndIsNeverNpcScaled() {
        for (SkillEffect affinity : AffinitySteps.affinities()) {
            assertNull(affinity.statMode(), affinity.name());
            assertFalse(NpcBonusScaling.isScalableBonus(affinity, 5f), affinity.name());
        }
    }
}
