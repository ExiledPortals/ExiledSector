package exiledsector.effects;

import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.persistence.ShipRecords;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModuleLinksTest {

    private static FleetMemberAPI member(String id, ShipVariantAPI ownVariant) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        when(member.getVariant()).thenReturn(ownVariant);
        when(member.getFleetData()).thenReturn(mock(FleetDataAPI.class));
        return member;
    }

    private static boolean linkedTo(ShipVariantAPI moduleVariant, String parentMemberId) {
        return moduleVariant.getPermaMods().contains(ModuleHullMod.ID) && parentMemberId.equals(ModuleLinks.parentId(moduleVariant));
    }

    @Test
    void everyModuleSlotGetsTheHullModAndThePartnerTag() {
        ShipVariantAPI front = ModuleVariants.variant(VariantSource.REFIT);
        ShipVariantAPI rear = ModuleVariants.variant(VariantSource.REFIT);
        ShipVariantAPI parent = ModuleVariants.parent(VariantSource.REFIT, Map.of("WS0001", front, "WS0002", rear));

        ModuleLinks.sync(member("parent", parent), parent);

        assertTrue(linkedTo(front, "parent"));
        assertTrue(linkedTo(rear, "parent"));
        assertEquals(Set.of(ModuleLinks.PARENT_TAG_PREFIX + "parent"), front.getTags());
    }

    @Test
    void aStockModuleVariantIsClonedToRefitBeforeItIsStored() {
        ShipVariantAPI stockModule = ModuleVariants.variant(VariantSource.STOCK);
        ShipVariantAPI clonedModule = ModuleVariants.variant(VariantSource.STOCK);
        when(stockModule.clone()).thenReturn(clonedModule);
        ShipVariantAPI parent = ModuleVariants.parent(VariantSource.REFIT, Map.of("WS0001", stockModule));

        ModuleLinks.sync(member("parent", parent), parent);

        verify(clonedModule).setSource(VariantSource.REFIT);
        verify(parent).setModuleVariant("WS0001", clonedModule);
        assertSame(clonedModule, parent.getModuleVariant("WS0001"));
        assertTrue(linkedTo(clonedModule, "parent"));
        verify(stockModule, never()).addTag(anyString());
        verify(stockModule, never()).addPermaMod(anyString());
    }

    @Test
    void aSharedEmptyHullModuleVariantIsClonedToo() {
        ShipVariantAPI hullModule = ModuleVariants.variant(VariantSource.HULL);
        ShipVariantAPI clonedModule = ModuleVariants.variant(VariantSource.HULL);
        when(hullModule.clone()).thenReturn(clonedModule);
        ShipVariantAPI parent = ModuleVariants.parent(VariantSource.REFIT, Map.of("WS0001", hullModule));

        ModuleLinks.sync(member("parent", parent), parent);

        verify(clonedModule).setSource(VariantSource.REFIT);
        assertTrue(linkedTo(clonedModule, "parent"));
        assertNull(ModuleLinks.parentId(hullModule));
    }

    @Test
    void aLinkToAnOldParentIdIsReplaced() {
        ShipVariantAPI module = ModuleVariants.linkedModule("old-parent");
        ShipVariantAPI parent = ModuleVariants.parent(VariantSource.REFIT, Map.of("WS0001", module));

        ModuleLinks.sync(member("new-parent", parent), parent);

        assertTrue(linkedTo(module, "new-parent"));
        assertEquals(1, module.getTags().size());
    }

    @Test
    void aModuleWithATreeOfItsOwnIsLeftUnlinked() {
        ShipVariantAPI dockedShip = ModuleVariants.linkedModule("parent");
        dockedShip.addTag(ShipRecords.SHIP_TAG_PREFIX + "docked");
        ShipVariantAPI parent = ModuleVariants.parent(VariantSource.REFIT, Map.of("WS0001", dockedShip));

        try (MockedStatic<ShipSkillDataManager> records = Mockito.mockStatic(ShipSkillDataManager.class)) {
            records.when(() -> ShipSkillDataManager.find("docked")).thenReturn(new ShipSkillData());
            ModuleLinks.sync(member("parent", parent), parent);
        }

        assertNull(ModuleLinks.parentId(dockedShip));
        assertFalse(dockedShip.getPermaMods().contains(ModuleHullMod.ID));
        assertTrue(dockedShip.getTags().contains(ShipRecords.SHIP_TAG_PREFIX + "docked"));
    }

    @Test
    void aStockParentVariantIsNeverEdited() {
        ShipVariantAPI stockModule = ModuleVariants.variant(VariantSource.STOCK);
        ShipVariantAPI parent = ModuleVariants.parent(VariantSource.STOCK, Map.of("WS0001", stockModule));

        ModuleLinks.sync(member("parent", parent), parent);

        verify(parent, never()).setModuleVariant(anyString(), Mockito.any());
        verify(stockModule, never()).clone();
        assertNull(ModuleLinks.parentId(stockModule));
    }

    @Test
    void theVariantBeingEditedIsLinkedBesideTheMembersOwn() {
        ShipVariantAPI savedModule = ModuleVariants.variant(VariantSource.REFIT);
        ShipVariantAPI editedModule = ModuleVariants.variant(VariantSource.REFIT);
        ShipVariantAPI saved = ModuleVariants.parent(VariantSource.REFIT, Map.of("WS0001", savedModule));
        ShipVariantAPI edited = ModuleVariants.parent(VariantSource.REFIT, Map.of("WS0001", editedModule));

        ModuleLinks.sync(member("parent", saved), edited);

        assertTrue(linkedTo(savedModule, "parent"));
        assertTrue(linkedTo(editedModule, "parent"));
    }

    @Test
    void aMemberOutsideAnyFleetLinksNothingBecauseItIsOnlyAStandIn() {
        ShipVariantAPI module = ModuleVariants.variant(VariantSource.REFIT);
        ShipVariantAPI parent = ModuleVariants.parent(VariantSource.REFIT, Map.of("WS0001", module));
        FleetMemberAPI simulatorCopy = member("fresh-id", parent);
        when(simulatorCopy.getFleetData()).thenReturn(null);

        ModuleLinks.sync(simulatorCopy, parent);

        assertNull(ModuleLinks.parentId(module));
    }

    @Test
    void aFleetMembersOwnVariantDropsALinkItCarriedAsAModule() {
        ShipVariantAPI ejectedShip = ModuleVariants.linkedModule("old-parent");
        FleetMemberAPI ejected = member("ejected", ejectedShip);
        when(ejected.getFleetData().getMembersListCopy()).thenReturn(List.of(ejected));

        ModuleLinks.sync(ejected, ejectedShip);

        assertNull(ModuleLinks.parentId(ejectedShip));
        assertFalse(ejectedShip.getPermaMods().contains(ModuleHullMod.ID));
    }

    @Test
    void aModuleCarryingTheCoreHullModKeepsItsLinkWhileItIsBuiltAsAModule() {
        ShipVariantAPI dockedShip = ModuleVariants.linkedModule("parent");

        ModuleLinks.sync(member("temp", dockedShip), dockedShip);

        assertEquals("parent", ModuleLinks.parentId(dockedShip));
        assertTrue(dockedShip.getPermaMods().contains(ModuleHullMod.ID));
    }
}
