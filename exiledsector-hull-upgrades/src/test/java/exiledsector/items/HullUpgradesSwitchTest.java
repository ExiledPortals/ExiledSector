package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledlib.MockSector;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HullUpgradesSwitchTest {

    private MockSector mockSector;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private FleetDataAPI fleetData;
    private FleetMemberAPI firstMember;
    private FleetMemberAPI secondMember;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        CampaignFleetAPI playerFleet = mockSector.playerFleet();
        fleetData = mock(FleetDataAPI.class);
        firstMember = mock(FleetMemberAPI.class);
        secondMember = mock(FleetMemberAPI.class);
        when(playerFleet.getFleetData()).thenReturn(fleetData);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(firstMember, secondMember));

        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getInt(anyString(), anyString())).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        lunaSettingsMock.close();
        HullUpgradesSetting.setEnabledForTests(HullUpgradesSetting.DEFAULT_ENABLED);
        HullUpgradesSetting.setUnlockLevelForTests(HullUpgradesSetting.DEFAULT_UNLOCK_LEVEL);
    }

    private void switchTo(Boolean enabled) {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), eq(HullUpgradesSetting.ENABLED_FIELD_ID))).thenReturn(enabled);
        HullUpgradesSetting.reloadSettings();
    }

    @Test
    void switchingOffMarksEveryPlayerShipForAStatUpdate() {
        HullUpgradesSetting.setEnabledForTests(true);

        switchTo(false);

        assertFalse(HullUpgradesSetting.enabled());
        verify(firstMember).setStatUpdateNeeded(true);
        verify(secondMember).setStatUpdateNeeded(true);
        verify(fleetData).setSyncNeeded();
    }

    @Test
    void switchingBackOnMarksEveryPlayerShipForAStatUpdate() {
        HullUpgradesSetting.setEnabledForTests(false);

        switchTo(true);

        assertTrue(HullUpgradesSetting.enabled());
        verify(firstMember).setStatUpdateNeeded(true);
        verify(secondMember).setStatUpdateNeeded(true);
        verify(fleetData).setSyncNeeded();
    }

    @Test
    void reloadingTheSameValueLeavesTheFleetAlone() {
        HullUpgradesSetting.setEnabledForTests(true);

        switchTo(true);

        verify(firstMember, never()).setStatUpdateNeeded(anyBoolean());
        verify(fleetData, never()).setSyncNeeded();
    }

    @Test
    void changingOnlyTheUnlockLevelLeavesTheFleetAlone() {
        HullUpgradesSetting.setEnabledForTests(true);
        lunaSettingsMock.when(() -> LunaSettings.getInt(anyString(), eq(HullUpgradesSetting.UNLOCK_LEVEL_FIELD_ID))).thenReturn(5);

        switchTo(null);

        assertEquals(5, HullUpgradesSetting.unlockLevel());
        verify(firstMember, never()).setStatUpdateNeeded(anyBoolean());
        verify(fleetData, never()).setSyncNeeded();
    }

    @Test
    void switchingWithoutASectorOnlyUpdatesTheSetting() {
        mockSector.global.when(Global::getSector).thenReturn(null);
        HullUpgradesSetting.setEnabledForTests(true);

        assertDoesNotThrow(() -> switchTo(false));

        assertFalse(HullUpgradesSetting.enabled());
        verify(fleetData, never()).setSyncNeeded();
    }
}
