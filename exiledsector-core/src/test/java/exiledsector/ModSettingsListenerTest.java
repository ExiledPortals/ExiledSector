package exiledsector;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModSettingsListenerTest {

    private final AtomicInteger reloads = new AtomicInteger();

    @AfterEach
    void forget() {
        ModSettingsListener.forgetForTests(ModSettingsListenerTest.class);
    }

    @Test
    void onlyThisModsSettingChangesRunTheReloads() {
        try (MockedStatic<LunaSettings> luna = Mockito.mockStatic(LunaSettings.class)) {
            ModSettingsListener.onReload(ModSettingsListenerTest.class, reloads::incrementAndGet);
        }
        ModSettingsListener listener = new ModSettingsListener();

        listener.settingsChanged("someOtherMod");
        listener.settingsChanged(ExiledSector.MOD_ID);

        assertEquals(1, reloads.get());
    }

    @Test
    void anOwnerRegisteredTwiceReloadsOnceAndOneListenerIsAdded() {
        try (MockedStatic<LunaSettings> luna = Mockito.mockStatic(LunaSettings.class)) {
            luna.when(() -> LunaSettings.hasSettingsListenerOfClass(ModSettingsListener.class)).thenReturn(false, true);

            ModSettingsListener.onReload(ModSettingsListenerTest.class, reloads::incrementAndGet);
            ModSettingsListener.onReload(ModSettingsListenerTest.class, reloads::incrementAndGet);

            luna.verify(() -> LunaSettings.addSettingsListener(ArgumentMatchers.any(LunaSettingsListener.class)), Mockito.times(1));
        }
        new ModSettingsListener().settingsChanged(ExiledSector.MOD_ID);

        assertEquals(1, reloads.get());
    }
}
