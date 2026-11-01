package exiledsector;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModSettingsListener implements LunaSettingsListener {

    private static final Map<Class<?>, Runnable> RELOADS_BY_OWNER = new LinkedHashMap<>();

    ModSettingsListener() {
    }

    @Override
    public void settingsChanged(String modId) {
        if (ExiledSector.MOD_ID.equals(modId)) {
            reloads().forEach(Runnable::run);
        }
    }

    public static void onReload(Class<?> owner, Runnable reload) {
        synchronized (RELOADS_BY_OWNER) {
            RELOADS_BY_OWNER.put(owner, reload);
        }
        if (!LunaSettings.hasSettingsListenerOfClass(ModSettingsListener.class)) {
            LunaSettings.addSettingsListener(new ModSettingsListener());
        }
    }

    private static List<Runnable> reloads() {
        synchronized (RELOADS_BY_OWNER) {
            return List.copyOf(RELOADS_BY_OWNER.values());
        }
    }

    static void forgetForTests(Class<?> owner) {
        synchronized (RELOADS_BY_OWNER) {
            RELOADS_BY_OWNER.remove(owner);
        }
    }
}
