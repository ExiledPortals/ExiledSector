package exiledsector.skills;

import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.defense.DefenseSkillEffect;
import exiledsector.skills.skilleffect.fighter.FighterSkillEffect;
import exiledsector.skills.skilleffect.flux.FluxSkillEffect;
import exiledsector.skills.skilleffect.logistics.LogisticsSkillEffect;
import exiledsector.skills.skilleffect.special.SpecialSkillEffect;
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

    private static Map<SkillEffect, Float> scaled(List<EffectContribution> contributions) {
        Map<SkillEffect, Float> bonuses = new LinkedHashMap<>();
        AffinitySteps.scaledBonuses(contributions, AffinitySteps.totals(contributions)).forEach(bonus -> bonuses.put(bonus.effect(), bonus.magnitude()));
        return bonuses;
    }

    @Test
    void anAffinityScaledEffectAddsItsTargetStatOncePerWholeStepOfItsAffinities() {
        EffectContribution item = EffectContribution.scaledPermanent("item", "item_", List.of(
                new SkillTypeEffect(SpecialSkillEffect.ARMOR_PERCENT_PER_LOW_TECH_AFFINITY, 1f),
                new SkillTypeEffect(SpecialSkillEffect.FIGHTER_DAMAGE_TAKEN_MULT_PER_AFFINITY, -1f)));

        assertEquals(Map.of(DefenseSkillEffect.ARMOR_PERCENT, 2f, FighterSkillEffect.FIGHTER_DAMAGE_TAKEN_MULT, -2f),
                scaled(List.of(item, node(LOW_TECH, 25f), node(HIGH_TECH, 9f))));
        assertEquals(Map.of(FighterSkillEffect.FIGHTER_DAMAGE_TAKEN_MULT, -1f), scaled(List.of(item, node(HIGH_TECH, 9f), node(MIDLINE, 6f))));
        assertEquals(Map.of(), scaled(List.of(item, node(LOW_TECH, 9f))));
    }

    @Test
    void perSModAffinityGoesToTheHighestAndLowestAsHighestRaisesTheLowestAfterwards() {
        EffectContribution vault = EffectContribution.scaledPermanent("vault", "vault_", List.of(
                new SkillTypeEffect(SpecialSkillEffect.HIGHEST_AFFINITY_FLAT_PER_S_MOD, 6f)));
        EffectContribution absence = EffectContribution.scaledPermanent("absence", "absence_", List.of(
                new SkillTypeEffect(SpecialSkillEffect.LOWEST_AFFINITY_AS_HIGHEST, 1f)));

        assertEquals(Map.of(LOW_TECH, 10f, HIGH_TECH, 21f), AffinitySteps.totals(List.of(vault, node(LOW_TECH, 10f), node(HIGH_TECH, 15f))));
        Map<SkillEffect, Float> raised = AffinitySteps.totals(List.of(vault, absence, node(LOW_TECH, 10f), node(HIGH_TECH, 15f)));
        assertEquals(21f, raised.get(HIGH_TECH));
        assertEquals(21f, raised.get(MIDLINE));
        assertEquals(10f, raised.get(LOW_TECH));
    }

    @Test
    void anExchangeEmptiesTheOtherTwoAffinitiesIntoItsOwnAtItsShareAfterLowestAsHighest() {
        EffectContribution exchange = EffectContribution.scaledPermanent("exchange", "exchange_", List.of(
                new SkillTypeEffect(SpecialSkillEffect.AFFINITY_EXCHANGE_TO_HIGH_TECH_PERCENT, 50f)));
        EffectContribution absence = EffectContribution.scaledPermanent("absence", "absence_", List.of(
                new SkillTypeEffect(SpecialSkillEffect.LOWEST_AFFINITY_AS_HIGHEST, 1f)));

        assertEquals(Map.of(LOW_TECH, 0f, HIGH_TECH, 20f, MIDLINE, 0f),
                AffinitySteps.totals(List.of(exchange, node(LOW_TECH, 20f), node(MIDLINE, 10f), node(HIGH_TECH, 5f))));
        assertEquals(Map.of(LOW_TECH, 0f, HIGH_TECH, 5f), AffinitySteps.totals(List.of(exchange, node(LOW_TECH, -4f), node(HIGH_TECH, 5f))));
        Map<SkillEffect, Float> raisedFirst = AffinitySteps.totals(List.of(exchange, absence, node(LOW_TECH, 20f), node(HIGH_TECH, 5f)));
        assertEquals(25f, raisedFirst.get(HIGH_TECH));
        assertEquals(0f, raisedFirst.get(MIDLINE));
    }

    @Test
    void lowTechCountsWholeStepsOfFiveAndFifteen() {
        assertEquals(Map.of(), bonuses(LOW_TECH, 0f));
        assertEquals(Map.of(), bonuses(LOW_TECH, 4f));
        assertEquals(Map.of(DefenseSkillEffect.HULL_MULT, 2f, LogisticsSkillEffect.FUEL_CAPACITY_MULT, 1f), bonuses(LOW_TECH, 5f));
        assertEquals(bonuses(LOW_TECH, 5f), bonuses(LOW_TECH, 9f));
        assertEquals(Map.of(DefenseSkillEffect.HULL_MULT, 4f, LogisticsSkillEffect.FUEL_CAPACITY_MULT, 2f), bonuses(LOW_TECH, 10f));
        assertEquals(Map.of(DefenseSkillEffect.HULL_MULT, 6f, DefenseSkillEffect.ARMOR_MULT, 1f, LogisticsSkillEffect.FUEL_CAPACITY_MULT, 3f),
                bonuses(LOW_TECH, 15f));
        assertEquals(bonuses(LOW_TECH, 10f), bonuses(LOW_TECH, 14f));
        assertEquals(Map.of(DefenseSkillEffect.HULL_MULT, 40f, DefenseSkillEffect.ARMOR_MULT, 6f, LogisticsSkillEffect.FUEL_CAPACITY_MULT, 20f),
                bonuses(LOW_TECH, 100f));
    }

    @Test
    void highTechAndMidlineFollowTheirOwnTables() {
        assertEquals(Map.of(FluxSkillEffect.FLUX_CAPACITY_MULT, 4f, FluxSkillEffect.FLUX_DISSIPATION_MULT, 1f, LogisticsSkillEffect.CREW_CAPACITY_MULT, 2f),
                bonuses(HIGH_TECH, 14f));
        assertEquals(Map.of(FluxSkillEffect.FLUX_CAPACITY_MULT, 2f, DefenseSkillEffect.HULL_MULT, 2f, LogisticsSkillEffect.CARGO_CAPACITY_MULT, 1f),
                bonuses(MIDLINE, 9f));
        assertEquals(Map.of(FluxSkillEffect.FLUX_CAPACITY_MULT, 6f, DefenseSkillEffect.HULL_MULT, 6f, FluxSkillEffect.FLUX_DISSIPATION_MULT, 1f,
                LogisticsSkillEffect.CARGO_CAPACITY_MULT, 3f), bonuses(MIDLINE, 19f));
        assertEquals(Map.of(FluxSkillEffect.FLUX_CAPACITY_MULT, 8f, DefenseSkillEffect.HULL_MULT, 8f, DefenseSkillEffect.ARMOR_MULT, 1f,
                FluxSkillEffect.FLUX_DISSIPATION_MULT, 2f, LogisticsSkillEffect.CARGO_CAPACITY_MULT, 4f), bonuses(MIDLINE, 20f));
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
        totals.put(MIDLINE, 5f);
        Map<SkillEffect, Float> bonuses = new LinkedHashMap<>();
        AffinitySteps.bonuses(totals).forEach(bonus -> bonuses.put(bonus.effect(), bonus.magnitude()));

        assertEquals(4f, bonuses.get(DefenseSkillEffect.HULL_MULT));
    }

    @Test
    void eachLogisticsBonusBelongsToOneAffinity() {
        assertEquals(Map.of(LogisticsSkillEffect.FUEL_CAPACITY_MULT, 1f), logistics(LOW_TECH));
        assertEquals(Map.of(LogisticsSkillEffect.CARGO_CAPACITY_MULT, 1f), logistics(MIDLINE));
        assertEquals(Map.of(LogisticsSkillEffect.CREW_CAPACITY_MULT, 1f), logistics(HIGH_TECH));
    }

    private static Map<SkillEffect, Float> logistics(SkillEffect affinity) {
        Map<SkillEffect, Float> logistics = new LinkedHashMap<>(bonuses(affinity, 5f));
        logistics.keySet().removeIf(effect -> !(effect instanceof LogisticsSkillEffect));
        return logistics;
    }

    @Test
    void effectTotalsSeeTheSteppedBonusesNextToTheAffinityItself() {
        EffectTotals totals = EffectTotals.of(List.of(node(LOW_TECH, 15f), node(HIGH_TECH, 4f)), 1f);
        Map<SkillEffect, Float> permanent = totals.groups().get(0).totals();

        assertEquals(15f, permanent.get(LOW_TECH));
        assertEquals(4f, permanent.get(HIGH_TECH));
        assertEquals(6f, totals.pooledMultipliers().get(DefenseSkillEffect.HULL_MULT));
        assertEquals(1f, totals.pooledMultipliers().get(DefenseSkillEffect.ARMOR_MULT));
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
