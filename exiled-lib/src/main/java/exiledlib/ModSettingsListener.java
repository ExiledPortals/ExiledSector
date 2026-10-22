package exiledlib;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

import java.util.function.Supplier;

public abstract class ModSettingsListener implements LunaSettingsListener {

    @Override
    public final void settingsChanged(String modId) {
        if (HostMod.id().equals(modId)) {
            reload();
        }
    }

    protected abstract void reload();

    public static <T extends ModSettingsListener> void ensureRegistered(Class<T> listenerType, Supplier<T> listenerFactory) {
        if (!LunaSettings.hasSettingsListenerOfClass(listenerType)) {
            LunaSettings.addSettingsListener(listenerFactory.get());
        }
    }
}
