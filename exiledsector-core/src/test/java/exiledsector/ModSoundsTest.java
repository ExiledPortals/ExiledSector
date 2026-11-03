package exiledsector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ModSoundsTest {

    private MockedStatic<Global> globalMock;
    private SoundPlayerAPI player;

    @BeforeEach
    void setUp() {
        player = mock(SoundPlayerAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSoundPlayer).thenReturn(player);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        ModSounds.setEnabledForTests(ModSounds.DEFAULT_ENABLED);
    }

    @Test
    void soundsPlayByDefault() {
        Vector2f location = new Vector2f(1f, 2f);
        Vector2f velocity = new Vector2f();

        ModSounds.playUi("ui_button_pressed");
        ModSounds.playAt("fighter_takeoff", 0.5f, location, velocity);

        verify(player).playUISound("ui_button_pressed", 1f, 1f);
        verify(player).playSound("fighter_takeoff", 1f, 0.5f, location, velocity);
        assertEquals("tachyon_lance_emp_impact", ModSounds.orSilent("tachyon_lance_emp_impact"));
    }

    @Test
    void turningSoundsOffSilencesUiWorldAndArcSounds() {
        ModSounds.setEnabledForTests(false);

        ModSounds.playUi("ui_button_pressed");
        ModSounds.playAt("fighter_takeoff", 1f, new Vector2f(), new Vector2f());

        verify(player, never()).playUISound(anyString(), anyFloat(), anyFloat());
        verify(player, never()).playSound(anyString(), anyFloat(), anyFloat(), any(), any());
        assertNull(ModSounds.orSilent("tachyon_lance_emp_impact"));
    }

    @Test
    void nothingPlaysWithoutASoundPlayer() {
        globalMock.when(Global::getSoundPlayer).thenReturn(null);

        ModSounds.playUi("ui_button_pressed");
        ModSounds.playAt("fighter_takeoff", 1f, new Vector2f(), new Vector2f());

        verify(player, never()).playUISound(anyString(), anyFloat(), anyFloat());
    }

    @Test
    void reloadingReadsTheLunaLibSwitchAndFallsBackToOnWhenItIsUnset() {
        try (MockedStatic<LunaSettings> lunaSettingsMock = Mockito.mockStatic(LunaSettings.class)) {
            lunaSettingsMock.when(() -> LunaSettings.getBoolean(ExiledSector.MOD_ID, ModSounds.ENABLED_FIELD_ID)).thenReturn(false);
            ModSounds.reloadSettings();
            assertFalse(ModSounds.enabled());

            lunaSettingsMock.when(() -> LunaSettings.getBoolean(ExiledSector.MOD_ID, ModSounds.ENABLED_FIELD_ID)).thenReturn(null);
            ModSounds.reloadSettings();
            assertTrue(ModSounds.enabled());
        }
    }
}
