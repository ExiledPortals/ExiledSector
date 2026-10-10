package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.LearnedPhantomConflicts;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.magiclib.util.MagicIncompatibleHullmods;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhantomConflictWatchTest {

    private static final String SAFETY_OVERRIDES = "safetyoverrides";
    private static final String MODDED_EFFECT = "modded.hullmods.Effect";

    private final Map<String, Object> persistentData = new HashMap<>();
    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        when(settings.getHullModSpec(anyString())).thenAnswer(invocation -> spec(MODDED_EFFECT));
        when(settings.getHullModSpec("hardenedshieldemitter")).thenAnswer(invocation -> spec(PhantomConflictWatch.VANILLA_HULL_MOD_PACKAGE + "Hardened"));
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getSector).thenReturn(sector);
        LearnedPhantomConflicts.load();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    @AfterEach
    void tearDown() {
        PhantomConflictWatch.clearPendingReverts();
        MagicIncompatibleHullmods.clearData();
        LearnedPhantomConflicts.load();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        globalMock.close();
    }

    private static HullModSpecAPI spec(String effectClass) {
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.getEffectClass()).thenReturn(effectClass);
        return spec;
    }

    private static ShipVariantAPI strippedVariant(String baseHullId, List<String> hullMods, Set<String> builtIns) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getBaseHullId()).thenReturn(baseHullId);
        when(hullSpec.isBuiltInMod(anyString())).thenAnswer(invocation -> builtIns.contains(invocation.<String>getArgument(0)));
        when(variant.getHullSpec()).thenReturn(hullSpec);
        when(variant.getHullMods()).thenReturn(new LinkedHashSet<>(hullMods));
        when(variant.hasHullMod(anyString())).thenAnswer(invocation -> hullMods.contains(invocation.<String>getArgument(0)));
        when(variant.getPermaMods()).thenReturn(new LinkedHashSet<>());
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>());
        when(variant.hasTag(InstalledHullMods.tag(SAFETY_OVERRIDES))).thenReturn(true);
        return variant;
    }

    private static FleetMemberAPI member(String memberId) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(memberId);
        return member;
    }

    private static boolean blocks(String baseHullId, Set<String> hullMods) {
        return LearnedPhantomConflicts.conflictFor(SAFETY_OVERRIDES, baseHullId, hullMods::contains) != null;
    }

    @Test
    void aBuiltInHullModNamedByMagicLibIsLearnedAndTheNodeQueuedForRemoval() {
        List<String> hullMods = new ArrayList<>(List.of(SAFETY_OVERRIDES, "csp_cetanframe"));
        ShipVariantAPI variant = strippedVariant("cetan", hullMods, Set.of("csp_cetanframe"));
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>());
        MagicIncompatibleHullmods.removeHullmodWithWarning(variant, SAFETY_OVERRIDES, "csp_cetanframe");
        hullMods.remove(SAFETY_OVERRIDES);

        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertTrue(blocks("other", Set.of("csp_cetanframe")));
        assertTrue(PhantomConflictWatch.hasPendingReverts());
    }

    @Test
    void aRemovableCauseIsLeftToTheExistingResolver() {
        List<String> hullMods = new ArrayList<>(List.of(SAFETY_OVERRIDES, "rat_exogrid_overload"));
        ShipVariantAPI variant = strippedVariant("wolf", hullMods, Set.of());
        MagicIncompatibleHullmods.removeHullmodWithWarning(variant, SAFETY_OVERRIDES, "rat_exogrid_overload");
        hullMods.remove(SAFETY_OVERRIDES);

        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertFalse(blocks("wolf", Set.of("rat_exogrid_overload")));
        assertFalse(PhantomConflictWatch.hasPendingReverts());
    }

    @Test
    void aPlainRemovalByTheOnlyModdedBuiltInIsBlamedOnIt() {
        ShipVariantAPI variant = strippedVariant("ohm", List.of("ashesofohm_domainmachinerymod", "hardenedshieldemitter"),
                Set.of("ashesofohm_domainmachinerymod"));

        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertTrue(blocks("anything", Set.of("ashesofohm_domainmachinerymod")));
    }

    @Test
    void aPlainRemovalWithSeveralModdedBuiltInsBlocksTheWholeHull() {
        ShipVariantAPI variant = strippedVariant("gr_nunki", List.of("gr_NunkiCR", "gr_GhostDesign"), Set.of("gr_NunkiCR", "gr_GhostDesign"));

        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertTrue(blocks("gr_nunki", Set.of()));
        assertFalse(blocks("wolf", Set.of("gr_NunkiCR")));
    }

    @Test
    void aPlainRemovalThatMightBeAFittedModIsNotLearnedButStillRevertsThisShip() {
        ShipVariantAPI variant = strippedVariant("wolf", List.of("csp_cetanframe", "fitted_modded_mod"), Set.of("csp_cetanframe"));

        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertFalse(blocks("wolf", Set.of("csp_cetanframe", "fitted_modded_mod")));
        assertTrue(PhantomConflictWatch.hasPendingReverts());
    }

    @Test
    void npcShipsTeachConflictsButAreNeverReverted() {
        ShipVariantAPI variant = strippedVariant("cetan", List.of("csp_cetanframe"), Set.of("csp_cetanframe"));

        PhantomConflictWatch.inspect(member("npc"), variant, Set.of(SAFETY_OVERRIDES), false);

        assertTrue(blocks("cetan", Set.of("csp_cetanframe")));
        assertFalse(PhantomConflictWatch.hasPendingReverts());
    }

    @Test
    void anUntouchedPhantomIsNotAConflict() {
        ShipVariantAPI variant = strippedVariant("cetan", List.of(SAFETY_OVERRIDES, "csp_cetanframe"), Set.of("csp_cetanframe"));

        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertFalse(blocks("cetan", Set.of("csp_cetanframe")));
        assertFalse(PhantomConflictWatch.hasPendingReverts());
    }

    @Test
    void learnedConflictsStopThePhantomBeingPlacedOnThatShipOnly() {
        LearnedPhantomConflicts.learnHullMod(SAFETY_OVERRIDES, "csp_cetanframe");
        ShipVariantAPI cetan = strippedVariant("cetan", List.of("csp_cetanframe"), Set.of("csp_cetanframe"));
        when(cetan.hasTag(InstalledHullMods.tag(SAFETY_OVERRIDES))).thenReturn(false);
        ShipVariantAPI wolf = strippedVariant("wolf", List.of(), Set.of());

        assertEquals(Set.of(), PhantomConflictWatch.withoutLearnedConflicts(Set.of(SAFETY_OVERRIDES), cetan));
        assertEquals(Set.of(SAFETY_OVERRIDES), PhantomConflictWatch.withoutLearnedConflicts(Set.of(SAFETY_OVERRIDES), wolf));
    }

    @Test
    void aQueuedRevertRemovesTheProvidingNodeAndWhatDependedOnIt() {
        SkillType rootType = new SkillType.Builder("root", "Root", "a.png", SkillTier.ROOT).effects(List.of()).build();
        SkillType overridesType = new SkillType.Builder("safety_overrides", "Safety Overrides", "a.png", SkillTier.NOTABLE).effects(List.of())
                .phantomHullModIds(List.of(SAFETY_OVERRIDES)).build();
        SkillType plainType = new SkillType.Builder("plain", "Plain", "a.png", SkillTier.SMALL).effects(List.of()).build();
        SkillNode root = new SkillNode("root_1", rootType, List.of("overrides_1"), 0f, 0f);
        SkillNode overrides = new SkillNode("overrides_1", overridesType, List.of("root_1", "beyond_1"), 0f, 0f);
        SkillNode beyond = new SkillNode("beyond_1", plainType, List.of("overrides_1"), 0f, 0f);
        for (SkillType type : List.of(rootType, overridesType, plainType)) {
            SkillTree.registerType(type);
        }
        for (SkillNode node : List.of(root, overrides, beyond)) {
            SkillTree.register(node);
        }
        ShipSkillData data = ShipSkillDataManager.get("ship-a");
        data.chooseStartingRoot(root);
        data.allocate(overrides, 3);
        data.allocate(beyond, 3);
        ShipVariantAPI variant = strippedVariant("cetan", List.of("csp_cetanframe"), Set.of("csp_cetanframe"));
        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertTrue(PhantomConflictWatch.applyPendingReverts("ship-a"));

        assertTrue(data.isAllocated("root_1"));
        assertFalse(data.isAllocated("overrides_1"));
        assertFalse(data.isAllocated("beyond_1"));
        assertFalse(PhantomConflictWatch.hasPendingReverts());
    }
}
