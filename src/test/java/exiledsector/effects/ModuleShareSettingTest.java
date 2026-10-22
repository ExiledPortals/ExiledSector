package exiledsector.effects;

import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleShareSettingTest {

    private static float shareFor(Integer setting) {
        try (MockedStatic<LunaSettings> lunaSettings = Mockito.mockStatic(LunaSettings.class)) {
            lunaSettings.when(() -> LunaSettings.getInt("exiledSector", ModuleShareSetting.FIELD_ID)).thenReturn(setting);
            return ModuleShareSetting.share();
        }
    }

    @Test
    void defaultsToHalfAndClampsToZeroThroughOneHundredPercent() {
        assertEquals(0.5f, shareFor(null), 1e-6f);
        assertEquals(0.3f, shareFor(30), 1e-6f);
        assertEquals(1f, shareFor(150), 1e-6f);
        assertEquals(0f, shareFor(-5), 1e-6f);
    }
}
