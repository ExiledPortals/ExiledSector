package exiledsector.skills;

import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipRecords;
import exiledsector.persistence.ShipSkillDataManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class SkillDataResolverTest {

    private static final String NPC_TAG = "exiledSector_npcTree|bulwark|2|root,a";

    private MockedStatic<ShipSkillDataManager> dataManagerMock;

    @BeforeEach
    void setUp() {
        SkillDataResolver.clearCache();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        SkillType rootType = new SkillType.Builder("root_type", "Root", "a.png", SkillTier.ROOT).build();
        SkillType smallType = new SkillType.Builder("a_type", "A", "a.png", SkillTier.SMALL).build();
        SkillTree.register(new SkillNode("root", rootType, List.of(), 0f, 0f));
        SkillTree.register(new SkillNode("a", smallType, List.of("root"), 0f, 0f));
        dataManagerMock = Mockito.mockStatic(ShipSkillDataManager.class);
    }

    @AfterEach
    void tearDown() {
        dataManagerMock.close();
        SkillDataResolver.clearCache();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    private static ShipVariantAPI variantWithTags(String... tags) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of(tags));
        return variant;
    }

    private static FleetMemberAPI member(String id) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        return member;
    }

    @Test
    void anNpcTaggedVariantResolvesToItsTaggedTreeWithoutTouchingTheSave() {
        ShipSkillData data = SkillDataResolver.resolve(member("npc-1"), variantWithTags(NPC_TAG));

        assertEquals(List.of("root", "a"), List.copyOf(data.getAllocatedNodeIds()));
        assertTrue(data.isNpcBuild());
        dataManagerMock.verifyNoInteractions();
    }

    @Test
    void theSameTagIsDecodedOnlyOnce() {
        ShipSkillData first = SkillDataResolver.resolve(member("npc-1"), variantWithTags(NPC_TAG));
        ShipSkillData second = SkillDataResolver.resolve(null, variantWithTags(NPC_TAG));

        assertSame(first, second);
    }

    @Test
    void clearingTheCacheDecodesTagsAfresh() {
        ShipSkillData before = SkillDataResolver.resolve(null, variantWithTags(NPC_TAG));
        SkillDataResolver.clearCache();

        assertNotSame(before, SkillDataResolver.resolve(null, variantWithTags(NPC_TAG)));
    }

    @Test
    void theNpcTreeCacheDropsTheLeastRecentlyUsedTreesOnceFull() {
        ShipSkillData first = SkillDataResolver.resolve(null, variantWithTags("exiledSector_npcTree|first|2|root,a"));
        ShipSkillData kept = SkillDataResolver.resolve(null, variantWithTags(NPC_TAG));
        for (int i = 0; i < 1023; i++) {
            SkillDataResolver.resolve(null, variantWithTags("exiledSector_npcTree|filler" + i + "|2|root,a"));
            SkillDataResolver.resolve(null, variantWithTags(NPC_TAG));
        }

        assertSame(kept, SkillDataResolver.resolve(null, variantWithTags(NPC_TAG)));
        assertNotSame(first, SkillDataResolver.resolve(null, variantWithTags("exiledSector_npcTree|first|2|root,a")));
    }

    @Test
    void aMalformedNpcTagGivesAnEmptyNpcTreeInsteadOfTheSavedOne() {
        ShipSkillData data = SkillDataResolver.resolve(member("npc-1"), variantWithTags("exiledSector_npcTree|broken"));

        assertTrue(data.getAllocatedNodeIds().isEmpty());
        assertTrue(data.isNpcBuild());
        dataManagerMock.verifyNoInteractions();
    }

    @Test
    void anUntaggedShipUsesItsSavedTree() {
        ShipSkillData saved = new ShipSkillData();
        dataManagerMock.when(() -> ShipSkillDataManager.find("ship-a")).thenReturn(saved);

        assertSame(saved, SkillDataResolver.resolve(member("ship-a"), variantWithTags("exiledSector_installed_x")));
        assertSame(saved, SkillDataResolver.resolve(member("ship-a"), null));
    }

    @Test
    void anUntaggedShipWithNoSavedTreeReadsAsBlankWithoutOneBeingStored() {
        ShipSkillData resolved = SkillDataResolver.resolve(member("temporary-copy"), variantWithTags());

        assertTrue(resolved.isBlank());
        dataManagerMock.verify(() -> ShipSkillDataManager.get("temporary-copy"), never());
    }

    @Test
    void aShipWithNoSavedTreeKeepsOneBlankRecordSoItsResolvedTreeStaysCached() {
        FleetMemberAPI unsaved = member("unsaved");
        ShipSkillData first = SkillDataResolver.resolve(unsaved, variantWithTags());

        assertSame(first, SkillDataResolver.resolve(unsaved, variantWithTags()));
        assertNotSame(first, SkillDataResolver.resolve(member("other"), variantWithTags()));
    }

    @Test
    void anUntaggedShipWithoutAFleetMemberHasNoTree() {
        assertNull(SkillDataResolver.resolve(null, variantWithTags()));
        assertNull(SkillDataResolver.resolve(null, null));
    }

    @Test
    void theOpCostPassFindsAPlayerShipsTreeThroughItsShipTag() {
        ShipSkillData saved = new ShipSkillData();
        dataManagerMock.when(() -> ShipSkillDataManager.find("ship-a")).thenReturn(saved);

        assertSame(saved, SkillDataResolver.resolve(null, variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "ship-a")));
        assertNull(SkillDataResolver.resolve(null, variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "sold")));
    }

    @Test
    void aStandInMemberOutsideAnyFleetBorrowsTheTreeItsVariantIsTaggedWith() {
        ShipSkillData saved = new ShipSkillData();
        dataManagerMock.when(() -> ShipSkillDataManager.find("ship-a")).thenReturn(saved);
        ShipVariantAPI variant = variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "ship-a");

        assertSame(saved, SkillDataResolver.resolve(member("sim-copy"), variant));
        assertTrue(SkillDataResolver.borrowsShipTree(member("sim-copy"), variant));
    }

    @Test
    void aMemberInAFleetNeverBorrowsTheTreeOfACopiedShipTag() {
        dataManagerMock.when(() -> ShipSkillDataManager.find("ship-a")).thenReturn(new ShipSkillData());
        ShipVariantAPI variant = variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "ship-a");
        FleetMemberAPI fleetMember = member("ship-b");
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(fleetMember));
        when(fleetMember.getFleetData()).thenReturn(fleetData);

        assertTrue(SkillDataResolver.resolve(fleetMember, variant).isBlank());
        assertFalse(SkillDataResolver.borrowsShipTree(fleetMember, variant));
    }

    @Test
    void aRefitModuleTabBorrowsTheTreeOfTheShipDockedInThatSlot() {
        ShipSkillData docked = new ShipSkillData();
        dataManagerMock.when(() -> ShipSkillDataManager.find("docked-core")).thenReturn(docked);
        ShipVariantAPI moduleVariant = variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "docked-core");
        FleetMemberAPI moduleTab = member("module-tab");
        FleetMemberAPI parent = member("parent");
        FleetDataAPI parentFleetData = mock(FleetDataAPI.class);
        when(parentFleetData.getMembersListCopy()).thenReturn(List.of(parent));
        when(moduleTab.getFleetData()).thenReturn(parentFleetData);

        assertSame(docked, SkillDataResolver.resolve(moduleTab, moduleVariant));
        assertTrue(SkillDataResolver.borrowsShipTree(moduleTab, moduleVariant));
    }

    @Test
    void aMemberWithItsOwnTreeOrAnUntaggedVariantBorrowsNothing() {
        dataManagerMock.when(() -> ShipSkillDataManager.find("ship-a")).thenReturn(new ShipSkillData());
        dataManagerMock.when(() -> ShipSkillDataManager.find("ship-b")).thenReturn(new ShipSkillData());

        assertFalse(SkillDataResolver.borrowsShipTree(member("ship-b"), variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "ship-a")));
        assertFalse(SkillDataResolver.borrowsShipTree(member("sim-copy"), variantWithTags()));
        assertFalse(SkillDataResolver.borrowsShipTree(member("sim-copy"), variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "sold")));
        assertFalse(SkillDataResolver.borrowsShipTree(null, variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "ship-a")));
    }

    @Test
    void aShipTaggedWithItsOwnIdWhoseTreeIsNotInTheSectorHasItsSavedTreeOutOfReach() {
        dataManagerMock.when(() -> ShipSkillDataManager.find("ship-b")).thenReturn(new ShipSkillData());

        assertTrue(ShipRecords.savedTreeOutOfReach(member("ship-a"), variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "ship-a")));
        assertFalse(ShipRecords.savedTreeOutOfReach(member("ship-b"), variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "ship-b")));
        assertFalse(ShipRecords.savedTreeOutOfReach(member("sim-copy"), variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "ship-a")));
        assertFalse(ShipRecords.savedTreeOutOfReach(member("ship-a"), variantWithTags()));
        assertFalse(ShipRecords.savedTreeOutOfReach(null, variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "ship-a")));
    }

    @Test
    void syncingTheShipTagReplacesAStaleTagAndForgetsTheCachedOpCostStats() {
        dataManagerMock.when(() -> ShipSkillDataManager.find("ship-a")).thenReturn(new ShipSkillData());
        ShipVariantAPI variant = variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "copied-from");

        SkillDataResolver.syncShipTag(member("ship-a"), variant);

        Mockito.verify(variant).removeTag(ShipRecords.SHIP_TAG_PREFIX + "copied-from");
        Mockito.verify(variant).addTag(ShipRecords.SHIP_TAG_PREFIX + "ship-a");
        Mockito.verify(variant).removeMod(Mockito.anyString());
    }

    @Test
    void aShipWithoutASavedTreeLosesItsShipTagAndAnUpToDateTagIsLeftAlone() {
        ShipVariantAPI untreed = variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "ship-b");
        dataManagerMock.when(() -> ShipSkillDataManager.find("ship-a")).thenReturn(new ShipSkillData());
        ShipVariantAPI current = variantWithTags(ShipRecords.SHIP_TAG_PREFIX + "ship-a");
        when(current.hasTag(ShipRecords.SHIP_TAG_PREFIX + "ship-a")).thenReturn(true);

        SkillDataResolver.syncShipTag(member("ship-b"), untreed);
        SkillDataResolver.syncShipTag(member("ship-a"), current);

        Mockito.verify(untreed).removeTag(ShipRecords.SHIP_TAG_PREFIX + "ship-b");
        Mockito.verify(untreed, never()).addTag(Mockito.anyString());
        Mockito.verify(current, never()).removeTag(Mockito.anyString());
        Mockito.verify(current, never()).addTag(Mockito.anyString());
        Mockito.verify(current, never()).addMod(Mockito.anyString());
    }

    @Test
    void reportsWhetherAVariantCarriesAnNpcTree() {
        assertTrue(SkillDataResolver.isNpcTree(variantWithTags(NPC_TAG)));
        assertFalse(SkillDataResolver.isNpcTree(variantWithTags("exiledSector_installed_x")));
        assertFalse(SkillDataResolver.isNpcTree(null));
    }
}
