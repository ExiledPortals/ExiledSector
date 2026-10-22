package exiledlib.persistence;

import com.fs.starfarer.api.Global;

import java.util.Map;
import java.util.function.Supplier;

public final class SaveData {

    private SaveData() {
    }

    public static <T> T getOrCreate(String key, Supplier<T> creator) {
        return stored(persistentData().computeIfAbsent(key, missingKey -> creator.get()));
    }

    public static <T> T findOrNull(String key, Class<? super T> storedType) {
        Object storedValue = persistentData().get(key);
        return storedType.isInstance(storedValue) ? stored(storedValue) : null;
    }

    public static void put(String key, Object value) {
        persistentData().put(key, value);
    }

    public static void remove(String key) {
        persistentData().remove(key);
    }

    private static Map<String, Object> persistentData() {
        return Global.getSector().getPersistentData();
    }

    // persistentData is a raw Object map; each key is only ever written with the one type its owner reads back
    @SuppressWarnings("unchecked")
    private static <T> T stored(Object storedValue) {
        return (T) storedValue;
    }
}
