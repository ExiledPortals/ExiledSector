package exiledsector.ui.refit;

import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipRecords;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefitModuleTabsTest {

    @Test
    void aMemberSharingTheFleetDataButMissingFromItsMembersIsAModuleTab() {
        FleetMemberAPI parent = mock(FleetMemberAPI.class);
        FleetMemberAPI module = mock(FleetMemberAPI.class);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(parent));
        when(parent.getFleetData()).thenReturn(fleetData);
        when(module.getFleetData()).thenReturn(fleetData);

        assertFalse(RefitModuleTabs.isModuleTab(parent));
        assertTrue(RefitModuleTabs.isModuleTab(module));
    }

    @Test
    void aMemberWithoutFleetDataOrNoMemberIsNotAModuleTab() {
        assertFalse(RefitModuleTabs.isModuleTab(mock(FleetMemberAPI.class)));
        assertFalse(RefitModuleTabs.isModuleTab(null));
    }

    @Test
    void aModuleTabShowingADockedShipTakesThatShipsIdSoItsTreeOpens() {
        FleetMemberAPI moduleTab = moduleTab();
        ShipVariantAPI dockedVariant = taggedVariant("docked-core");

        try (MockedStatic<ShipSkillDataManager> records = Mockito.mockStatic(ShipSkillDataManager.class)) {
            records.when(() -> ShipSkillDataManager.find("docked-core")).thenReturn(new ShipSkillData());

            assertTrue(RefitModuleTabs.showsDockedShip(moduleTab, dockedVariant));
            RefitModuleTabs.adoptDockedShip(moduleTab, dockedVariant);
        }

        verify(moduleTab).setId("docked-core");
    }

    @Test
    void aPlainModuleATagWithoutATreeOrAShipInTheFleetKeepsItsId() {
        FleetMemberAPI plainModule = moduleTab();
        FleetMemberAPI untreedModule = moduleTab();
        FleetMemberAPI fleetMember = mock(FleetMemberAPI.class);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(fleetMember));
        when(fleetMember.getFleetData()).thenReturn(fleetData);
        ShipVariantAPI plainVariant = mock(ShipVariantAPI.class);
        when(plainVariant.getTags()).thenReturn(List.of());

        try (MockedStatic<ShipSkillDataManager> records = Mockito.mockStatic(ShipSkillDataManager.class)) {
            records.when(() -> ShipSkillDataManager.find("docked-core")).thenReturn(new ShipSkillData());

            assertFalse(RefitModuleTabs.showsDockedShip(plainModule, plainVariant));
            assertFalse(RefitModuleTabs.showsDockedShip(untreedModule, taggedVariant("sold")));
            assertFalse(RefitModuleTabs.showsDockedShip(fleetMember, taggedVariant("docked-core")));
            RefitModuleTabs.adoptDockedShip(plainModule, plainVariant);
            RefitModuleTabs.adoptDockedShip(untreedModule, taggedVariant("sold"));
            RefitModuleTabs.adoptDockedShip(fleetMember, taggedVariant("docked-core"));
        }

        verify(plainModule, never()).setId(anyString());
        verify(untreedModule, never()).setId(anyString());
        verify(fleetMember, never()).setId(anyString());
    }

    private static FleetMemberAPI moduleTab() {
        FleetMemberAPI moduleTab = mock(FleetMemberAPI.class);
        when(moduleTab.getFleetData()).thenReturn(mock(FleetDataAPI.class));
        return moduleTab;
    }

    private static ShipVariantAPI taggedVariant(String shipId) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of(ShipRecords.SHIP_TAG_PREFIX + shipId));
        return variant;
    }
}
