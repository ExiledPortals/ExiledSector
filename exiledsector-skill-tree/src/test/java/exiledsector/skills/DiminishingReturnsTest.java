package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.effects.SkillTreeEffectSource;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.skilleffect.FluxSkillEffect;
import exiledsector.skills.skilleffect.ScopedWeaponEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.WeaponScope;
import exiledsector.skills.skilleffect.WeaponStatFamily;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiminishingReturnsTest {

    private static final StatBaseline CRUISER = new StatBaseline(1000f, 8000f, 10000f, 500f, 50f);
    private static final DiminishingReturns.Config HALF_PAST_FIFTY = config(50, 50);

    private ShipSkillData data;

    @BeforeEach
    void setUp() {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        SkillNode root = register("root_1", new SkillType.Builder("root", "Root", "a.png", SkillTier.ROOT).build());
        data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    private static DiminishingReturns.Config config(int efficiencyPercent, int thresholdPercent) {
        Map<DiminishedStat, Integer> thresholds = new EnumMap<>(DiminishedStat.class);
        for (DiminishedStat stat : DiminishedStat.values()) {
            thresholds.put(stat, thresholdPercent);
        }
        return new DiminishingReturns.Config(efficiencyPercent, thresholds);
    }

    private static SkillNode register(String id, SkillType type) {
        SkillTree.registerType(type);
        SkillNode node = new SkillNode(id, type, List.of(), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    private void allocate(String nodeId, SkillType.Builder type) {
        ShipTrees.allocate(data, register(nodeId, type.build()), 0);
    }

    private static SkillType.Builder small(String id, SkillTypeEffect... effects) {
        return new SkillType.Builder(id, id, "a.png", SkillTier.SMALL).effects(List.of(effects));
    }

    private static SkillEffect range(WeaponScope scope) {
        return ScopedWeaponEffect.find(WeaponStatFamily.RANGE, scope, StatMode.PERCENT);
    }

    private Map<SkillEffect, Float> permanentTotals(DiminishingReturns.Config config) {
        return totals(config).groups().get(0).totals();
    }

    private EffectTotals totals(DiminishingReturns.Config config) {
        return EffectTotals.of(SkillTreeEffectSource.contributions(data, HullSize.CRUISER), 1f, CRUISER, config);
    }

    @Test
    void bonusesUpToTheThresholdCountInFullAndTheRestAtTheEfficiency() {
        assertEquals(40f, DiminishingReturns.diminished(40f, 50f, 0.5f), 0.001f);
        assertEquals(75f, DiminishingReturns.diminished(100f, 50f, 0.5f), 0.001f);
        assertEquals(50f, DiminishingReturns.diminished(100f, 50f, 0f), 0.001f);
    }

    private static Map<SkillEffect, Float> ratios(Map<SkillEffect, Float> bonuses) {
        return DiminishingReturns.ratios(HALF_PAST_FIFTY, CRUISER, new DiminishingReturns.Sums(bonuses, Map.of()));
    }

    @Test
    void flatBonusesCountAsThePercentTheyAddToTheHullsBaseValue() {
        Map<SkillEffect, Float> ratios = ratios(Map.of(DefenseSkillEffect.ARMOR_FLAT, 1000f, DefenseSkillEffect.ARMOR_PERCENT, 28f));

        assertEquals(89f / 128f, ratios.get(DefenseSkillEffect.ARMOR_PERCENT), 0.0001f);
        assertEquals(89f / 128f, ratios.get(DefenseSkillEffect.ARMOR_FLAT), 0.0001f);
    }

    @Test
    void fullEfficiencyTurnsDiminishingReturnsOff() {
        assertTrue(DiminishingReturns.ratios(config(100, 0), CRUISER,
                new DiminishingReturns.Sums(Map.of(DefenseSkillEffect.ARMOR_PERCENT, 500f), Map.of())).isEmpty());
    }

    @Test
    void separateWeaponTypesEachGetTheirOwnThreshold() {
        assertTrue(ratios(Map.of(range(WeaponScope.BALLISTIC), 40f, range(WeaponScope.MISSILE), 40f)).isEmpty());
    }

    @Test
    void stackedRangeScopesShrinkUntilEachWeaponTypeMeetsItsOwnCurve() {
        Map<SkillEffect, Float> ratios = ratios(Map.of(range(WeaponScope.ENERGY), 40f, range(WeaponScope.BEAM), 40f));

        float nonBeamEnergyRange = 40f * ratios.getOrDefault(range(WeaponScope.ENERGY), 1f);
        float beamRange = nonBeamEnergyRange + 40f * ratios.getOrDefault(range(WeaponScope.BEAM), 1f);
        assertEquals(DiminishingReturns.diminished(40f, 50f, 0.5f), nonBeamEnergyRange, 0.001f);
        assertEquals(DiminishingReturns.diminished(80f, 50f, 0.5f), beamRange, 0.001f);
    }

    @Test
    void rangeForWeaponTypesUnderTheThresholdIsNotCutByAnotherTypesStack() {
        Map<SkillEffect, Float> ratios = ratios(Map.of(range(WeaponScope.ALL), 20f, range(WeaponScope.BEAM), 60f));

        assertTrue(!ratios.containsKey(range(WeaponScope.ALL)), ratios.toString());
        assertEquals(45f / 60f, ratios.get(range(WeaponScope.BEAM)), 0.0001f);
    }

    @Test
    void theThresholdIsMeasuredOnTheNetTotalAfterDrawbacks() {
        Map<SkillEffect, Float> underNet = DiminishingReturns.ratios(HALF_PAST_FIFTY, CRUISER,
                new DiminishingReturns.Sums(Map.of(DefenseSkillEffect.ARMOR_PERCENT, 80f), Map.of(DefenseSkillEffect.ARMOR_PERCENT, -40f)));

        assertTrue(underNet.isEmpty(), underNet.toString());
    }

    @Test
    void permanentBonusesPastTheThresholdShrinkButDrawbacksStayAsWritten() {
        SkillType.Builder armour = small("armour", new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, 60f));
        allocate("armour_1", armour);
        allocate("armour_2", armour);
        allocate("brittle", small("brittle", new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, -40f)));

        EffectTotals totals = totals(HALF_PAST_FIFTY);

        assertEquals(DiminishingReturns.diminished(80f, 50f, 0.5f), totals.groups().get(0).totals().get(DefenseSkillEffect.ARMOR_PERCENT), 0.001f);
        assertEquals(Set.of(DiminishedStat.ARMOUR), totals.diminishedStats());
    }

    @Test
    void pooledMultipliersShrinkWithTheirStat() {
        SkillType.Builder flux = small("flux", new SkillTypeEffect(FluxSkillEffect.FLUX_CAPACITY_MULT, 40f));
        allocate("flux_1", flux);
        allocate("flux_2", flux);

        assertEquals(65f, totals(HALF_PAST_FIFTY).pooledMultipliers().get(FluxSkillEffect.FLUX_CAPACITY_MULT), 0.001f);
    }

    @Test
    void temporaryNodesNeitherCountTowardsNorLoseToTheThreshold() {
        allocate("steady", small("steady", new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, 50f)));
        allocate("burst", small("burst", new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, 100f)).temporaryAfterDeploymentSeconds(30f));

        EffectTotals totals = totals(HALF_PAST_FIFTY);

        assertTrue(totals.diminishedStats().isEmpty());
        assertEquals(50f, permanentTotals(HALF_PAST_FIFTY).get(DefenseSkillEffect.ARMOR_PERCENT), 0.001f);
        assertEquals(100f, totals.groups().get(1).totals().get(DefenseSkillEffect.ARMOR_PERCENT), 0.001f);
    }

    @Test
    void theDefaultOverloadsLeaveTotalsUntouched() {
        SkillType.Builder armour = small("armour", new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, 80f));
        allocate("armour_1", armour);
        allocate("armour_2", armour);

        EffectTotals totals = EffectTotals.of(SkillTreeEffectSource.contributions(data, HullSize.CRUISER), 1f);

        assertEquals(160f, totals.groups().get(0).totals().get(DefenseSkillEffect.ARMOR_PERCENT), 0.001f);
    }
}
