package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.MockSector;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.battle.BattleSkillEffect;
import exiledsector.skills.skilleffect.defense.DefenseSkillEffect;
import exiledsector.skills.skilleffect.flux.FluxSkillEffect;
import exiledsector.skills.skilleffect.logistics.LogisticsSkillEffect;
import exiledsector.skills.skilleffect.phase.PhaseSkillEffect;
import exiledsector.ExiledSector;
import exiledsector.persistence.ShipRecords;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.PhantomHullModStatus;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.progression.SkillNodeOpCost;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.magiclib.util.MagicIncompatibleHullmods;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillTreeHullModTest {

    private MockSector mockSector;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private Map<String, Object> persistentData;

    @BeforeEach
    void setUp() {
        SkillDataResolver.clearCache();
        mockSector = new MockSector();
        persistentData = mockSector.persistentData;

        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);

        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    @AfterEach
    void tearDown() {
        PhantomHullMods.clearForTests();
        PhantomConflictWatch.clearPendingReverts();
        mockSector.close();
        lunaSettingsMock.close();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    @Test
    void appliesTheEffectOfEachAllocatedNodeWithOneRegisteredOnTheTree() {
        SkillType hullType = new SkillType.Builder("hull", "Reinforced Hull", "graphics/hullmods/reinforced_bulkheads.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode hullNode = new SkillNode("hull_1", hullType, List.of(), 0f, 0f);
        SkillTree.register(hullNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), hullNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        com.fs.starfarer.api.combat.StatBonus hullStatBonus = mock(com.fs.starfarer.api.combat.StatBonus.class);
        when(stats.getHullBonus()).thenReturn(hullStatBonus);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");

        verify(hullStatBonus).modifyPercent("exiledSector_skill_hull_1_HULL_PERCENT", 10f);
    }

    private com.fs.starfarer.api.combat.StatBonus hullBonusAfterAllocating(float... hullMultMagnitudes) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        for (int i = 0; i < hullMultMagnitudes.length; i++) {
            SkillType type = new SkillType.Builder("hull_mult_" + i, "Hull", "a.png", SkillTier.SMALL)
                    .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_MULT, hullMultMagnitudes[i]))).build();
            SkillNode node = new SkillNode("hull_mult_node_" + i, type, List.of(), 0f, 0f);
            SkillTree.register(node);
            ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), node, 1);
        }
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        com.fs.starfarer.api.combat.StatBonus hullBonus = mock(com.fs.starfarer.api.combat.StatBonus.class);
        when(stats.getHullBonus()).thenReturn(hullBonus);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");
        return hullBonus;
    }

    @Test
    void moreMultipliersFromSeveralNodesAddIntoOneMultiplierInsteadOfCompounding() {
        com.fs.starfarer.api.combat.StatBonus hullBonus = hullBonusAfterAllocating(2f, 2f, 5f);

        verify(hullBonus).modifyMult("exiledSector_skillMult_HULL_MULT", 1.09f);
        verify(hullBonus, never()).modifyMult(org.mockito.ArgumentMatchers.startsWith("exiledSector_skill_"), anyFloat());
    }

    @Test
    void moreAndLessMultipliersOnOneEffectAddTogether() {
        com.fs.starfarer.api.combat.StatBonus hullBonus = hullBonusAfterAllocating(15f, -30f);

        verify(hullBonus).modifyMult("exiledSector_skillMult_HULL_MULT", 0.85f);
    }

    @Test
    void addedMultipliersNeverGoBelowOneHundredPercentLess() {
        com.fs.starfarer.api.combat.StatBonus hullBonus = hullBonusAfterAllocating(-75f, -50f);

        verify(hullBonus).modifyMult("exiledSector_skillMult_HULL_MULT", 0f);
    }

    @Test
    void appliesTheSelectedOptionsEffectForAnOptionalNodeNotThePlaceholders() {
        SkillType placeholder = new SkillType.Builder("slot", "Optional Skill", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .todo(null)
                .optionalOptionIds(List.of("hull"))
                .build();
        SkillType hullOption = new SkillType.Builder("hull", "Reinforced Hull", "graphics/hullmods/reinforced_bulkheads.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillTree.registerType(hullOption);
        SkillNode slotNode = new SkillNode("slot_1", placeholder, List.of(), 0f, 0f);
        SkillTree.register(slotNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.selectOption(ShipSkillDataManager.get("ship-a"), slotNode, hullOption, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        com.fs.starfarer.api.combat.StatBonus hullStatBonus = mock(com.fs.starfarer.api.combat.StatBonus.class);
        when(stats.getHullBonus()).thenReturn(hullStatBonus);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");

        verify(hullStatBonus).modifyPercent("exiledSector_skill_slot_1_HULL_PERCENT", 10f);
    }

    @Test
    void appliesEachEffectOnANodeWithMultipleEffectsIndependently() {
        SkillType multiType = new SkillType.Builder("heavyarmor", "Heavy Armor", "graphics/icons/notable_hullmods/heavy_armor.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, 15f), new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 5f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode multiNode = new SkillNode("heavyarmor_1", multiType, List.of(), 0f, 0f);
        SkillTree.register(multiNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), multiNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        com.fs.starfarer.api.combat.StatBonus armorBonus = mock(com.fs.starfarer.api.combat.StatBonus.class);
        com.fs.starfarer.api.combat.StatBonus hullBonus = mock(com.fs.starfarer.api.combat.StatBonus.class);
        when(stats.getArmorBonus()).thenReturn(armorBonus);
        when(stats.getHullBonus()).thenReturn(hullBonus);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");

        verify(armorBonus).modifyPercent("exiledSector_skill_heavyarmor_1_ARMOR_PERCENT", 15f);
        verify(hullBonus).modifyPercent("exiledSector_skill_heavyarmor_1_HULL_PERCENT", 5f);
    }

    @Test
    void theSameEffectTwiceOnOneNodeStacksInsteadOfOverwritingItself() {
        SkillType doubled = new SkillType.Builder("doubled", "Doubled", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f), new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 5f)))
                .build();
        SkillNode doubledNode = new SkillNode("doubled_1", doubled, List.of(), 0f, 0f);
        SkillTree.register(doubledNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), doubledNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        com.fs.starfarer.api.combat.StatBonus hullBonus = mock(com.fs.starfarer.api.combat.StatBonus.class);
        when(stats.getHullBonus()).thenReturn(hullBonus);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");

        verify(hullBonus).modifyPercent("exiledSector_skill_doubled_1_HULL_PERCENT", 10f);
        verify(hullBonus).modifyPercent("exiledSector_skill_doubled_1_HULL_PERCENT_2", 5f);
    }

    @Test
    void skipsNodesWithNoEffectDefinedYet() {
        SkillType cosmeticType = new SkillType.Builder("capacitors", "Capacitors", "graphics/hullmods/flux_coil_adjunct.png", SkillTier.SMALL)
                .effects(List.of())
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode cosmeticNode = new SkillNode("capacitors_1", cosmeticType, List.of(), 0f, 0f);
        SkillTree.register(cosmeticNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), cosmeticNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");

        verify(stats, never()).getHullBonus();
    }

    @Test
    void delegatesToTheRealVanillaHullModEffectWhenTypeSpecifiesOne() {
        SkillType keystoneType = new SkillType.Builder("safety_overrides", "Safety Overrides", "graphics/icons/skills/helmsmanship.png", SkillTier.KEYSTONE)
                .effects(List.of())
                .vanillaHullModId("safetyoverrides")
                .todo(null)
                .build();
        SkillNode keystoneNode = new SkillNode("safety_overrides_1", keystoneType, List.of(), 0f, 0f);
        SkillTree.register(keystoneNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), keystoneNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);

        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(settings.getHullModSpec("safetyoverrides")).thenReturn(spec);
        HullModEffect vanillaEffect = mock(HullModEffect.class);
        when(spec.getEffect()).thenReturn(vanillaEffect);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");

        verify(vanillaEffect).applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "safetyoverrides");
    }

    @Test
    void doesNothingWhenTheShipHasNoFleetMember() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(null);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");

        verify(stats, never()).getHullBonus();
    }

    @Test
    void beforeShipCreationRemovesARealHullModThatConflictsWithAnAllocatedSkill() {
        SkillType frontType = new SkillType.Builder("frontemitter", "Shield Conversion - Front", "a.png", SkillTier.NOTABLE)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .todo(null)
                .optionalOptionIds(List.of())
                .exclusiveHullModIds(List.of("adaptiveshields"))
                .build();
        SkillNode frontNode = new SkillNode("frontemitter_1", frontType, List.of(), 0f, 0f);
        SkillTree.register(frontNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), frontNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        RealVariantTags.attach(variant);
        when(stats.getVariant()).thenReturn(variant);
        when(variant.hasHullMod("adaptiveshields")).thenReturn(true);
        when(variant.getHullMods()).thenReturn(new LinkedHashSet<>(List.of("adaptiveshields")));
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>());

        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");

        verify(variant).removeMod("adaptiveshields");
        verify(variant).removeMod("ML_incompatibleHullmodWarning");
        verify(variant).addMod("exiledSector_conflictWarning");
        verify(variant, never()).removeMod(SkillTreeHullMod.ID);

        SkillConflictWarnings.Removal removal = SkillConflictWarnings.get(variant);
        assertEquals(new SkillConflictWarnings.Removal("adaptiveshields", "frontemitter"), removal);
    }

    @Test
    void beforeShipCreationNeverRemovesTheUmbrellaHullModEvenWhenTheConflictingHullModIsSModded() {
        SkillType hullType = new SkillType.Builder("hull", "Reinforced Hull", "graphics/hullmods/reinforced_bulkheads.png", SkillTier.SMALL)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .todo(null)
                .optionalOptionIds(List.of())
                .exclusiveHullModIds(List.of("armoredcladding"))
                .build();
        SkillNode hullNode = new SkillNode("hull_1", hullType, List.of(), 0f, 0f);
        SkillTree.register(hullNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), hullNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(stats.getVariant()).thenReturn(variant);
        when(variant.hasHullMod("armoredcladding")).thenReturn(true);
        when(variant.getHullMods()).thenReturn(new LinkedHashSet<>(List.of("armoredcladding")));
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>(List.of("armoredcladding")));

        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");

        verify(variant, never()).removeMod(SkillTreeHullMod.ID);
        verify(variant, never()).removeMod("armoredcladding");
        verify(variant, never()).addMod("exiledSector_conflictWarning");
        assertNull(SkillConflictWarnings.get(variant));
    }

    @Test
    void aPermaModThatConflictsIsNeverReportedAsRemoved() {
        SkillType hullType = new SkillType.Builder("hull", "Reinforced Hull", "graphics/hullmods/reinforced_bulkheads.png", SkillTier.SMALL)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .todo(null)
                .optionalOptionIds(List.of())
                .exclusiveHullModIds(List.of("armoredcladding"))
                .build();
        SkillNode hullNode = new SkillNode("hull_1", hullType, List.of(), 0f, 0f);
        SkillTree.register(hullNode);
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), hullNode, 1);

        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        RealVariantTags.attach(variant);
        when(variant.hasHullMod("armoredcladding")).thenReturn(true);
        when(variant.getHullMods()).thenReturn(new LinkedHashSet<>(List.of("armoredcladding")));
        when(variant.getPermaMods()).thenReturn(new LinkedHashSet<>(List.of("armoredcladding")));
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>());

        HullModConflictResolver.removeConflicts(memberWithId("ship-a"), variant);

        verify(variant, never()).addMod("exiledSector_conflictWarning");
        assertNull(SkillConflictWarnings.get(variant));
    }

    @Test
    void theWarningStaysAfterTheConflictIsResolvedUntilThePlayerRemovesIt() {
        SkillType frontType = new SkillType.Builder("frontemitter", "Shield Conversion - Front", "a.png", SkillTier.NOTABLE)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .todo(null)
                .optionalOptionIds(List.of())
                .exclusiveHullModIds(List.of("adaptiveshields"))
                .build();
        SkillNode frontNode = new SkillNode("frontemitter_1", frontType, List.of(), 0f, 0f);
        SkillTree.register(frontNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipSkillData data = ShipSkillDataManager.get("ship-a");
        ShipTrees.allocate(data, frontNode, 1);

        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        RealVariantTags.attach(variant);
        when(variant.hasHullMod("adaptiveshields")).thenReturn(true);
        when(variant.getHullMods()).thenReturn(new LinkedHashSet<>(List.of("adaptiveshields")));
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>());

        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);

        HullModConflictResolver.removeConflicts(member, variant);
        verify(variant).addMod("exiledSector_conflictWarning");

        ShipTrees.deallocate(data, frontNode);
        when(variant.hasHullMod("adaptiveshields")).thenReturn(false);
        when(variant.hasHullMod("exiledSector_conflictWarning")).thenReturn(true);

        HullModConflictResolver.removeConflicts(member, variant);

        verify(variant, never()).removeMod("exiledSector_conflictWarning");
        assertEquals("adaptiveshields", SkillConflictWarnings.get(variant).removedHullModId());

        when(variant.hasHullMod("exiledSector_conflictWarning")).thenReturn(false);

        HullModConflictResolver.removeConflicts(member, variant);

        assertNull(SkillConflictWarnings.get(variant));
    }

    @Test
    void aWarningWithNoRecordedRemovalIsTakenOff() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        RealVariantTags.attach(variant);
        when(variant.hasHullMod("exiledSector_conflictWarning")).thenReturn(true);

        HullModConflictResolver.removeConflicts(List.of(), variant);

        verify(variant).removeMod("exiledSector_conflictWarning");
    }

    private static SkillNode registerMilitarizedNode() {
        SkillType militarizedType = new SkillType.Builder("militarized_subsystems", "Militarized Subsystems", "a.png", SkillTier.NOTABLE)
                .phantomHullModIds(List.of("militarized_subsystems"))
                .build();
        PhantomHullModStatus.markActive("militarized_subsystems");
        SkillNode militarizedNode = new SkillNode("militarized_subsystems_1", militarizedType, List.of(), 0f, 0f);
        SkillTree.register(militarizedNode);
        return militarizedNode;
    }

    private static FleetMemberAPI memberWithId(String id) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        return member;
    }

    @Test
    void syncInstalledHullModsInstallsTheHullModAsATaggedPermaModWhileTheNodeIsAllocated() {
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), registerMilitarizedNode(), 1);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of());

        syncPhantoms(memberWithId("ship-a"), variant);

        verify(variant).addPermaMod("militarized_subsystems");
        verify(variant).addTag("exiledSector_installed_militarized_subsystems");
    }

    private static SkillNode registerPhantomSafetyOverridesNode() {
        SkillType type = new SkillType.Builder("safety_overrides", "Safety Overrides", "a.png", SkillTier.KEYSTONE)
                .phantomHullModIds(List.of("safetyoverrides"))
                .build();
        SkillNode node = new SkillNode("safety_overrides_1", type, List.of(), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    private void makeSafetyOverridesAPhantom() {
        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        when(settings.getScriptClassLoader()).thenReturn(SkillTreeHullModTest.class.getClassLoader());
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(settings.getHullModSpec("safetyoverrides")).thenReturn(spec);
        when(spec.getId()).thenReturn("safetyoverrides");
        when(spec.getEffectClass()).thenReturn(RecordingHullModEffect.class.getName());
        when(spec.getEffect()).thenAnswer(invocation -> {
            PhantomHullModEffect effect = new PhantomHullModEffect();
            effect.init(spec);
            return effect;
        });
        PhantomHullMods.install(List.of("safetyoverrides"));
    }

    @Test
    void aPhantomHullModIsPlacedAsATaggedPermaModOnlyOnceItsVanillaEffectIsWrapped() {
        PhantomHullMods.clearForTests();
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), registerPhantomSafetyOverridesNode(), 1);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of());

        syncPhantoms(memberWithId("ship-a"), variant);
        verify(variant, never()).addPermaMod("safetyoverrides");

        makeSafetyOverridesAPhantom();
        syncPhantoms(memberWithId("ship-a"), variant);

        verify(variant).addPermaMod("safetyoverrides");
        verify(variant).addTag("exiledSector_installed_safetyoverrides");
    }

    private static ShipVariantAPI variantWhereAModTriedToStripThePhantomFor(String causeId) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.hasHullMod("ML_incompatibleHullmodWarning")).thenReturn(true);
        when(variant.hasHullMod("safetyoverrides")).thenReturn(true);
        when(variant.hasHullMod(causeId)).thenReturn(true);
        when(variant.hasTag("exiledSector_installed_safetyoverrides")).thenReturn(true);
        when(variant.getHullMods()).thenReturn(new LinkedHashSet<>(List.of("safetyoverrides", causeId)));
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>());
        MagicIncompatibleHullmods.removeHullmodWithWarning(variant, "safetyoverrides", causeId);
        return variant;
    }

    @Test
    void aHullModThatTriesToStripAPhantomIsRemovedInsteadAndThePlayerIsWarned() {
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), registerPhantomSafetyOverridesNode(), 1);
        makeSafetyOverridesAPhantom();
        when(Global.getSettings().getHullModSpec("nskr_volatile")).thenReturn(mock(HullModSpecAPI.class));
        ShipVariantAPI variant = variantWhereAModTriedToStripThePhantomFor("nskr_volatile");
        RealVariantTags.attach(variant);

        HullModConflictResolver.removeConflicts(memberWithId("ship-a"), variant);

        verify(variant).removeMod("ML_incompatibleHullmodWarning");
        verify(variant).removeMod("nskr_volatile");
        verify(variant).addMod("exiledSector_conflictWarning");
        assertEquals("nskr_volatile", SkillConflictWarnings.get(variant).removedHullModId());
    }

    @Test
    void aPermanentHullModThatTriesToStripAPhantomIsNeverReportedAsRemoved() {
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), registerPhantomSafetyOverridesNode(), 1);
        makeSafetyOverridesAPhantom();
        when(Global.getSettings().getHullModSpec("nskr_volatile")).thenReturn(mock(HullModSpecAPI.class));
        ShipVariantAPI variant = variantWhereAModTriedToStripThePhantomFor("nskr_volatile");
        when(variant.getPermaMods()).thenReturn(new LinkedHashSet<>(List.of("safetyoverrides", "nskr_volatile")));

        HullModConflictResolver.removeConflicts(memberWithId("ship-a"), variant);

        verify(variant, never()).removeMod("nskr_volatile");
        verify(variant, never()).addMod("exiledSector_conflictWarning");
        assertNull(SkillConflictWarnings.get(variant));
    }

    @Test
    void aStripAttemptAgainstAHullModTheTreeDoesNotProvideIsLeftToMagicLib() {
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), registerMilitarizedNode(), 1);
        makeSafetyOverridesAPhantom();
        when(Global.getSettings().getHullModSpec("nskr_volatile")).thenReturn(mock(HullModSpecAPI.class));
        ShipVariantAPI variant = variantWhereAModTriedToStripThePhantomFor("nskr_volatile");

        HullModConflictResolver.removeConflicts(memberWithId("ship-a"), variant);

        verify(variant, never()).removeMod("ML_incompatibleHullmodWarning");
        verify(variant, never()).removeMod("nskr_volatile");
    }

    @Test
    void syncInstalledHullModsRemovesOnlyHullModsItInstalledOnceTheNodeIsNoLongerAllocated() {
        registerMilitarizedNode();
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of("exiledSector_installed_militarized_subsystems", "some_other_tag"));

        syncPhantoms(memberWithId("ship-a"), variant);

        verify(variant).removePermaMod("militarized_subsystems");
        verify(variant).removeTag("exiledSector_installed_militarized_subsystems");
        verify(variant, never()).removeTag("some_other_tag");
    }

    private static boolean syncPhantoms(FleetMemberAPI member, ShipVariantAPI variant) {
        List<AllocatedNode> allocatedNodes = AllocatedNode.of(SkillDataResolver.resolve(member, variant));
        return PhantomInstallSync.sync(SkillTreeEffectSource.phantomHullModIdsOf(allocatedNodes), variant);
    }

    private static ShipVariantAPI variantWithTreeInstalledMilitarizedSubsystems(boolean permanent) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.hasHullMod("militarized_subsystems")).thenReturn(true);
        when(variant.hasTag("exiledSector_installed_militarized_subsystems")).thenReturn(true);
        when(variant.getPermaMods()).thenReturn(new LinkedHashSet<>(permanent ? List.of("militarized_subsystems") : List.of()));
        when(variant.getTags()).thenReturn(List.of("exiledSector_installed_militarized_subsystems", "some_other_tag"));
        return variant;
    }

    @Test
    void syncMakesOnlyTheTreesOwnCopiesThatTheBuildInDialogMadeNormalPermanentAgain() {
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), registerMilitarizedNode(), 1);
        ShipVariantAPI demoted = variantWithTreeInstalledMilitarizedSubsystems(false);
        ShipVariantAPI intact = variantWithTreeInstalledMilitarizedSubsystems(true);

        assertTrue(syncPhantoms(memberWithId("ship-a"), demoted));
        assertFalse(syncPhantoms(memberWithId("ship-a"), intact));

        verify(demoted).addPermaMod("militarized_subsystems");
        verify(demoted, never()).addTag(anyString());
        verify(intact, never()).addPermaMod(anyString());
    }

    @Test
    void aCopyOfTheHullModThePlayerInstalledThemselvesIsNeitherTakenOverNorMadePermanent() {
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), registerMilitarizedNode(), 1);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.hasHullMod("militarized_subsystems")).thenReturn(true);
        when(variant.getPermaMods()).thenReturn(new LinkedHashSet<>());
        when(variant.getTags()).thenReturn(List.of());

        syncPhantoms(memberWithId("ship-a"), variant);

        verify(variant, never()).addPermaMod(anyString());
        verify(variant, never()).addTag(anyString());
    }

    @Test
    void anInstalledHullModThePlayerBuiltInAsAnSModStaysWhenTheNodeIsRemoved() {
        registerMilitarizedNode();
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of("exiledSector_installed_militarized_subsystems"));
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>(List.of("militarized_subsystems")));

        syncPhantoms(memberWithId("ship-a"), variant);

        verify(variant, never()).removePermaMod(anyString());
        verify(variant).removeTag("exiledSector_installed_militarized_subsystems");
    }

    @Test
    void removeHullModsConflictingWithAllocatedSkillsLeavesAHullModTheSkillTreeInstalledItself() {
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), registerMilitarizedNode(), 1);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.hasHullMod("militarized_subsystems")).thenReturn(true);
        when(variant.hasTag("exiledSector_installed_militarized_subsystems")).thenReturn(true);

        HullModConflictResolver.removeConflicts(memberWithId("ship-a"), variant);

        verify(variant, never()).removeMod("militarized_subsystems");
        verify(variant, never()).addMod("exiledSector_conflictWarning");
    }

    @Test
    void removeHullModsConflictingWithAllocatedSkillsNeverStripsABuiltInHullMod() {
        SkillType targetingType = new SkillType.Builder("targetingunit", "Integrated Targeting Unit", "a.png", SkillTier.NOTABLE)
                .exclusiveHullModIds(List.of("advancedcore"))
                .build();
        SkillNode targetingNode = new SkillNode("targetingunit_1", targetingType, List.of(), 0f, 0f);
        SkillTree.register(targetingNode);
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), targetingNode, 1);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(variant.getHullSpec()).thenReturn(hullSpec);
        when(hullSpec.isBuiltInMod("advancedcore")).thenReturn(true);
        when(variant.hasHullMod("advancedcore")).thenReturn(true);

        HullModConflictResolver.removeConflicts(memberWithId("ship-a"), variant);

        verify(variant, never()).removeMod("advancedcore");
        verify(variant, never()).addMod("exiledSector_conflictWarning");
    }

    @Test
    void removeHullModsConflictingWithAllocatedSkillsAlsoStripsWhatAnOptionalNodesContainerExcludes() {
        SkillType plain = new SkillType.Builder("plain", "Plain", "a.png", SkillTier.SMALL).build();
        SkillTree.registerType(plain);
        SkillType slotType = new SkillType.Builder("slot", "Slot", "a.png", SkillTier.SMALL)
                .exclusiveHullModIds(List.of("heavyarmor"))
                .optionalOptionIds(List.of("plain"))
                .build();
        SkillNode slotNode = new SkillNode("slot_1", slotType, List.of(), 0f, 0f);
        SkillTree.register(slotNode);
        ShipTrees.selectOption(ShipSkillDataManager.get("ship-a"), slotNode, plain, 1);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.hasHullMod("heavyarmor")).thenReturn(true);
        when(variant.getHullMods()).thenReturn(new LinkedHashSet<>(List.of("heavyarmor")));
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>());
        mockSector.global.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));

        HullModConflictResolver.removeConflicts(memberWithId("ship-a"), variant);

        verify(variant).removeMod("heavyarmor");
        verify(variant).addMod("exiledSector_conflictWarning");
    }

    @Test
    void removeHullModsConflictingWithAllocatedSkillsDoesNothingWhenNoConflictWasEverPresent() {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");

        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.hasHullMod("exiledSector_conflictWarning")).thenReturn(false);

        HullModConflictResolver.removeConflicts(member, variant);

        verify(variant, never()).removeMod("exiledSector_conflictWarning");
    }

    @Test
    void beforeShipCreationDoesNotRemoveAnythingWhenNoConflictingHullModIsInstalled() {
        SkillType frontType = new SkillType.Builder("frontemitter", "Shield Conversion - Front", "a.png", SkillTier.NOTABLE)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .todo(null)
                .optionalOptionIds(List.of())
                .exclusiveHullModIds(List.of("adaptiveshields"))
                .build();
        SkillNode frontNode = new SkillNode("frontemitter_1", frontType, List.of(), 0f, 0f);
        SkillTree.register(frontNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), frontNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(stats.getVariant()).thenReturn(variant);
        when(variant.hasHullMod("adaptiveshields")).thenReturn(false);
        when(variant.hasTag(ShipRecords.SHIP_TAG_PREFIX + "ship-a")).thenReturn(true);

        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");

        verify(variant, never()).removeMod(anyString());
    }

    @Test
    void beforeShipCreationReservesThePaidNodesAtTheCurrentPerNodeCost() {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getHullSize()).thenReturn(HullSize.FRIGATE);
        when(member.getHullSpec()).thenReturn(hull);
        lunaSettingsMock.when(() -> LunaSettings.getInt(ExiledSector.MOD_ID, SkillNodeOpCost.FRIGATE_FIELD_ID)).thenReturn(2);
        SkillType type = new SkillType.Builder("t", "t", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode node = new SkillNode("armor_1", type, List.of(), 0f, 0f);
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), node, 4);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(stats.getVariant()).thenReturn(variant);

        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        HullModSpecAPI reserveSpec = mock(HullModSpecAPI.class);
        when(settings.getHullModSpec("exiledSector_opReserve_2")).thenReturn(reserveSpec);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");

        verify(variant).addMod("exiledSector_opReserve_2");
        verify(reserveSpec, never()).setFrigateCost(anyInt());
    }

    @Test
    void beforeShipCreationRemovesEveryReserveWhenNoOpHasBeenSpentOnAllocatedNodes() {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipSkillDataManager.get("ship-a");

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(stats.getVariant()).thenReturn(variant);
        when(variant.getHullMods()).thenReturn(List.of("exiledSector_opReserve_5", "exiledSector_opSpent_0"));

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");

        verify(variant).removeMod("exiledSector_opReserve_5");
        verify(variant).removeMod("exiledSector_opSpent_0");
        verify(variant, never()).addMod(startsWith("exiledSector_opReserve_"));
    }

    @Test
    void aShipWithoutARecordGetsNoReserveAndNoSkillRecord() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        FleetMemberAPI copy = memberWithId("temporary-copy");
        when(stats.getFleetMember()).thenReturn(copy);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(stats.getVariant()).thenReturn(variant);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, SkillTreeHullMod.ID);

        assertNull(ShipSkillDataManager.find("temporary-copy"));
        verify(variant, never()).addMod(anyString());
    }

    @Test
    void aCopyWithoutARecordKeepsTheReservesOfTheShipWhoseVariantItSharesButDropsRetiredSlots() {
        ShipVariantAPI sharedVariant = mock(ShipVariantAPI.class);
        when(sharedVariant.getHullMods()).thenReturn(List.of("exiledSector_opReserve_4", "exiledSector_opSpent_4"));

        OpReserveHullMods.sync(memberWithId("temporary-copy"), sharedVariant);

        verify(sharedVariant).removeMod("exiledSector_opSpent_4");
        verify(sharedVariant, never()).removeMod("exiledSector_opReserve_4");
        verify(sharedVariant, never()).addMod(anyString());
    }

    @Test
    void aShipWithARecordButNoPaidNodesDropsEveryReserveAndNothingElse() {
        ShipSkillDataManager.get("reset-ship");
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullMods()).thenReturn(List.of("exiledSector_opReserve_4", "exiledSector_opSpent_9", "hardenedshieldemitter"));

        OpReserveHullMods.sync(memberWithId("reset-ship"), variant);

        verify(variant).removeMod("exiledSector_opReserve_4");
        verify(variant).removeMod("exiledSector_opSpent_9");
        verify(variant, never()).removeMod("hardenedshieldemitter");
    }

    @Test
    void stalePaidReservesAreSwappedForTheCoinsMatchingThisShipsSpentOp() {
        FleetMemberAPI member = memberWithId("ship-a");
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getHullSize()).thenReturn(HullSize.FRIGATE);
        when(member.getHullSpec()).thenReturn(hull);
        SkillType type = new SkillType.Builder("t", "t", "a.png", SkillTier.SMALL).effects(List.of()).build();
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), new SkillNode("armor_1", type, List.of(), 0f, 0f), 1);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullMods()).thenReturn(List.of("hardenedshieldemitter", "exiledSector_opSpent_7", "exiledSector_opReserve_30"));
        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        when(settings.getHullModSpec("exiledSector_opReserve_1")).thenReturn(mock(HullModSpecAPI.class));

        OpReserveHullMods.sync(member, variant);

        verify(variant).removeMod("exiledSector_opSpent_7");
        verify(variant).removeMod("exiledSector_opReserve_30");
        verify(variant, never()).removeMod("hardenedshieldemitter");
        verify(variant).addMod("exiledSector_opReserve_1");
    }

    @Test
    void refundingTheLastPaidNodeClearsTheReservesFromTheShipAndTheRefitWorkingCopyAlike() {
        ShipSkillDataManager.get("ship-a");
        ShipVariantAPI memberVariant = mock(ShipVariantAPI.class);
        ShipVariantAPI refitWorkingCopy = mock(ShipVariantAPI.class);
        when(memberVariant.getHullMods()).thenReturn(List.of("exiledSector_opReserve_3"));
        when(refitWorkingCopy.getHullMods()).thenReturn(List.of("exiledSector_opReserve_3"));
        FleetMemberAPI member = memberWithId("ship-a");

        OpReserveHullMods.sync(member, memberVariant);
        OpReserveHullMods.sync(member, refitWorkingCopy);

        verify(memberVariant).removeMod("exiledSector_opReserve_3");
        verify(refitWorkingCopy).removeMod("exiledSector_opReserve_3");
    }

    @Test
    void delegatesToTheRealVanillaHullModEffectAfterShipCreationWhenTypeSpecifiesOne() {
        SkillType keystoneType = new SkillType.Builder("frontshield", "Makeshift Shield Generator", "graphics/icons/skills/front_shield_generator.png", SkillTier.KEYSTONE)
                .effects(List.of())
                .vanillaHullModId("frontshield")
                .todo(null)
                .build();
        SkillNode keystoneNode = new SkillNode("frontshield_1", keystoneType, List.of(), 0f, 0f);
        SkillTree.register(keystoneNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), keystoneNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = mockShip(member, stats);

        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(settings.getHullModSpec("frontshield")).thenReturn(spec);
        HullModEffect vanillaEffect = mock(HullModEffect.class);
        when(spec.getEffect()).thenReturn(vanillaEffect);

        new SkillTreeHullMod().applyEffectsAfterShipCreation(ship, "exiledSector_core");

        verify(vanillaEffect).applyEffectsAfterShipCreation(ship, "frontshield");
    }

    private HullModEffect allocateVanillaHullModNode(String hullModId) {
        SkillType keystoneType = new SkillType.Builder(hullModId, hullModId, "graphics/icons/" + hullModId + ".png", SkillTier.KEYSTONE)
                .effects(List.of())
                .vanillaHullModId(hullModId)
                .build();
        SkillNode keystoneNode = new SkillNode(hullModId + "_1", keystoneType, List.of(), 0f, 0f);
        SkillTree.register(keystoneNode);
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), keystoneNode, 1);

        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(settings.getHullModSpec(hullModId)).thenReturn(spec);
        HullModEffect vanillaEffect = mock(HullModEffect.class);
        when(spec.getEffect()).thenReturn(vanillaEffect);
        return vanillaEffect;
    }

    @Test
    void advanceInCombatDelegatesToTheRealVanillaHullModEffect() {
        HullModEffect vanillaEffect = allocateVanillaHullModNode("missile_autoloader");
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipAPI ship = mockShip(member, mock(MutableShipStatsAPI.class));

        new SkillTreeHullMod().advanceInCombat(ship, 0.1f);

        verify(vanillaEffect).advanceInCombat(ship, 0.1f);
    }

    @Test
    void afterShipCreationSkipsNodesWithoutAVanillaHullMod() {
        SkillType hullType = new SkillType.Builder("hull", "Reinforced Hull", "graphics/hullmods/reinforced_bulkheads.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode hullNode = new SkillNode("hull_1", hullType, List.of(), 0f, 0f);
        SkillTree.register(hullNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), hullNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = mockShip(member, stats);

        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);

        new SkillTreeHullMod().applyEffectsAfterShipCreation(ship, "exiledSector_core");

        verify(settings, never()).getHullModSpec(anyString());
    }

    @Test
    void fighterSpawnDelegatesToTheRealVanillaHullModEffectWhenTypeSpecifiesOne() {
        SkillType passthroughType = new SkillType.Builder("defensive_targeting_array", "Defensive Targeting Array", "graphics/icons/skills/defensive_targeting_array.png", SkillTier.NOTABLE)
                .effects(List.of())
                .vanillaHullModId("defensive_targeting_array")
                .todo(null)
                .build();
        SkillNode passthroughNode = new SkillNode("defensive_targeting_array_1", passthroughType, List.of(), 0f, 0f);
        SkillTree.register(passthroughNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), passthroughNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = mockShip(member, stats);
        ShipAPI fighter = mock(ShipAPI.class);

        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(settings.getHullModSpec("defensive_targeting_array")).thenReturn(spec);
        HullModEffect vanillaEffect = mock(HullModEffect.class);
        when(spec.getEffect()).thenReturn(vanillaEffect);

        new SkillTreeHullMod().applyEffectsToFighterSpawnedByShip(fighter, ship, "exiledSector_core");

        verify(vanillaEffect).applyEffectsToFighterSpawnedByShip(fighter, ship, "defensive_targeting_array");
    }

    @Test
    void fighterSpawnCallsApplyToFighterSpawnedByShipOnNonPassthroughEffects() {
        exiledsector.skills.skilleffect.SkillEffect fighterEffect = mock(exiledsector.skills.skilleffect.SkillEffect.class);
        when(fighterEffect.name()).thenReturn("TEST_EFFECT");
        SkillType fighterType = new SkillType.Builder("fighter_weapon_damage", "Fighter Weapon Damage", "graphics/hullmods/fighter_uplink2.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(fighterEffect, 15f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode fighterNode = new SkillNode("fighter_weapon_damage_1", fighterType, List.of(), 0f, 0f);
        SkillTree.register(fighterNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), fighterNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = mockShip(member, stats);
        ShipAPI fighter = mock(ShipAPI.class);

        new SkillTreeHullMod().applyEffectsToFighterSpawnedByShip(fighter, ship, "exiledSector_core");

        verify(fighterEffect).applyToFighterSpawnedByShip(fighter, ship, "exiledSector_skill_fighter_weapon_damage_1_TEST_EFFECT", 15f);
    }

    @Test
    void laterFighterLaunchesReuseTheTreeResolvedForTheCarriersCombatPlan() {
        exiledsector.skills.skilleffect.SkillEffect fighterEffect = mock(exiledsector.skills.skilleffect.SkillEffect.class);
        when(fighterEffect.name()).thenReturn("TEST_EFFECT");
        SkillType fighterType = new SkillType.Builder("fighter_weapon_damage", "Fighter Weapon Damage", "graphics/hullmods/fighter_uplink2.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(fighterEffect, 15f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode fighterNode = new SkillNode("fighter_weapon_damage_1", fighterType, List.of(), 0f, 0f);
        SkillTree.register(fighterNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), fighterNode, 1);

        ShipAPI ship = withRealCustomData(mockShip(member, mock(MutableShipStatsAPI.class)));
        ShipAPI firstFighter = mock(ShipAPI.class);
        ShipAPI secondFighter = mock(ShipAPI.class);

        new SkillTreeHullMod().applyEffectsToFighterSpawnedByShip(firstFighter, ship, "exiledSector_core");
        ShipCombatPlan plan = (ShipCombatPlan) ship.getCustomData().values().stream().filter(ShipCombatPlan.class::isInstance).findFirst().orElseThrow();
        new SkillTreeHullMod().applyEffectsToFighterSpawnedByShip(secondFighter, ship, "exiledSector_core");

        assertTrue(plan.resolvedTree() != null);
        assertTrue(ship.getCustomData().containsValue(plan));
        verify(fighterEffect).applyToFighterSpawnedByShip(secondFighter, ship, "exiledSector_skill_fighter_weapon_damage_1_TEST_EFFECT", 15f);
    }

    @Test
    void fighterSpawnDoesNothingWhenTheShipHasNoFleetMember() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(null);
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        ShipAPI fighter = mock(ShipAPI.class);

        new SkillTreeHullMod().applyEffectsToFighterSpawnedByShip(fighter, ship, "exiledSector_core");

        mockSector.global.verify(Global::getSettings, never());
    }

    @Test
    void afterShipCreationCallsApplyAfterShipCreationOnNonPassthroughEffects() {
        exiledsector.skills.skilleffect.SkillEffect listenerEffect = mock(exiledsector.skills.skilleffect.SkillEffect.class);
        when(listenerEffect.name()).thenReturn("TEST_EFFECT");
        SkillType listenerType = new SkillType.Builder("high_scatter_amp", "High Scatter Amplifier", "graphics/hullmods/high_scatter_amp.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(listenerEffect, 50f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode listenerNode = new SkillNode("high_scatter_amp_1", listenerType, List.of(), 0f, 0f);
        SkillTree.register(listenerNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), listenerNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = mockShip(member, stats);

        new SkillTreeHullMod().applyEffectsAfterShipCreation(ship, "exiledSector_core");

        verify(listenerEffect).applyAfterShipCreation(ship, "exiledSector_skill_high_scatter_amp_1_TEST_EFFECT", 50f);
    }

    @Test
    void damageReshapingEffectsAreAppliedBeforeEveryOtherEffectWhateverTheAllocationOrder() {
        exiledsector.skills.skilleffect.SkillEffect reader = mock(exiledsector.skills.skilleffect.SkillEffect.class);
        when(reader.name()).thenReturn("READER");
        exiledsector.skills.skilleffect.SkillEffect reshaper = mock(exiledsector.skills.skilleffect.SkillEffect.class);
        when(reshaper.name()).thenReturn("RESHAPER");
        when(reshaper.reshapesDealtDamage()).thenReturn(true);
        SkillNode readerNode = new SkillNode("reader_1", new SkillType.Builder("reader", "Reader", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(reader, 1f))).build(), List.of(), 0f, 0f);
        SkillNode reshaperNode = new SkillNode("reshaper_1", new SkillType.Builder("reshaper", "Reshaper", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(reshaper, 1f))).build(), List.of(), 0f, 0f);
        SkillTree.register(readerNode);
        SkillTree.register(reshaperNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), readerNode, 1);
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), reshaperNode, 1);
        ShipAPI ship = mockShip(member, mock(MutableShipStatsAPI.class));

        new SkillTreeHullMod().applyEffectsAfterShipCreation(ship, "exiledSector_core");

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(reshaper, reader);
        order.verify(reshaper).applyAfterShipCreation(ship, "exiledSector_skill_reshaper_1_RESHAPER", 1f);
        order.verify(reader).applyAfterShipCreation(ship, "exiledSector_skill_reader_1_READER", 1f);
    }

    @Test
    void afterShipCreationDoesNothingWhenTheShipHasNoFleetMember() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(null);
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);

        new SkillTreeHullMod().applyEffectsAfterShipCreation(ship, "exiledSector_core");

        mockSector.global.verify(Global::getSettings, never());
    }

    private ShipAPI mockShip(FleetMemberAPI member, MutableShipStatsAPI stats) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(ship.getHullSize()).thenReturn(HullSize.FRIGATE);
        when(stats.getFleetMember()).thenReturn(member);
        return ship;
    }

    private static ShipAPI withRealCustomData(ShipAPI ship) {
        Map<String, Object> customData = new HashMap<>();
        when(ship.getCustomData()).thenReturn(customData);
        doAnswer(invocation -> customData.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(ship).setCustomData(anyString(), any());
        return ship;
    }

    private static SkillNode registerTemporaryHullNode() {
        SkillType temporaryType = new SkillType.Builder("surge", "Surge", "graphics/icons/surge.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .temporaryAfterDeploymentSeconds(60f)
                .build();
        SkillNode node = new SkillNode("surge_1", temporaryType, List.of(), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    @Test
    void aTemporaryNodeIsLeftAloneWhileItsWindowIsOpen() {
        SkillNode node = registerTemporaryHullNode();
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), node, 1);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus hull = mock(StatBonus.class);
        when(stats.getHullBonus()).thenReturn(hull);
        ShipAPI ship = withRealCustomData(mockShip(member, stats));
        when(ship.getFullTimeDeployed()).thenReturn(30f);

        new SkillTreeHullMod().advanceInCombat(ship, 0.1f);

        verify(hull, never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void aTemporaryNodeIsZeroedOnceWhenItsWindowCloses() {
        SkillNode node = registerTemporaryHullNode();
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), node, 1);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus hull = mock(StatBonus.class);
        when(stats.getHullBonus()).thenReturn(hull);
        ShipAPI ship = withRealCustomData(mockShip(member, stats));
        when(ship.getFullTimeDeployed()).thenReturn(59f, 61f, 62f);
        SkillTreeHullMod hullMod = new SkillTreeHullMod();

        hullMod.advanceInCombat(ship, 0.1f);
        hullMod.advanceInCombat(ship, 0.1f);
        hullMod.advanceInCombat(ship, 0.1f);

        verify(hull, times(1)).modifyPercent("exiledSector_skill_surge_1_HULL_PERCENT", 0f);
        verify(hull, never()).modifyPercent("exiledSector_skill_surge_1_HULL_PERCENT", 10f);
    }

    @Test
    void advanceInCombatResolvesTheShipsTreeOnlyOnce() {
        SkillNode node = registerTemporaryHullNode();
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), node, 1);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = withRealCustomData(mockShip(member, stats));
        SkillTreeHullMod hullMod = new SkillTreeHullMod();

        hullMod.advanceInCombat(ship, 0.1f);
        hullMod.advanceInCombat(ship, 0.1f);
        hullMod.advanceInCombat(ship, 0.1f);

        verify(ship, times(1)).getVariant();
    }

    @Test
    void advanceInCombatAppliesConditionalEffectWhileVenting() {
        SkillType ventType = new SkillType.Builder("fluxbreakers", "Resistant Flux Conduits", "graphics/icons/notable_hullmods/resistant_flux_conduits.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(FluxSkillEffect.FLUX_DISSIPATION_WHILE_VENTING_PERCENT, 25f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode ventNode = new SkillNode("fluxbreakers_1", ventType, List.of(), 0f, 0f);
        SkillTree.register(ventNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), ventNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = mockShip(member, stats);
        FluxTrackerAPI fluxTracker = mock(FluxTrackerAPI.class);
        when(ship.getFluxTracker()).thenReturn(fluxTracker);
        when(fluxTracker.isVenting()).thenReturn(true);
        MutableStat dissipation = mock(MutableStat.class);
        when(stats.getFluxDissipation()).thenReturn(dissipation);

        new SkillTreeHullMod().advanceInCombat(ship, 0.1f);

        verify(dissipation).modifyPercent("exiledSector_skill_fluxbreakers_1_FLUX_DISSIPATION_WHILE_VENTING_PERCENT", 25f);
        verify(dissipation, never()).unmodify(anyString());
    }

    @Test
    void advanceInCombatRemovesConditionalEffectWhenNotVenting() {
        SkillType ventType = new SkillType.Builder("fluxbreakers", "Resistant Flux Conduits", "graphics/icons/notable_hullmods/resistant_flux_conduits.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(FluxSkillEffect.FLUX_DISSIPATION_WHILE_VENTING_PERCENT, 25f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode ventNode = new SkillNode("fluxbreakers_1", ventType, List.of(), 0f, 0f);
        SkillTree.register(ventNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), ventNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = mockShip(member, stats);
        FluxTrackerAPI fluxTracker = mock(FluxTrackerAPI.class);
        when(ship.getFluxTracker()).thenReturn(fluxTracker);
        when(fluxTracker.isVenting()).thenReturn(false);
        MutableStat dissipation = mock(MutableStat.class);
        when(stats.getFluxDissipation()).thenReturn(dissipation);

        new SkillTreeHullMod().advanceInCombat(ship, 0.1f);

        verify(dissipation).modifyPercent("exiledSector_skill_fluxbreakers_1_FLUX_DISSIPATION_WHILE_VENTING_PERCENT", 0f);
    }

    @Test
    void advanceInCombatDoesNotTouchNonConditionalEffects() {
        SkillType hullType = new SkillType.Builder("hull", "Reinforced Hull", "graphics/hullmods/reinforced_bulkheads.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode hullNode = new SkillNode("hull_1", hullType, List.of(), 0f, 0f);
        SkillTree.register(hullNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), hullNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = mockShip(member, stats);

        new SkillTreeHullMod().advanceInCombat(ship, 0.1f);

        verify(ship, never()).getFluxTracker();
        verify(stats, never()).getHullBonus();
    }

    @Test
    void advanceInCombatDoesNothingWhenTheShipHasNoFleetMember() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(null);
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);

        new SkillTreeHullMod().advanceInCombat(ship, 0.1f);

        verify(ship, never()).getFluxTracker();
    }

    @Test
    void advanceInCombatDoublesStatsWhilePhased() {
        SkillType phaseType = new SkillType.Builder("phase_anchor", "Phase Anchor", "graphics/icons/notable_hullmods/phase_anchor.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(PhaseSkillEffect.COMBAT_BOOST_WHILE_PHASED, 100f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode phaseNode = new SkillNode("phase_anchor_1", phaseType, List.of(), 0f, 0f);
        SkillTree.register(phaseNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), phaseNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = mockShip(member, stats);
        when(ship.isPhased()).thenReturn(true);
        when(ship.getPhaseCloak()).thenReturn(null);
        MutableStat dissipation = mock(MutableStat.class);
        when(stats.getFluxDissipation()).thenReturn(dissipation);
        MutableStat ballisticRoF = mock(MutableStat.class);
        MutableStat energyRoF = mock(MutableStat.class);
        MutableStat missileRoF = mock(MutableStat.class);
        MutableStat ballisticAmmoRegen = mock(MutableStat.class);
        MutableStat energyAmmoRegen = mock(MutableStat.class);
        MutableStat missileAmmoRegen = mock(MutableStat.class);
        when(stats.getBallisticRoFMult()).thenReturn(ballisticRoF);
        when(stats.getEnergyRoFMult()).thenReturn(energyRoF);
        when(stats.getMissileRoFMult()).thenReturn(missileRoF);
        when(stats.getBallisticAmmoRegenMult()).thenReturn(ballisticAmmoRegen);
        when(stats.getEnergyAmmoRegenMult()).thenReturn(energyAmmoRegen);
        when(stats.getMissileAmmoRegenMult()).thenReturn(missileAmmoRegen);

        new SkillTreeHullMod().advanceInCombat(ship, 0.1f);

        verify(dissipation).modifyMult("exiledSector_skillMult_COMBAT_BOOST_WHILE_PHASED", 2f);
        verify(dissipation, never()).unmodifyMult(anyString());
    }

    @Test
    void advanceInCombatUndoesTheBoostWhenNotPhased() {
        SkillType phaseType = new SkillType.Builder("phase_anchor", "Phase Anchor", "graphics/icons/notable_hullmods/phase_anchor.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(PhaseSkillEffect.COMBAT_BOOST_WHILE_PHASED, 100f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode phaseNode = new SkillNode("phase_anchor_1", phaseType, List.of(), 0f, 0f);
        SkillTree.register(phaseNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), phaseNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = mockShip(member, stats);
        when(ship.isPhased()).thenReturn(false);
        MutableStat dissipation = mock(MutableStat.class);
        when(stats.getFluxDissipation()).thenReturn(dissipation);
        MutableStat ballisticRoF = mock(MutableStat.class);
        MutableStat energyRoF = mock(MutableStat.class);
        MutableStat missileRoF = mock(MutableStat.class);
        MutableStat ballisticAmmoRegen = mock(MutableStat.class);
        MutableStat energyAmmoRegen = mock(MutableStat.class);
        MutableStat missileAmmoRegen = mock(MutableStat.class);
        when(stats.getBallisticRoFMult()).thenReturn(ballisticRoF);
        when(stats.getEnergyRoFMult()).thenReturn(energyRoF);
        when(stats.getMissileRoFMult()).thenReturn(missileRoF);
        when(stats.getBallisticAmmoRegenMult()).thenReturn(ballisticAmmoRegen);
        when(stats.getEnergyAmmoRegenMult()).thenReturn(energyAmmoRegen);
        when(stats.getMissileAmmoRegenMult()).thenReturn(missileAmmoRegen);

        new SkillTreeHullMod().advanceInCombat(ship, 0.1f);

        verify(dissipation).modifyMult("exiledSector_skillMult_COMBAT_BOOST_WHILE_PHASED", 1f);
        verify(missileAmmoRegen).modifyMult("exiledSector_skillMult_COMBAT_BOOST_WHILE_PHASED", 1f);
    }

    @Test
    void advanceInCombatGrantsCommandPointRecoveryWhenFlagship() {
        SkillType opsType = new SkillType.Builder("operations_center", "Operations Center", "graphics/icons/notable_hullmods/operations_center.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(BattleSkillEffect.COMMAND_POINT_RECOVERY_WHILE_FLAGSHIP, 250f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode opsNode = new SkillNode("operations_center_1", opsType, List.of(), 0f, 0f);
        SkillTree.register(opsNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), opsNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = mockShip(member, stats);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        mockSector.global.when(Global::getCombatEngine).thenReturn(engine);
        when(engine.getPlayerShip()).thenReturn(ship);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        StatBonus commandPointRate = mock(StatBonus.class);
        when(dynamic.getMod("command_point_rate_flat")).thenReturn(commandPointRate);

        new SkillTreeHullMod().advanceInCombat(ship, 0.1f);

        verify(commandPointRate).modifyFlat("exiledSector_skill_operations_center_1_COMMAND_POINT_RECOVERY_WHILE_FLAGSHIP", 2.5f);
        verify(commandPointRate, never()).unmodify(anyString());
    }

    @Test
    void advanceInCombatWithholdsCommandPointRecoveryWhenNotFlagship() {
        SkillType opsType = new SkillType.Builder("operations_center", "Operations Center", "graphics/icons/notable_hullmods/operations_center.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(BattleSkillEffect.COMMAND_POINT_RECOVERY_WHILE_FLAGSHIP, 250f)))
                .vanillaHullModId(null)
                .todo(null)
                .build();
        SkillNode opsNode = new SkillNode("operations_center_1", opsType, List.of(), 0f, 0f);
        SkillTree.register(opsNode);

        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), opsNode, 1);

        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = mockShip(member, stats);
        ShipAPI otherShip = mock(ShipAPI.class);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        mockSector.global.when(Global::getCombatEngine).thenReturn(engine);
        when(engine.getPlayerShip()).thenReturn(otherShip);
        PersonAPI captain = mock(PersonAPI.class);
        when(ship.getCaptain()).thenReturn(captain);
        when(member.getFleetCommander()).thenReturn(null);
        when(member.getFleetCommanderForStats()).thenReturn(null);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        StatBonus commandPointRate = mock(StatBonus.class);
        when(dynamic.getMod("command_point_rate_flat")).thenReturn(commandPointRate);

        new SkillTreeHullMod().advanceInCombat(ship, 0.1f);

        verify(commandPointRate).modifyFlat("exiledSector_skill_operations_center_1_COMMAND_POINT_RECOVERY_WHILE_FLAGSHIP", 0f);
    }

    private static MutableShipStatsAPI statsWithHullAndRecovery(String memberId, StatBonus hull, StatBonus recovery) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(memberId);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        when(stats.getHullBonus()).thenReturn(hull);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD)).thenReturn(recovery);
        return stats;
    }

    @Test
    void anNpcBuildSkipsTheRecoveryBonusButKeepsTheNodesOtherEffects() {
        SkillType bulkheads = new SkillType.Builder("reinforcedhull", "Reinforced Bulkheads", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 40f),
                        new SkillTypeEffect(DefenseSkillEffect.SHIP_RECOVERY_CHANCE_BONUS, 1000f)))
                .build();
        SkillNode bulkheadsNode = new SkillNode("reinforcedhull_1", bulkheads, List.of(), 0f, 0f);
        SkillTree.register(bulkheadsNode);
        ShipSkillDataManager.get("npc-ship").markNpcBuild();
        ShipTrees.allocate(ShipSkillDataManager.get("npc-ship"), bulkheadsNode, 1);
        ShipTrees.allocate(ShipSkillDataManager.get("player-ship"), bulkheadsNode, 1);
        StatBonus npcHull = mock(StatBonus.class);
        StatBonus npcRecovery = mock(StatBonus.class);
        StatBonus playerHull = mock(StatBonus.class);
        StatBonus playerRecovery = mock(StatBonus.class);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.CRUISER,
                statsWithHullAndRecovery("npc-ship", npcHull, npcRecovery), SkillTreeHullMod.ID);
        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.CRUISER,
                statsWithHullAndRecovery("player-ship", playerHull, playerRecovery), SkillTreeHullMod.ID);

        verify(npcHull).modifyPercent("exiledSector_skill_reinforcedhull_1_HULL_PERCENT", 40f);
        verify(npcRecovery, never()).modifyFlat(anyString(), anyFloat());
        verify(playerHull).modifyPercent("exiledSector_skill_reinforcedhull_1_HULL_PERCENT", 40f);
        verify(playerRecovery).modifyFlat("exiledSector_skill_reinforcedhull_1_SHIP_RECOVERY_CHANCE_BONUS", 1000f);
    }

    private static SkillNode registerNpcRoot() {
        SkillNode root = new SkillNode("root_1", new SkillType.Builder("root", "Root", "a.png", SkillTier.ROOT).build(), List.of(), 0f, 0f);
        SkillTree.register(root);
        return root;
    }

    private static ShipVariantAPI npcVariant(String... nodeIds) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of("exiledSector_npcTree|bulwark|" + nodeIds.length + "|root_1,"
                + String.join(",", nodeIds)));
        return variant;
    }

    @Test
    void crewBasedGroundSupportCountsCrewBonusesFromNodesAllocatedAfterIt() {
        registerNpcRoot();
        SkillType groundType = new SkillType.Builder("ground", "Ground", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(LogisticsSkillEffect.GROUND_SUPPORT_PER_MAX_CREW_PERCENT, 25f)))
                .build();
        SkillType crewType = new SkillType.Builder("crew", "Crew", "a.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(LogisticsSkillEffect.CREW_CAPACITY_PERCENT, 50f)))
                .build();
        SkillTree.register(new SkillNode("ground_1", groundType, List.of("root_1"), 0f, 0f));
        SkillTree.register(new SkillNode("crew_1", crewType, List.of("root_1"), 0f, 0f));
        ShipVariantAPI variant = npcVariant("ground_1", "crew_1");
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getMaxCrew()).thenReturn(400f);
        when(variant.getHullSpec()).thenReturn(hullSpec);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        StatBonus groundSupport = new StatBonus();
        FleetMemberAPI raider = memberWithId("npc-raider");
        when(stats.getFleetMember()).thenReturn(raider);
        when(stats.getVariant()).thenReturn(variant);
        when(stats.getMaxCrewMod()).thenReturn(new StatBonus());
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getMod(Stats.FLEET_GROUND_SUPPORT)).thenReturn(groundSupport);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.CRUISER, stats, SkillTreeHullMod.ID);

        assertEquals(150f, groundSupport.getFlatBonus(), 0.001f);
    }

    @Test
    void anNpcTaggedShipGetsItsTaggedNodesEffectsWithoutCreatingASavedTree() {
        registerNpcRoot();
        SkillType hullType = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .build();
        SkillTree.register(new SkillNode("hull_1", hullType, List.of("root_1"), 0f, 0f));
        FleetMemberAPI member = memberWithId("npc-ship");
        ShipVariantAPI variant = npcVariant("hull_1");
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        when(stats.getVariant()).thenReturn(variant);
        StatBonus hullBonus = mock(StatBonus.class);
        when(stats.getHullBonus()).thenReturn(hullBonus);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, SkillTreeHullMod.ID);

        verify(hullBonus).modifyPercent("exiledSector_skill_hull_1_HULL_PERCENT", 10f);
        assertTrue(persistentData.isEmpty());
    }

    @Test
    void anNpcTaggedShipNeverReservesOpOrStripsConflictingHullmods() {
        registerNpcRoot();
        SkillType frontType = new SkillType.Builder("frontemitter", "Shield Conversion - Front", "a.png", SkillTier.NOTABLE)
                .exclusiveHullModIds(List.of("adaptiveshields"))
                .build();
        SkillTree.register(new SkillNode("frontemitter_1", frontType, List.of("root_1"), 0f, 0f));
        ShipVariantAPI variant = npcVariant("frontemitter_1");
        when(variant.hasHullMod("adaptiveshields")).thenReturn(true);
        FleetMemberAPI member = memberWithId("npc-ship");
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        when(stats.getVariant()).thenReturn(variant);
        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, SkillTreeHullMod.ID);

        verify(variant, never()).removeMod(anyString());
        verify(variant, never()).addMod(anyString());
        verify(settings, never()).getHullModSpec(anyString());
        assertTrue(persistentData.isEmpty());
    }

    @Test
    void anNpcTaggedShipStillGetsThePhantomHullModsItsNodesPlace() {
        registerNpcRoot();
        SkillType militarizedType = new SkillType.Builder("militarized_subsystems", "Militarized Subsystems", "a.png", SkillTier.NOTABLE)
                .phantomHullModIds(List.of("militarized_subsystems"))
                .build();
        PhantomHullModStatus.markActive("militarized_subsystems");
        SkillTree.register(new SkillNode("militarized_subsystems_1", militarizedType, List.of("root_1"), 0f, 0f));
        ShipVariantAPI variant = npcVariant("militarized_subsystems_1");
        FleetMemberAPI member = memberWithId("npc-ship");
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getVariant()).thenReturn(variant);
        when(stats.getFleetMember()).thenReturn(member);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, SkillTreeHullMod.ID);

        verify(variant).addPermaMod("militarized_subsystems");
        verify(variant).addTag("exiledSector_installed_militarized_subsystems");
    }

    @Test
    void theGamesOpCostPassAppliesNodeStatsButNeverEditsTheVariant() {
        registerNpcRoot();
        SkillType militarizedType = new SkillType.Builder("militarized_subsystems", "Militarized Subsystems", "a.png", SkillTier.NOTABLE)
                .phantomHullModIds(List.of("militarized_subsystems"))
                .effects(List.of(new SkillTypeEffect(LogisticsSkillEffect.BURN_LEVEL_FLAT, 1f)))
                .build();
        PhantomHullModStatus.markActive("militarized_subsystems");
        SkillTree.register(new SkillNode("militarized_subsystems_1", militarizedType, List.of("root_1"), 0f, 0f));
        ShipVariantAPI variant = npcVariant("militarized_subsystems_1");
        MutableShipStatsAPI opCostStats = mock(MutableShipStatsAPI.class);
        MutableStat burnLevel = mock(MutableStat.class);
        when(opCostStats.getVariant()).thenReturn(variant);
        when(opCostStats.getMaxBurnLevel()).thenReturn(burnLevel);

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, opCostStats, SkillTreeHullMod.ID);

        verify(burnLevel).modifyFlat("exiledSector_skill_militarized_subsystems_1_BURN_LEVEL_FLAT", 1f);
        verify(variant, never()).addPermaMod(anyString());
        verify(variant, never()).addTag(anyString());
        verify(variant, never()).addMod(anyString());
        verify(variant, never()).removeMod(anyString());
    }

    @Test
    void publicSyncHelpersLeaveNpcTaggedShipsAlone() {
        registerNpcRoot();
        ShipVariantAPI variant = npcVariant();
        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);

        OpReserveHullMods.sync(memberWithId("npc-ship"), variant);
        HullModConflictResolver.removeConflicts(memberWithId("npc-ship"), variant);

        verify(settings, never()).getHullModSpec(anyString());
        verify(variant, never()).addMod(anyString());
        verify(variant, never()).removeMod(anyString());
        assertTrue(persistentData.isEmpty());
    }

    @Test
    void combatHooksReadTheNpcTreeFromTheShipsVariant() {
        registerNpcRoot();
        exiledsector.skills.skilleffect.SkillEffect listenerEffect = mock(exiledsector.skills.skilleffect.SkillEffect.class);
        when(listenerEffect.name()).thenReturn("TEST_EFFECT");
        when(listenerEffect.appliesToNpcShips()).thenReturn(true);
        SkillType listenerType = new SkillType.Builder("listener", "Listener", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(listenerEffect, 50f)))
                .build();
        SkillTree.register(new SkillNode("listener_1", listenerType, List.of("root_1"), 0f, 0f));
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipVariantAPI variant = npcVariant("listener_1");
        ShipAPI ship = mockShip(null, stats);
        when(ship.getVariant()).thenReturn(variant);

        new SkillTreeHullMod().applyEffectsAfterShipCreation(ship, SkillTreeHullMod.ID);

        verify(listenerEffect).applyAfterShipCreation(ship, "exiledSector_skill_listener_1_TEST_EFFECT", 50f);
        assertTrue(persistentData.isEmpty());
    }

    private record DamageTakenStats(MutableStat emp, MutableStat energy, MutableStat energyOnShields) {
    }

    private DamageTakenStats damageTakenAfterAllocating(SkillTypeEffect... nodeEffects) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-a");
        for (int i = 0; i < nodeEffects.length; i++) {
            SkillType type = new SkillType.Builder("damage_taken_" + i, "Damage Taken", "a.png", SkillTier.SMALL)
                    .effects(List.of(nodeEffects[i])).build();
            SkillNode node = new SkillNode("damage_taken_node_" + i, type, List.of(), 0f, 0f);
            SkillTree.register(node);
            ShipTrees.allocate(ShipSkillDataManager.get("ship-a"), node, 1);
        }
        DamageTakenStats damageTaken = new DamageTakenStats(mock(MutableStat.class), mock(MutableStat.class), mock(MutableStat.class));
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getFleetMember()).thenReturn(member);
        when(stats.getEmpDamageTakenMult()).thenReturn(damageTaken.emp());
        when(stats.getEnergyDamageTakenMult()).thenReturn(damageTaken.energy());
        when(stats.getEnergyShieldDamageTakenMult()).thenReturn(damageTaken.energyOnShields());

        new SkillTreeHullMod().applyEffectsBeforeShipCreation(HullSize.FRIGATE, stats, "exiledSector_core");
        return damageTaken;
    }

    @Test
    void empDamageTakenReductionsUnderTheCapApplyAsUsual() {
        DamageTakenStats damageTaken = damageTakenAfterAllocating(new SkillTypeEffect(DefenseSkillEffect.EMP_DAMAGE_TAKEN_MULT, -30f),
                new SkillTypeEffect(DefenseSkillEffect.EMP_DAMAGE_TAKEN_PERCENT, -10f));

        verify(damageTaken.emp()).modifyMult("exiledSector_skillMult_EMP_DAMAGE_TAKEN_MULT", 0.7f);
        verify(damageTaken.emp()).modifyPercent("exiledSector_skill_damage_taken_node_1_EMP_DAMAGE_TAKEN_PERCENT", -10f);
        verify(damageTaken.emp(), never()).modifyMult(eq("exiledSector_skillCapped_EMP_DAMAGE_TAKEN_MULT"), anyFloat());
    }

    @Test
    void empDamageTakenReductionsFromMultipliersAndPercentagesTogetherStopAtEightyPercent() {
        DamageTakenStats damageTaken = damageTakenAfterAllocating(new SkillTypeEffect(DefenseSkillEffect.EMP_DAMAGE_TAKEN_MULT, -30f),
                new SkillTypeEffect(DefenseSkillEffect.EMP_DAMAGE_TAKEN_MULT, -40f),
                new SkillTypeEffect(DefenseSkillEffect.EMP_DAMAGE_TAKEN_PERCENT, -15f),
                new SkillTypeEffect(DefenseSkillEffect.EMP_DAMAGE_TAKEN_PERCENT, -15f),
                new SkillTypeEffect(DefenseSkillEffect.EMP_DAMAGE_TAKEN_PERCENT, -15f));

        verify(damageTaken.emp()).modifyMult(eq("exiledSector_skillCapped_EMP_DAMAGE_TAKEN_MULT"), org.mockito.AdditionalMatchers.eq(0.2f, 0.0001f));
        verify(damageTaken.emp(), never()).modifyMult(eq("exiledSector_skillMult_EMP_DAMAGE_TAKEN_MULT"), anyFloat());
        verify(damageTaken.emp(), never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void energyDamageTakenReductionsStopAtEightyPercentOnHullAndShields() {
        SkillTypeEffect[] reductions = new SkillTypeEffect[16];
        java.util.Arrays.fill(reductions, new SkillTypeEffect(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT, -6f));
        DamageTakenStats damageTaken = damageTakenAfterAllocating(reductions);

        verify(damageTaken.energy()).modifyPercent("exiledSector_skillCapped_ENERGY_DAMAGE_TAKEN_PERCENT", -80f);
        verify(damageTaken.energyOnShields()).modifyPercent("exiledSector_skillCapped_ENERGY_DAMAGE_TAKEN_PERCENT", -80f);
        verify(damageTaken.energy(), never()).modifyPercent(org.mockito.ArgumentMatchers.startsWith("exiledSector_skill_"), anyFloat());
    }
}
