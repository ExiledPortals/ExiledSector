package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.compat.MagicLibCompat;
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

    private final Map<String, Object> persistentData = new HashMap<>();
    private MockedStatic<Global> globalMock;
    private SectorAPI sector;

    @BeforeEach
    void setUp() {
        SettingsAPI settings = mock(SettingsAPI.class);
        sector = mock(SectorAPI.class);
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

    private static ShipVariantAPI variant(String hullId, List<String> hullMods, Set<String> builtIns) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullId()).thenReturn(hullId);
        when(hullSpec.isBuiltInMod(anyString())).thenAnswer(invocation -> builtIns.contains(invocation.<String>getArgument(0)));
        when(variant.getHullSpec()).thenReturn(hullSpec);
        when(variant.getHullMods()).thenAnswer(invocation -> new LinkedHashSet<>(hullMods));
        when(variant.hasHullMod(anyString())).thenAnswer(invocation -> hullMods.contains(invocation.<String>getArgument(0)));
        when(variant.getPermaMods()).thenReturn(new LinkedHashSet<>());
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>());
        when(variant.hasTag(InstalledHullMods.tag(SAFETY_OVERRIDES))).thenReturn(true);
        return variant;
    }

    private static ShipVariantAPI attemptedRemoval(String hullId, String cause, boolean causeBuiltIn) {
        List<String> hullMods = new ArrayList<>(List.of(SAFETY_OVERRIDES, cause, MagicLibCompat.WARNING_HULLMOD_ID));
        ShipVariantAPI variant = variant(hullId, hullMods, causeBuiltIn ? Set.of(cause) : Set.of());
        MagicIncompatibleHullmods.removeHullmodWithWarning(variant, SAFETY_OVERRIDES, cause);
        return variant;
    }

    private static FleetMemberAPI member(String memberId) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(memberId);
        return member;
    }

    private static boolean blocks(String hullId, Set<String> hullMods) {
        return LearnedPhantomConflicts.conflictFor(SAFETY_OVERRIDES, hullId, hullMods::contains) != null;
    }

    @Test
    void aBuiltInHullModThatTriesToRemoveThePhantomIsLearnedByName() {
        ShipVariantAPI variant = attemptedRemoval("cetan", "csp_cetanframe", true);

        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertTrue(blocks("any_other_hull", Set.of("csp_cetanframe")));
        assertFalse(blocks("cetan", Set.of()));
        assertTrue(PhantomConflictWatch.hasPendingReverts());
    }

    @Test
    void aRemovableHullModThatTriesToRemoveThePhantomIsLeftToTheResolver() {
        ShipVariantAPI variant = attemptedRemoval("wolf", "rat_exogrid_overload", false);

        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertFalse(blocks("wolf", Set.of("rat_exogrid_overload")));
        assertFalse(PhantomConflictWatch.hasPendingReverts());
    }

    @Test
    void aRemovalAttemptWithNoNamedCauseTeachesNothing() {
        ShipVariantAPI variant = attemptedRemoval("necro", "", true);

        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertTrue(LearnedPhantomConflicts.isEmpty());
        assertFalse(PhantomConflictWatch.hasPendingReverts());
    }

    @Test
    void aRemovalRecordIsNotReadWithoutMagicLibsWarningOnTheShip() {
        List<String> hullMods = new ArrayList<>(List.of(SAFETY_OVERRIDES, "csp_cetanframe"));
        ShipVariantAPI variant = variant("cetan", hullMods, Set.of("csp_cetanframe"));
        MagicIncompatibleHullmods.removeHullmodWithWarning(variant, SAFETY_OVERRIDES, "csp_cetanframe");

        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertTrue(LearnedPhantomConflicts.isEmpty());
    }

    @Test
    void aPhantomActuallyStrippedIsLearnedForThatHullOnly() {
        ShipVariantAPI variant = variant("asm_skin", List.of("asm_amorphous_alloy", "hardenedshieldemitter"), Set.of("asm_amorphous_alloy"));

        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertTrue(blocks("asm_skin", Set.of()));
        assertFalse(blocks("asm_other_skin", Set.of("asm_amorphous_alloy")));
        assertTrue(PhantomConflictWatch.hasPendingReverts());
    }

    @Test
    void npcShipsTeachConflictsButAreNeverReverted() {
        ShipVariantAPI variant = attemptedRemoval("cetan", "csp_cetanframe", true);

        PhantomConflictWatch.inspect(member("npc"), variant, Set.of(SAFETY_OVERRIDES), false);

        assertTrue(blocks("cetan", Set.of("csp_cetanframe")));
        assertFalse(PhantomConflictWatch.hasPendingReverts());
    }

    @Test
    void anUntouchedPhantomIsNotAConflict() {
        ShipVariantAPI variant = variant("cetan", List.of(SAFETY_OVERRIDES, "csp_cetanframe"), Set.of("csp_cetanframe"));

        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertTrue(LearnedPhantomConflicts.isEmpty());
        assertFalse(PhantomConflictWatch.hasPendingReverts());
    }

    @Test
    void learnedConflictsStopThePhantomBeingPlacedOnThatShipOnly() {
        LearnedPhantomConflicts.learnHullMod(SAFETY_OVERRIDES, "csp_cetanframe");
        ShipVariantAPI cetan = variant("cetan", List.of("csp_cetanframe"), Set.of("csp_cetanframe"));
        when(cetan.hasTag(InstalledHullMods.tag(SAFETY_OVERRIDES))).thenReturn(false);
        ShipVariantAPI wolf = variant("wolf", List.of(), Set.of());

        assertEquals(Set.of(), PhantomConflictWatch.withoutLearnedConflicts(Set.of(SAFETY_OVERRIDES), cetan));
        assertEquals(Set.of(SAFETY_OVERRIDES), PhantomConflictWatch.withoutLearnedConflicts(Set.of(SAFETY_OVERRIDES), wolf));
    }

    private ShipSkillData treeWithOverrides(String memberId) {
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
        ShipSkillData data = ShipSkillDataManager.get(memberId);
        data.chooseStartingRoot(root);
        data.allocate(overrides, 3);
        data.allocate(beyond, 3);
        return data;
    }

    private void playerFleetHolds(FleetMemberAPI... members) {
        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(playerFleet.getFleetData()).thenReturn(fleetData);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(members));
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
    }

    private static FleetMemberAPI fittedMember(String memberId, ShipVariantAPI variant) {
        FleetMemberAPI member = member(memberId);
        when(member.getVariant()).thenReturn(variant);
        return member;
    }

    @Test
    void aQueuedRevertRemovesTheProvidingNodeAndWhatDependedOnIt() {
        ShipSkillData data = treeWithOverrides("ship-a");
        ShipVariantAPI variant = attemptedRemoval("cetan", "csp_cetanframe", true);
        playerFleetHolds(fittedMember("ship-a", variant));
        PhantomConflictWatch.inspect(member("ship-a"), variant, Set.of(SAFETY_OVERRIDES), true);

        try (MockedStatic<ShipTreeSync> sync = Mockito.mockStatic(ShipTreeSync.class)) {
            assertTrue(PhantomConflictWatch.applyPendingReverts("ship-a"));
        }

        assertTrue(data.isAllocated("root_1"));
        assertFalse(data.isAllocated("overrides_1"));
        assertFalse(data.isAllocated("beyond_1"));
        assertFalse(PhantomConflictWatch.hasPendingReverts());
    }

    @Test
    void aShipNoLongerInThePlayerFleetIsNeverRevertedOrRefunded() {
        ShipSkillData data = treeWithOverrides("sold-ship");
        ShipVariantAPI variant = attemptedRemoval("cetan", "csp_cetanframe", true);
        playerFleetHolds();
        PhantomConflictWatch.inspect(member("sold-ship"), variant, Set.of(SAFETY_OVERRIDES), true);

        assertFalse(PhantomConflictWatch.applyPendingReverts());

        assertTrue(data.isAllocated("overrides_1"));
        assertFalse(PhantomConflictWatch.hasPendingReverts());
    }

    @Test
    void aRevertIsDroppedWhenTheShipNoLongerHasTheConflict() {
        ShipSkillData data = treeWithOverrides("ship-a");
        PhantomConflictWatch.inspect(member("ship-a"), attemptedRemoval("cetan", "csp_cetanframe", true), Set.of(SAFETY_OVERRIDES), true);
        playerFleetHolds(fittedMember("ship-a", variant("wolf", List.of(), Set.of())));

        assertFalse(PhantomConflictWatch.applyPendingReverts());

        assertTrue(data.isAllocated("overrides_1"));
    }

    @Test
    void aShipWhosePhantomIsBlockedByALearnedConflictIsQueuedEvenWithoutAFreshDetection() {
        LearnedPhantomConflicts.learnHullMod(SAFETY_OVERRIDES, "csp_cetanframe");
        Set<String> wanted = Set.of(SAFETY_OVERRIDES);

        PhantomConflictWatch.queueRevertsForBlocked(member("npc"), wanted, Set.of(), false);
        assertFalse(PhantomConflictWatch.hasPendingReverts());
        PhantomConflictWatch.queueRevertsForBlocked(member("ship-a"), wanted, wanted, true);
        assertFalse(PhantomConflictWatch.hasPendingReverts());
        PhantomConflictWatch.queueRevertsForBlocked(member("ship-a"), wanted, Set.of(), true);
        assertTrue(PhantomConflictWatch.hasPendingReverts());

        PhantomConflictWatch.clearPendingReverts();
        assertFalse(PhantomConflictWatch.hasPendingReverts());
    }
}
