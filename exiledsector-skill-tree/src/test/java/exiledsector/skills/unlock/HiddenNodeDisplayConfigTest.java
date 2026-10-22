package exiledsector.skills.unlock;

import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiddenNodeDisplayConfigTest {

    private MockedStatic<LunaSettings> lunaSettingsMock;

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
    }

    @Test
    void defaultsToNotShowingHiddenNodesWhenNoSettingIsConfigured() {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_showHiddenNodesByDefault")).thenReturn(null);

        assertFalse(HiddenNodeDisplayConfig.showHiddenNodesByDefault());
    }

    @Test
    void usesTheConfiguredLunaLibSettingWhenPresent() {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_showHiddenNodesByDefault")).thenReturn(true);

        assertTrue(HiddenNodeDisplayConfig.showHiddenNodesByDefault());
    }
}
