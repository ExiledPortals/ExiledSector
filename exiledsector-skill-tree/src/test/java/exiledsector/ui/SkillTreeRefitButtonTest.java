package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipRecords;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import lunalib.lunaRefit.BaseRefitButton;
import lunalib.lunaRefit.LunaRefitManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillTreeRefitButtonTest {

    private final SkillTreeRefitButton button = new SkillTreeRefitButton();

    @AfterEach
    void tearDown() {
        BaseRefitButton registered = LunaRefitManager.getFirstButtonOfClass(SkillTreeRefitButton.class);
        if (registered != null) {
            LunaRefitManager.INSTANCE.removeButton(registered);
        }
    }

    @Test
    void buttonIsNamedSkillTree() {
        assertEquals("Skill Tree", button.getButtonName(null, null));
    }

    @Test
    void usesTheVanillaSkillsCodexIcon() {
        assertEquals("graphics/icons/skills.png", button.getIconName(null, null));
    }

    @Test
    void hasPanelSoClickingOpensTheOverlay() {
        assertTrue(button.hasPanel(null, null, null));
    }

    @Test
    void hasNoPanelOnAModuleTab() {
        FleetMemberAPI parent = mock(FleetMemberAPI.class);
        FleetMemberAPI module = mock(FleetMemberAPI.class);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(parent));
        when(parent.getFleetData()).thenReturn(fleetData);
        when(module.getFleetData()).thenReturn(fleetData);

        assertTrue(button.hasPanel(parent, null, null));
        assertFalse(button.hasPanel(module, null, null));
    }

    @Test
    void opensOnTheModuleTabOfAShipDockedWithATreeOfItsOwn() {
        FleetMemberAPI parent = mock(FleetMemberAPI.class);
        FleetMemberAPI module = mock(FleetMemberAPI.class);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(parent));
        when(module.getFleetData()).thenReturn(fleetData);
        ShipVariantAPI dockedVariant = mock(ShipVariantAPI.class);
        when(dockedVariant.getTags()).thenReturn(List.of(ShipRecords.SHIP_TAG_PREFIX + "docked-core"));

        try (MockedStatic<ShipSkillDataManager> records = Mockito.mockStatic(ShipSkillDataManager.class)) {
            records.when(() -> ShipSkillDataManager.find("docked-core")).thenReturn(new ShipSkillData());

            assertTrue(button.hasPanel(module, dockedVariant, null));
        }
    }

    @Test
    void addButtonRegistersAnInstanceWithLunaRefitManager() {
        SkillTreeRefitButton.addButton();

        assertTrue(LunaRefitManager.hasButtonOfClass(SkillTreeRefitButton.class));
    }

    @Test
    void panelWidthIsFullScreenWidth() {
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getScreenWidth()).thenReturn(1920f);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSettings).thenReturn(settings);

            assertEquals(1920f, button.getPanelWidth(null, null));
        }
    }

    @Test
    void panelHeightIsFullScreenHeight() {
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getScreenHeight()).thenReturn(1080f);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSettings).thenReturn(settings);

            assertEquals(1080f, button.getPanelHeight(null, null));
        }
    }
}
