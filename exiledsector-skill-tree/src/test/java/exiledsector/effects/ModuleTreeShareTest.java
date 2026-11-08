package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.skills.DiminishingReturns;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.EffectTotals;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.StatBaseline;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.persistence.ShipRecords;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.ModuleShareOverride;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModuleTreeShareTest {

    private static final float SHARE = 0.5f;
    private static final String PREFIX = "exiledSector_moduleShare_parent_";

    private MockedStatic<LunaSettings> lunaSettings;

    @BeforeEach
    void mockSettings() {
        lunaSettings = Mockito.mockStatic(LunaSettings.class);
        lunaSettings.when(() -> LunaSettings.getInt("exiledSector", ModuleShareSetting.FIELD_ID)).thenReturn(null);
        lunaSettings.when(() -> LunaSettings.getInt("exiledSector", ModuleShareSetting.STATION_FIELD_ID)).thenReturn(null);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getCustomData()).thenReturn(new HashMap<>());
        Global.setCombatEngine(engine);
    }

    @AfterEach
    void closeSettings() {
        lunaSettings.close();
        Global.setCombatEngine(null);
        SkillTreeSetting.setEnabledForTests(SkillTreeSetting.DEFAULT_ENABLED);
    }

    private static SkillTypeEffect effect(String name, float magnitude) {
        return new SkillTypeEffect(SkillEffect.byName(name), magnitude);
    }

    private static SkillTreeEffectSource.NodeEffects node(SkillType.Builder type, SkillTypeEffect... resolvedEffects) {
        SkillType skillType = type.effects(List.of(resolvedEffects)).build();
        SkillNode skillNode = mock(SkillNode.class);
        when(skillNode.getType()).thenReturn(skillType);
        when(skillNode.getId()).thenReturn(skillType.getId());
        return new SkillTreeEffectSource.NodeEffects(new AllocatedNode(skillNode, skillType), List.of(resolvedEffects));
    }

    private static SkillTreeEffectSource.NodeEffects node(SkillTypeEffect... resolvedEffects) {
        return node(type("node"), resolvedEffects);
    }

    private static SkillType.Builder type(String id) {
        return new SkillType.Builder(id, id, "icon.png", SkillTier.SMALL);
    }

    private static Map<SkillEffect, Float> totals(ShipVariantAPI moduleVariant, SkillTreeEffectSource.NodeEffects... nodes) {
        return ModuleTreeShare.sharedTotals(List.of(nodes), Set.of(), Map.of(), HullSize.CRUISER, moduleVariant, SHARE);
    }

    private static Map<SkillEffect, Float> totals(SkillTreeEffectSource.NodeEffects... nodes) {
        return totals(mock(ShipVariantAPI.class), nodes);
    }

    private static float total(Map<SkillEffect, Float> totals, String name) {
        return totals.get(SkillEffect.byName(name));
    }

    @Test
    void percentAndFlatValuesScaleByTheShare() {
        Map<SkillEffect, Float> totals = totals(node(effect("FLUX_CAPACITY_PERCENT", 20f), effect("FLUX_DISSIPATION_FLAT", 10f)),
                node(effect("FLUX_CAPACITY_PERCENT", 10f)));

        assertEquals(15f, total(totals, "FLUX_CAPACITY_PERCENT"), 1e-4f);
        assertEquals(5f, total(totals, "FLUX_DISSIPATION_FLAT"), 1e-4f);
    }

    @Test
    void multipliersPoolAcrossNodesAndAreFlooredBeforeTheShare() {
        assertEquals(25f, total(totals(node(effect("FLUX_CAPACITY_MULT", 20f)), node(effect("FLUX_CAPACITY_MULT", 30f))), "FLUX_CAPACITY_MULT"), 1e-4f);
        assertEquals(-50f, total(totals(node(effect("SHIELD_UPKEEP_MULT", -80f)), node(effect("SHIELD_UPKEEP_MULT", -60f))), "SHIELD_UPKEEP_MULT"), 1e-4f);
    }

    @Test
    void aParentAtTheDamageTakenCapGivesTheModuleTheShareOfTheCap() {
        Map<SkillEffect, Float> totals = totals(node(effect("EMP_DAMAGE_TAKEN_PERCENT", -60f)), node(effect("EMP_DAMAGE_TAKEN_MULT", -60f)));

        assertEquals(-40f, total(totals, "EMP_DAMAGE_TAKEN_MULT"), 1e-4f);
        assertFalse(totals.containsKey(SkillEffect.byName("EMP_DAMAGE_TAKEN_PERCENT")));
    }

    @Test
    void skippedEffectsAndExcludedNodesGiveNothing() {
        Map<SkillEffect, Float> totals = totals(node(effect("TOP_SPEED_PERCENT", 20f), effect("FLUX_CAPACITY_PERCENT", 10f)),
                node(effect("WEAPON_DAMAGE_PERCENT", -20f), effect("SHIELD_DAMAGE_SHARED_PERCENT", 20f)));

        assertEquals(Map.of(SkillEffect.byName("FLUX_CAPACITY_PERCENT"), 5f), totals);
    }

    @Test
    void temporaryNodesPassthroughsAndNoneOverridesGiveNothing() {
        assertTrue(totals(node(type("burst").temporaryAfterDeploymentSeconds(30f), effect("FLUX_CAPACITY_PERCENT", 10f))).isEmpty());
        assertTrue(totals(node(type("passthrough").vanillaHullModId("missile_autoloader"), effect("FLUX_CAPACITY_PERCENT", 10f))).isEmpty());
        assertTrue(totals(node(type("none").moduleShareOverride(ModuleShareOverride.NONE), effect("FLUX_CAPACITY_PERCENT", 10f))).isEmpty());
    }

    @Test
    void aModuleWithTheNodesHullModOfItsOwnGetsNothingFromThatNode() {
        ShipVariantAPI builtInTargetingCore = mock(ShipVariantAPI.class);
        when(builtInTargetingCore.hasHullMod("dedicated_targeting_core")).thenReturn(true);
        ShipVariantAPI fittedExclusive = mock(ShipVariantAPI.class);
        when(fittedExclusive.hasHullMod("targetingunit")).thenReturn(true);
        SkillTreeEffectSource.NodeEffects targetingCore = node(type("dtc").phantomHullModIds(List.of("dedicated_targeting_core"))
                .exclusiveHullModIds(List.of("targetingunit")), effect("BALLISTIC_WEAPON_RANGE_PERCENT", 40f));
        SkillTreeEffectSource.NodeEffects flux = node(effect("FLUX_CAPACITY_PERCENT", 10f));

        assertEquals(Map.of(SkillEffect.byName("FLUX_CAPACITY_PERCENT"), 5f), totals(builtInTargetingCore, targetingCore, flux));
        assertEquals(Map.of(SkillEffect.byName("FLUX_CAPACITY_PERCENT"), 5f), totals(fittedExclusive, targetingCore, flux));
        assertEquals(20f, total(totals(targetingCore, flux), "BALLISTIC_WEAPON_RANGE_PERCENT"), 1e-4f);
    }

    @Test
    void affinityBonusesReachTheModuleScaledTheWayTheParentsWere() {
        SkillTypeEffect lowTech = effect("LOW_TECH_AFFINITY_FLAT", 30f);
        EffectTotals parentTotals = EffectTotals.of(List.of(EffectContribution.scaledPermanent("node", "prefix_", List.of(lowTech))), 0.5f);
        Map<SkillEffect, Float> ratios = ModuleTreeShare.affinityBonusRatios(parentTotals.contributions());
        float parentHull = parentTotals.pooledMultipliers().get(SkillEffect.byName("HULL_MULT"));

        assertTrue(ratios.get(SkillEffect.byName("HULL_MULT")) < 1f);
        Map<SkillEffect, Float> moduleTotals = ModuleTreeShare.sharedTotals(List.of(node(lowTech)), Set.of(), ratios, HullSize.CRUISER,
                mock(ShipVariantAPI.class), SHARE);
        assertEquals(parentHull * SHARE, total(moduleTotals, "HULL_MULT"), 1e-4f);
    }

    @Test
    void anAffinityTheParentExchangedAwayGivesTheModuleNoBonus() {
        SkillTypeEffect lowTech = effect("LOW_TECH_AFFINITY_FLAT", 30f);
        EffectTotals parentTotals = EffectTotals.of(List.of(EffectContribution.scaledPermanent("node", "prefix_", List.of(lowTech)),
                EffectContribution.scaledPermanent("item", "item_", List.of(effect("AFFINITY_EXCHANGE_TO_HIGH_TECH_PERCENT", 50f)))), 1f);
        Map<SkillEffect, Float> ratios = ModuleTreeShare.affinityBonusRatios(parentTotals.contributions());

        Map<SkillEffect, Float> moduleTotals = ModuleTreeShare.sharedTotals(List.of(node(lowTech)), Set.of(), ratios, HullSize.CRUISER,
                mock(ShipVariantAPI.class), SHARE);
        assertEquals(0f, moduleTotals.getOrDefault(SkillEffect.byName("HULL_MULT"), 0f), 1e-4f);
    }

    @Test
    void anUnscaledParentSharesItsSteppedAffinityBonuses() {
        assertEquals(6f, total(totals(node(effect("LOW_TECH_AFFINITY_FLAT", 30f))), "HULL_MULT"), 1e-4f);
    }

    private static void registerParent(boolean station, SkillTreeEffectSource.NodeEffects... nodes) {
        ShipVariantAPI parentVariant = ModuleVariants.parent(VariantSource.REFIT, Map.of("WS0001", ModuleVariants.variant(VariantSource.REFIT)));
        FleetMemberAPI parentMember = mock(FleetMemberAPI.class);
        when(parentMember.getId()).thenReturn("parent");
        when(parentMember.isStation()).thenReturn(station);
        ShipEffectContext parentContext = new ShipEffectContext(new ShipSkillData(), parentMember, parentVariant, HullSize.CRUISER);
        List<EffectContribution> contributions = new ArrayList<>();
        for (SkillTreeEffectSource.NodeEffects node : nodes) {
            contributions.add(new EffectContribution(node.node(), "exiledSector_skill_" + node.node().node().getId() + "_", node.effects(),
                    null, null, true, true));
        }
        ShipEffectSource parentTree = new ShipEffectSource() {
            @Override
            public List<EffectContribution> contributions(ShipEffectContext context) {
                return contributions;
            }
        };
        ModuleParents.register(parentContext, ResolvedTree.of(parentContext, 1f, StatBaseline.NONE, DiminishingReturns.OFF, List.of(parentTree)));
    }

    private static MutableShipStatsAPI buildModule(ShipVariantAPI moduleVariant) {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(stats.getFleetMember()).thenReturn(mock(FleetMemberAPI.class));
        when(stats.getVariant()).thenReturn(moduleVariant);
        new ModuleHullMod().applyEffectsBeforeShipCreation(HullSize.CRUISER, stats, ModuleHullMod.ID);
        return stats;
    }

    private static MutableShipStatsAPI buildModule() {
        return buildModule(ModuleVariants.linkedModule("parent"));
    }

    @Test
    void aModuleBuildsWithTheShareOfTheParentsTreeAndTheModuleBonusesAtFullStrength() {
        registerParent(false, node(type("caps"), effect("FLUX_CAPACITY_PERCENT", 20f), effect("HULL_PERCENT", 10f)),
                node(type("bulkheads"), effect("MODULE_HULL_PERCENT", 20f), effect("MODULE_ARMOR_PERCENT", 10f)));

        MutableShipStatsAPI stats = buildModule();

        verify(stats.getFluxCapacity()).modifyPercent(PREFIX + "FLUX_CAPACITY_PERCENT", 10f);
        verify(stats.getHullBonus()).modifyPercent(PREFIX + "HULL_PERCENT", 25f);
        verify(stats.getArmorBonus()).modifyPercent(PREFIX + "ARMOR_PERCENT", 10f);
    }

    @Test
    void aStationsModulesGetTheStationShare() {
        registerParent(true, node(effect("FLUX_CAPACITY_PERCENT", 20f)));

        verify(buildModule().getFluxCapacity()).modifyPercent(PREFIX + "FLUX_CAPACITY_PERCENT", 20f);

        lunaSettings.when(() -> LunaSettings.getInt("exiledSector", ModuleShareSetting.STATION_FIELD_ID)).thenReturn(25);
        verify(buildModule().getFluxCapacity()).modifyPercent(PREFIX + "FLUX_CAPACITY_PERCENT", 5f);
    }

    @Test
    void withTheShareOffModulesStillGetTheModuleBonuses() {
        lunaSettings.when(() -> LunaSettings.getInt("exiledSector", ModuleShareSetting.FIELD_ID)).thenReturn(0);
        registerParent(false, node(effect("FLUX_CAPACITY_PERCENT", 20f), effect("MODULE_ARMOR_PERCENT", 20f)));

        MutableShipStatsAPI stats = buildModule();

        verify(stats.getFluxCapacity(), never()).modifyPercent(anyString(), anyFloat());
        verify(stats.getArmorBonus()).modifyPercent(PREFIX + "ARMOR_PERCENT", 20f);
    }

    @Test
    void withTheSkillTreeSwitchedOffModulesBuildAtTheirPlainStats() {
        registerParent(false, node(effect("FLUX_CAPACITY_PERCENT", 20f), effect("MODULE_HULL_PERCENT", 20f)));
        SkillTreeSetting.setEnabledForTests(false);

        MutableShipStatsAPI stats = buildModule();

        verify(stats.getFluxCapacity(), never()).modifyPercent(anyString(), anyFloat());
        verify(stats.getHullBonus(), never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void aDockedShipWithATreeOfItsOwnGetsNoShareOfTheParentsTree() {
        registerParent(false, node(effect("FLUX_CAPACITY_PERCENT", 20f), effect("MODULE_HULL_PERCENT", 20f)));
        ShipVariantAPI dockedCore = ModuleVariants.linkedModule("parent");
        dockedCore.addTag(ShipRecords.SHIP_TAG_PREFIX + "docked-core");

        MutableShipStatsAPI stats;
        try (MockedStatic<ShipSkillDataManager> records = Mockito.mockStatic(ShipSkillDataManager.class)) {
            records.when(() -> ShipSkillDataManager.find("docked-core")).thenReturn(new ShipSkillData());
            stats = buildModule(dockedCore);
        }

        verify(stats.getFluxCapacity(), never()).modifyPercent(anyString(), anyFloat());
        verify(stats.getHullBonus(), never()).modifyPercent(anyString(), anyFloat());
    }
}
