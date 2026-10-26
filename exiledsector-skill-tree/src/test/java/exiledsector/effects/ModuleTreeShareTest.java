package exiledsector.effects;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import exiledlib.skills.EffectContribution;
import exiledlib.skills.EffectTotals;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.SkillEffect;
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
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModuleTreeShareTest {

    private static final float SHARE = 0.5f;

    private MockedStatic<LunaSettings> lunaSettings;

    @BeforeEach
    void mockSettings() {
        lunaSettings = Mockito.mockStatic(LunaSettings.class);
        lunaSettings.when(() -> LunaSettings.getInt("exiledSector", ModuleShareSetting.FIELD_ID)).thenReturn(null);
        lunaSettings.when(() -> LunaSettings.getInt("exiledSector", ModuleShareSetting.STATION_FIELD_ID)).thenReturn(null);
    }

    @AfterEach
    void closeSettings() {
        lunaSettings.close();
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
        return ModuleTreeShare.sharedTotals(List.of(nodes), Map.of(), HullSize.CRUISER, moduleVariant, SHARE);
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
        Map<SkillEffect, Float> moduleTotals = ModuleTreeShare.sharedTotals(List.of(node(lowTech)), ratios, HullSize.CRUISER,
                mock(ShipVariantAPI.class), SHARE);
        assertEquals(parentHull * SHARE, total(moduleTotals, "HULL_MULT"), 1e-4f);
    }

    @Test
    void anAffinityTheParentExchangedAwayGivesTheModuleNoBonus() {
        SkillTypeEffect lowTech = effect("LOW_TECH_AFFINITY_FLAT", 30f);
        EffectTotals parentTotals = EffectTotals.of(List.of(EffectContribution.scaledPermanent("node", "prefix_", List.of(lowTech)),
                EffectContribution.scaledPermanent("item", "item_", List.of(effect("AFFINITY_EXCHANGE_TO_HIGH_TECH_PERCENT", 50f)))), 1f);
        Map<SkillEffect, Float> ratios = ModuleTreeShare.affinityBonusRatios(parentTotals.contributions());

        Map<SkillEffect, Float> moduleTotals = ModuleTreeShare.sharedTotals(List.of(node(lowTech)), ratios, HullSize.CRUISER,
                mock(ShipVariantAPI.class), SHARE);
        assertEquals(0f, moduleTotals.getOrDefault(SkillEffect.byName("HULL_MULT"), 0f), 1e-4f);
    }

    @Test
    void anUnscaledParentSharesItsSteppedAffinityBonuses() {
        assertEquals(6f, total(totals(node(effect("LOW_TECH_AFFINITY_FLAT", 30f))), "HULL_MULT"), 1e-4f);
    }

    @Test
    void theFirstTickPushesTheShareOntoLivingModulesOnceUnderTheParentsModId() {
        ShipAPI parent = mock(ShipAPI.class);
        when(parent.getId()).thenReturn("parent");
        when(parent.getMutableStats()).thenReturn(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS));
        ShipAPI module = module(true);
        ShipAPI wreck = module(false);
        when(parent.getChildModulesCopy()).thenReturn(new ArrayList<>(List.of(module, wreck)));
        ModuleTreeShare share = new ModuleTreeShare(parent, List.of(node(effect("FLUX_CAPACITY_PERCENT", 20f), effect("ARMOR_PERCENT", 10f))),
                Map.of(), HullSize.CRUISER);

        share.advance(0.1f);
        share.advance(0.1f);

        MutableShipStatsAPI stats = module.getMutableStats();
        verify(stats.getFluxCapacity(), times(1)).modifyPercent("exiledSector_moduleShare_parent_FLUX_CAPACITY_PERCENT", 10f);
        verify(stats.getArmorBonus(), times(1)).modifyPercent("exiledSector_moduleShare_parent_ARMOR_PERCENT", 5f);
        verify(wreck.getMutableStats().getFluxCapacity(), never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void aDockedShipWithATreeOfItsOwnGetsNoShareOfTheParentsTree() {
        ShipAPI parent = mock(ShipAPI.class);
        when(parent.getId()).thenReturn("parent");
        when(parent.getMutableStats()).thenReturn(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS));
        ShipAPI dockedCore = module(true);
        when(dockedCore.getVariant().getTags()).thenReturn(List.of(ShipRecords.SHIP_TAG_PREFIX + "docked-core"));
        ShipAPI plate = module(true);
        when(parent.getChildModulesCopy()).thenReturn(new ArrayList<>(List.of(dockedCore, plate)));

        try (MockedStatic<ShipSkillDataManager> records = Mockito.mockStatic(ShipSkillDataManager.class)) {
            records.when(() -> ShipSkillDataManager.find("docked-core")).thenReturn(new ShipSkillData());
            new ModuleTreeShare(parent, List.of(node(effect("FLUX_CAPACITY_PERCENT", 20f))), Map.of(), HullSize.CRUISER).advance(0.1f);
        }

        verify(dockedCore.getMutableStats().getFluxCapacity(), never()).modifyPercent(anyString(), anyFloat());
        verify(plate.getMutableStats().getFluxCapacity()).modifyPercent("exiledSector_moduleShare_parent_FLUX_CAPACITY_PERCENT", 10f);
    }

    @Test
    void attachesOnceAndOnlyToShipsAndStationsWithModuleSlots() {
        ShipAPI modular = ship(true);
        ShipAPI plain = ship(false);
        ShipAPI station = ship(true);
        when(station.isStation()).thenReturn(true);
        ResolvedTree tree = mock(ResolvedTree.class);

        ModuleTreeShare.attach(modular, tree);
        when(modular.getListeners(ModuleTreeShare.class)).thenReturn(List.of(mock(ModuleTreeShare.class)));
        ModuleTreeShare.attach(modular, tree);
        ModuleTreeShare.attach(plain, tree);
        ModuleTreeShare.attach(station, tree);

        ArgumentCaptor<Object> added = ArgumentCaptor.forClass(Object.class);
        verify(modular, times(1)).addListener(added.capture());
        assertTrue(added.getValue() instanceof AdvanceableListener);
        verify(plain, never()).addListener(any());
        verify(station, times(1)).addListener(any());
    }

    @Test
    void aStationsModulesGetTheStationShare() {
        ShipAPI station = mock(ShipAPI.class);
        when(station.getId()).thenReturn("station");
        when(station.isStation()).thenReturn(true);
        when(station.getMutableStats()).thenReturn(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS));
        ShipAPI module = module(true);
        when(station.getChildModulesCopy()).thenReturn(new ArrayList<>(List.of(module)));
        new ModuleTreeShare(station, List.of(node(effect("FLUX_CAPACITY_PERCENT", 20f))), Map.of(), HullSize.CAPITAL_SHIP).advance(0.1f);

        verify(module.getMutableStats().getFluxCapacity()).modifyPercent("exiledSector_moduleShare_station_FLUX_CAPACITY_PERCENT", 20f);

        lunaSettings.when(() -> LunaSettings.getInt("exiledSector", ModuleShareSetting.STATION_FIELD_ID)).thenReturn(25);
        ShipAPI quarterModule = module(true);
        when(station.getChildModulesCopy()).thenReturn(new ArrayList<>(List.of(quarterModule)));
        new ModuleTreeShare(station, List.of(node(effect("FLUX_CAPACITY_PERCENT", 20f))), Map.of(), HullSize.CAPITAL_SHIP).advance(0.1f);

        verify(quarterModule.getMutableStats().getFluxCapacity()).modifyPercent("exiledSector_moduleShare_station_FLUX_CAPACITY_PERCENT", 5f);
    }

    @Test
    void withTheShareOffOnlyShipsWithTaniaModuleNodesAttach() {
        ShipAPI plain = ship(true);
        when(plain.getMutableStats()).thenReturn(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS));
        ShipAPI tania = ship(true);
        MutableShipStatsAPI taniaStats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(taniaStats.getDynamic().getValue("exiledLib_moduleArmorPercent", 0f)).thenReturn(20f);
        when(tania.getMutableStats()).thenReturn(taniaStats);

        lunaSettings.when(() -> LunaSettings.getInt("exiledSector", ModuleShareSetting.FIELD_ID)).thenReturn(0);
        ModuleTreeShare.attach(plain, mock(ResolvedTree.class));
        ModuleTreeShare.attach(tania, mock(ResolvedTree.class));

        verify(plain, never()).addListener(any());
        verify(tania, times(1)).addListener(any());
    }

    private static ShipAPI module(boolean alive) {
        ShipAPI module = mock(ShipAPI.class);
        when(module.isAlive()).thenReturn(alive);
        when(module.getVariant()).thenReturn(mock(ShipVariantAPI.class));
        when(module.getHullSpec()).thenReturn(mock(ShipHullSpecAPI.class));
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(module.getMutableStats()).thenReturn(stats);
        return module;
    }

    private static ShipAPI ship(boolean withModuleSlot) {
        ShipAPI ship = mock(ShipAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        WeaponSlotAPI slot = mock(WeaponSlotAPI.class);
        when(slot.isStationModule()).thenReturn(withModuleSlot);
        when(hullSpec.getAllWeaponSlotsCopy()).thenReturn(List.of(slot));
        when(ship.getHullSpec()).thenReturn(hullSpec);
        when(ship.getListeners(ModuleTreeShare.class)).thenReturn(List.of());
        return ship;
    }
}
