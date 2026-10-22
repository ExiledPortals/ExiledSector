package exiledsector;

import exiledlib.ModSettingsListener;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModSettingsListenerTest {

    private static final class CountingListener extends ModSettingsListener {
        private int reloads;

        @Override
        protected void reload() {
            reloads++;
        }
    }

    @Test
    void onlyThisModsSettingChangesReload() {
        CountingListener listener = new CountingListener();

        listener.settingsChanged("someOtherMod");
        listener.settingsChanged(ExiledSector.MOD_ID);

        assertEquals(1, listener.reloads);
    }

    @Test
    void aListenerIsAddedOnlyWhenNoneOfItsClassIsRegisteredYet() {
        try (MockedStatic<LunaSettings> luna = Mockito.mockStatic(LunaSettings.class)) {
            luna.when(() -> LunaSettings.hasSettingsListenerOfClass(CountingListener.class)).thenReturn(false, true);

            ModSettingsListener.ensureRegistered(CountingListener.class, CountingListener::new);
            ModSettingsListener.ensureRegistered(CountingListener.class, CountingListener::new);

            luna.verify(() -> LunaSettings.addSettingsListener(ArgumentMatchers.any(LunaSettingsListener.class)), Mockito.times(1));
        }
    }
}
