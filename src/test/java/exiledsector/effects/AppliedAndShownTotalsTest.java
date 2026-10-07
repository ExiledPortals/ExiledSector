package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.DamageTakenCaps;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeBonusSummary;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.skilleffect.FluxSkillEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.WeaponSkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AppliedAndShownTotalsTest {

    private static final float TEMPORARY_SECONDS = 60f;
    private static final String TEMPORARY_PREFIX = "For the first 60 seconds after deployment: ";
    private static final float TOLERANCE = 0.001f;

    private ShipSkillData shipData;
    private int nodeCount;

    @BeforeEach
    void setUp() {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        ResolvedTree.clearCache();
        SkillNode root = register(new SkillType.Builder("root", "Low Tech", "a.png", SkillTier.ROOT)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f))).build());
        shipData = new ShipSkillData();
        shipData.chooseStartingRoot(root);
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        ResolvedTree.clearCache();
    }

    @Test
    void cappedDamageTakenReductionsAreAppliedAndShownAtTheCap() {
        allocate(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT, -50f);
        allocate(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT, -50f);
        allocate(DefenseSkillEffect.EMP_DAMAGE_TAKEN_PERCENT, -50f);
        allocate(DefenseSkillEffect.EMP_DAMAGE_TAKEN_MULT, -70f);

        Map<SkillEffect, Float> applied = appliedPermanentTotals(ResolvedTree.of(shipData, HullSize.CRUISER));

        assertEquals(-DamageTakenCaps.MAX_REDUCTION_PERCENT, applied.get(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT), TOLERANCE);
        assertEquals(-DamageTakenCaps.MAX_REDUCTION_PERCENT, applied.get(DefenseSkillEffect.EMP_DAMAGE_TAKEN_MULT), TOLERANCE);
        assertFalse(applied.containsKey(DefenseSkillEffect.EMP_DAMAGE_TAKEN_PERCENT));
        assertShownTotalsMatchApplied(HullSize.CRUISER, 1f);
    }

    @Test
    void temporaryReductionsStayOutOfTheCapWhenAppliedAndShown() {
        allocate(DefenseSkillEffect.KINETIC_DAMAGE_TAKEN_PERCENT, -50f);
        allocate(DefenseSkillEffect.KINETIC_DAMAGE_TAKEN_PERCENT, -50f);
        allocateTemporary(DefenseSkillEffect.KINETIC_DAMAGE_TAKEN_PERCENT, -30f);

        ResolvedTree resolvedTree = ResolvedTree.of(shipData, HullSize.CRUISER);

        assertEquals(-DamageTakenCaps.MAX_REDUCTION_PERCENT,
                appliedPermanentTotals(resolvedTree).get(DefenseSkillEffect.KINETIC_DAMAGE_TAKEN_PERCENT), TOLERANCE);
        assertEquals(-30f, appliedTemporaryTotals(resolvedTree).get(DefenseSkillEffect.KINETIC_DAMAGE_TAKEN_PERCENT), TOLERANCE);
        assertShownTotalsMatchApplied(HullSize.CRUISER, 1f);
    }

    @Test
    void addedMultipliersAreFlooredAtOneHundredPercentLessWhenAppliedAndShown() {
        allocate(FluxSkillEffect.FLUX_CAPACITY_MULT, -75f);
        allocate(FluxSkillEffect.FLUX_CAPACITY_MULT, -50f);

        assertEquals(-100f, appliedPermanentTotals(ResolvedTree.of(shipData, HullSize.CRUISER)).get(FluxSkillEffect.FLUX_CAPACITY_MULT),
                TOLERANCE);
        assertShownTotalsMatchApplied(HullSize.CRUISER, 1f);
    }

    @Test
    void perDModMultipliersAddIntoOnePooledMultiplierWhenAppliedAndShown() {
        allocate(DefenseSkillEffect.ARMOR_DAMAGE_TAKEN_MULT_PER_DMOD, -5f);
        allocate(DefenseSkillEffect.ARMOR_DAMAGE_TAKEN_MULT_PER_DMOD, -5f);
        allocate(WeaponSkillEffect.WEAPON_DAMAGE_MULT_PER_DMOD, 4f);
        allocate(WeaponSkillEffect.WEAPON_DAMAGE_MULT_PER_DMOD, 6f);

        Map<String, Float> pooled = magnitudesByModId(ResolvedTree.of(shipData, HullSize.CRUISER));

        assertEquals(-10f, pooled.get(ResolvedTree.MULTIPLIER_MOD_ID_PREFIX + "ARMOR_DAMAGE_TAKEN_MULT_PER_DMOD"), TOLERANCE);
        assertEquals(10f, pooled.get(ResolvedTree.MULTIPLIER_MOD_ID_PREFIX + "WEAPON_DAMAGE_MULT_PER_DMOD"), TOLERANCE);
        assertShownTotalsMatchApplied(HullSize.CRUISER, 1f);
    }

    @Test
    void temporaryMultipliersCompoundWhenAppliedAndShown() {
        allocateTemporary(FluxSkillEffect.FLUX_CAPACITY_MULT, 10f);
        allocateTemporary(FluxSkillEffect.FLUX_CAPACITY_MULT, 10f);

        assertEquals(21f, appliedTemporaryTotals(ResolvedTree.of(shipData, HullSize.CRUISER)).get(FluxSkillEffect.FLUX_CAPACITY_MULT),
                TOLERANCE);
        assertShownTotalsMatchApplied(HullSize.CRUISER, 1f);
    }

    @Test
    void npcBonusScalingIsAppliedAndShownTheSameWayIncludingTheCap() {
        allocate(DefenseSkillEffect.ARMOR_PERCENT, 30f);
        allocate(DefenseSkillEffect.ARMOR_PERCENT, -10f);
        allocate(DefenseSkillEffect.HULL_MULT, 10f);
        allocate(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT, -30f);
        allocate(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT, -30f);
        allocate(DefenseSkillEffect.FRAGMENTATION_DAMAGE_TAKEN_PERCENT, -20f);

        Map<SkillEffect, Float> applied = appliedPermanentTotals(ResolvedTree.of(shipData, HullSize.CRUISER, 3f));

        assertEquals(30f, applied.get(DefenseSkillEffect.HULL_PERCENT), TOLERANCE);
        assertEquals(80f, applied.get(DefenseSkillEffect.ARMOR_PERCENT), TOLERANCE);
        assertEquals(30f, applied.get(DefenseSkillEffect.HULL_MULT), TOLERANCE);
        assertEquals(-DamageTakenCaps.MAX_REDUCTION_PERCENT, applied.get(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT), TOLERANCE);
        assertShownTotalsMatchApplied(HullSize.CRUISER, 3f);
    }

    private void assertShownTotalsMatchApplied(HullSize hullSize, float bonusScale) {
        ResolvedTree resolvedTree = ResolvedTree.of(shipData, hullSize, bonusScale);
        List<String> expected = new ArrayList<>();
        appliedPermanentTotals(resolvedTree).forEach((effect, total) -> addExpectedLine(expected, "", effect, total));
        appliedTemporaryTotals(resolvedTree).forEach((effect, total) -> addExpectedLine(expected, TEMPORARY_PREFIX, effect, total));
        List<String> shown = new ArrayList<>(SkillTreeBonusSummary.of(shipData, hullSize, bonusScale).bonuses().stream()
                .map(DescriptionLine::plain).toList());
        Collections.sort(expected);
        Collections.sort(shown);
        assertEquals(expected, shown);
    }

    private static void addExpectedLine(List<String> expected, String prefix, SkillEffect effect, float total) {
        float shownTotal = Math.round(total * 100f) / 100f;
        if (!effect.isMultiplicative() || shownTotal != 0f) {
            expected.add(prefix + effect.description(shownTotal).plain());
        }
    }

    private static Map<SkillEffect, Float> appliedPermanentTotals(ResolvedTree resolvedTree) {
        Set<ResolvedTree.EffectEntry> temporaryEntries = temporaryEntries(resolvedTree);
        Map<SkillEffect, Float> totals = new LinkedHashMap<>();
        for (ResolvedTree.Entry entry : resolvedTree.entries()) {
            if (entry instanceof ResolvedTree.EffectEntry effectEntry && !temporaryEntries.contains(effectEntry)) {
                assertFalse(effectEntry.effect().isMultiplicative() && totals.containsKey(effectEntry.effect()),
                        "multipliers should be pooled into one entry: " + effectEntry.effect().name());
                totals.merge(effectEntry.effect(), effectEntry.magnitude(), Float::sum);
            }
        }
        return totals;
    }

    private static Map<SkillEffect, Float> appliedTemporaryTotals(ResolvedTree resolvedTree) {
        Map<SkillEffect, Float> totals = new LinkedHashMap<>();
        for (ResolvedTree.TemporaryNode temporaryNode : resolvedTree.temporaryNodes()) {
            assertEquals(TEMPORARY_SECONDS, temporaryNode.durationSeconds(), TOLERANCE);
            for (ResolvedTree.EffectEntry effectEntry : temporaryNode.effects()) {
                totals.merge(effectEntry.effect(), effectEntry.magnitude(), effectEntry.effect().isMultiplicative()
                        ? (first, second) -> ((1f + first / 100f) * (1f + second / 100f) - 1f) * 100f
                        : Float::sum);
            }
        }
        return totals;
    }

    private static Set<ResolvedTree.EffectEntry> temporaryEntries(ResolvedTree resolvedTree) {
        Set<ResolvedTree.EffectEntry> temporaryEntries = Collections.newSetFromMap(new IdentityHashMap<>());
        resolvedTree.temporaryNodes().forEach(temporaryNode -> temporaryEntries.addAll(temporaryNode.effects()));
        return temporaryEntries;
    }

    private static Map<String, Float> magnitudesByModId(ResolvedTree resolvedTree) {
        Map<String, Float> magnitudes = new LinkedHashMap<>();
        for (ResolvedTree.Entry entry : resolvedTree.entries()) {
            if (entry instanceof ResolvedTree.EffectEntry effectEntry) {
                magnitudes.put(effectEntry.modId(), effectEntry.magnitude());
            }
        }
        return magnitudes;
    }

    private void allocate(SkillEffect effect, float magnitude) {
        allocate(new SkillType.Builder("type_" + nodeCount, "Type", "a.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(effect, magnitude))).build());
    }

    private void allocateTemporary(SkillEffect effect, float magnitude) {
        allocate(new SkillType.Builder("type_" + nodeCount, "Type", "a.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(effect, magnitude))).temporaryAfterDeploymentSeconds(TEMPORARY_SECONDS).build());
    }

    private void allocate(SkillType type) {
        shipData.allocate(register(type), 0);
    }

    private SkillNode register(SkillType type) {
        nodeCount++;
        SkillTree.registerType(type);
        SkillNode node = new SkillNode("node_" + nodeCount, type, List.of(), 0f, 0f);
        SkillTree.register(node);
        return node;
    }
}
