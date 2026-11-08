package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.persistence.ShipRecords;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.skilleffect.SkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModuleHullModTest {

    private static final String PREFIX = "exiledSector_moduleShare_parent_";

    private ShipEffectContext parentContext;

    @BeforeEach
    void registerParent() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getCustomData()).thenReturn(new HashMap<>());
        Global.setCombatEngine(engine);
        ShipVariantAPI parentVariant = ModuleVariants.parent(VariantSource.REFIT, Map.of("WS0001", ModuleVariants.variant(VariantSource.REFIT)));
        FleetMemberAPI parentMember = mock(FleetMemberAPI.class);
        when(parentMember.getId()).thenReturn("parent");
        parentContext = new ShipEffectContext(new ShipSkillData(), parentMember, parentVariant, HullSize.CRUISER);
        ModuleParents.register(parentContext, mock(ResolvedTree.class));
    }

    @AfterEach
    void leaveCombat() {
        Global.setCombatEngine(null);
    }

    private static MutableShipStatsAPI moduleStats(ShipVariantAPI moduleVariant) {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        FleetMemberAPI tempMember = mock(FleetMemberAPI.class);
        when(stats.getFleetMember()).thenReturn(tempMember);
        when(stats.getVariant()).thenReturn(moduleVariant);
        return stats;
    }

    private static ShipEffectSource source(Map<SkillEffect, Float> totals) {
        ShipEffectSource source = mock(ShipEffectSource.class);
        when(source.moduleContributions(any(), any(), any())).thenReturn(totals);
        return source;
    }

    private static SkillEffect effect(String name, boolean afterOtherEffects) {
        SkillEffect effect = mock(SkillEffect.class);
        when(effect.name()).thenReturn(name);
        when(effect.appliesAfterOtherEffects()).thenReturn(afterOtherEffects);
        return effect;
    }

    @Test
    void theTotalsLandInTheTwoPassesTheParentUsesUnderTheParentsModId() {
        MutableShipStatsAPI stats = moduleStats(ModuleVariants.linkedModule("parent"));
        SkillEffect readsOthers = effect("READS_OTHERS", true);
        SkillEffect plain = effect("PLAIN", false);
        ShipEffectSource source = source(Map.of(readsOthers, 5f, plain, 10f));

        ModuleHullMod.applyBeforeCreation(stats, List.of(source));

        InOrder passes = inOrder(plain, readsOthers);
        passes.verify(plain).apply(stats, PREFIX + "PLAIN", 10f);
        passes.verify(readsOthers).apply(stats, PREFIX + "READS_OTHERS", 5f);
        verify(source).applyToModule(parentContext, stats);
    }

    @Test
    void everySourcesTotalsAddUpPerEffect() {
        MutableShipStatsAPI stats = moduleStats(ModuleVariants.linkedModule("parent"));
        SkillEffect fluxCapacity = SkillEffect.byName("FLUX_CAPACITY_PERCENT");

        ModuleHullMod.applyBeforeCreation(stats, List.of(source(Map.of(fluxCapacity, 5f)), source(Map.of(fluxCapacity, 10f))));

        verify(stats.getFluxCapacity()).modifyPercent(PREFIX + "FLUX_CAPACITY_PERCENT", 15f);
    }

    @Test
    void afterCreationEachTotalAndEachSourceFinishesTheModule() {
        ShipVariantAPI moduleVariant = ModuleVariants.linkedModule("parent");
        ShipAPI module = mock(ShipAPI.class);
        MutableShipStatsAPI stats = moduleStats(moduleVariant);
        when(module.getMutableStats()).thenReturn(stats);
        when(module.getVariant()).thenReturn(moduleVariant);
        WeaponAPI smallGun = mock(WeaponAPI.class);
        when(smallGun.getSize()).thenReturn(WeaponAPI.WeaponSize.SMALL);
        when(smallGun.getType()).thenReturn(WeaponAPI.WeaponType.ENERGY);
        WeaponAPI largeGun = mock(WeaponAPI.class);
        when(largeGun.getSize()).thenReturn(WeaponAPI.WeaponSize.LARGE);
        when(module.getAllWeapons()).thenReturn(List.of(smallGun, largeGun));
        ShipEffectSource source = source(Map.of(SkillEffect.byName("PD_RECLASSIFY_SMALL_WEAPONS"), 0f));

        ModuleHullMod.applyAfterCreation(module, List.of(source));

        verify(smallGun).setPD(true);
        verify(largeGun, never()).setPD(true);
        verify(source).afterModuleCreation(parentContext, module);
    }

    @Test
    void afterCreationDamageReshapersAttachFirstAndFinalDamageReadersLast() {
        ShipVariantAPI moduleVariant = ModuleVariants.linkedModule("parent");
        ShipAPI module = mock(ShipAPI.class);
        MutableShipStatsAPI stats = moduleStats(moduleVariant);
        when(module.getMutableStats()).thenReturn(stats);
        when(module.getVariant()).thenReturn(moduleVariant);
        SkillEffect reader = effect("READER", false);
        when(reader.readsFinalDealtDamage()).thenReturn(true);
        SkillEffect plain = effect("PLAIN", false);
        SkillEffect reshaper = effect("RESHAPER", false);
        when(reshaper.reshapesDealtDamage()).thenReturn(true);
        Map<SkillEffect, Float> totals = new LinkedHashMap<>();
        totals.put(reader, 1f);
        totals.put(plain, 2f);
        totals.put(reshaper, 3f);

        ModuleHullMod.applyAfterCreation(module, List.of(source(totals)));

        InOrder passes = inOrder(reshaper, plain, reader);
        passes.verify(reshaper).applyAfterShipCreation(module, PREFIX + "RESHAPER", 3f);
        passes.verify(plain).applyAfterShipCreation(module, PREFIX + "PLAIN", 2f);
        passes.verify(reader).applyAfterShipCreation(module, PREFIX + "READER", 1f);
    }

    @Test
    void aDockedShipWithATreeOfItsOwnBorrowsNothing() {
        ShipVariantAPI dockedShip = ModuleVariants.linkedModule("parent");
        dockedShip.addTag(ShipRecords.SHIP_TAG_PREFIX + "docked");
        MutableShipStatsAPI stats = moduleStats(dockedShip);
        ShipEffectSource source = source(Map.of(SkillEffect.byName("FLUX_CAPACITY_PERCENT"), 10f));

        try (MockedStatic<ShipSkillDataManager> records = Mockito.mockStatic(ShipSkillDataManager.class)) {
            records.when(() -> ShipSkillDataManager.find("docked")).thenReturn(new ShipSkillData());
            ModuleHullMod.applyBeforeCreation(stats, List.of(source));
        }

        verify(stats.getFluxCapacity(), never()).modifyPercent(anyString(), anyFloat());
        verify(source, never()).applyToModule(any(), any());
    }

    @Test
    void aModuleWithNoParentAroundBuildsAtItsPlainStats() {
        MutableShipStatsAPI stats = moduleStats(ModuleVariants.linkedModule("gone"));
        ShipEffectSource source = source(Map.of(SkillEffect.byName("FLUX_CAPACITY_PERCENT"), 10f));

        ModuleHullMod.applyBeforeCreation(stats, List.of(source));

        verify(source, never()).moduleContributions(any(), any(), any());
        verify(stats.getFluxCapacity(), never()).modifyPercent(anyString(), anyFloat());
    }
}
