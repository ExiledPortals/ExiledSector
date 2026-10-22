package exiledsector.ui.refit;

import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
}
