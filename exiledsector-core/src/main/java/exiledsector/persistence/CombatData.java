package exiledsector.persistence;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;

import java.util.Map;
import java.util.function.Supplier;

public final class CombatData {

    private CombatData() {
    }

    public static <T> T getOrCreate(CombatEngineAPI engine, String key, Class<? super T> storedType, Supplier<T> creator) {
        Map<String, Object> customData = engine.getCustomData();
        Object storedValue = customData.get(key);
        if (storedType.isInstance(storedValue)) {
            return stored(storedValue);
        }
        T created = creator.get();
        customData.put(key, created);
        return created;
    }

    public static <T extends EveryFrameCombatPlugin> T getOrCreatePlugin(CombatEngineAPI engine, String key, Class<T> pluginType, Supplier<T> creator) {
        Map<String, Object> customData = engine.getCustomData();
        Object storedValue = customData.get(key);
        if (pluginType.isInstance(storedValue)) {
            return pluginType.cast(storedValue);
        }
        T created = creator.get();
        customData.put(key, created);
        engine.addPlugin(created);
        return created;
    }

    // custom data is a raw Object map; each key is only ever written with the one type its owner reads back
    @SuppressWarnings("unchecked")
    private static <T> T stored(Object storedValue) {
        return (T) storedValue;
    }
}
