package exiledsector;

import lunalib.lunaSettings.LunaSettings;

public final class ModSettings {

    private ModSettings() {
    }

    public static int intOr(String fieldId, int defaultValue) {
        Integer value = LunaSettings.getInt(HostMod.id(), fieldId);
        return value != null ? value : defaultValue;
    }

    public static float floatOr(String fieldId, float defaultValue) {
        Float value = LunaSettings.getFloat(HostMod.id(), fieldId);
        return value != null ? value : defaultValue;
    }

    public static String stringOr(String fieldId, String defaultValue) {
        String value = LunaSettings.getString(HostMod.id(), fieldId);
        return value != null ? value : defaultValue;
    }

    public static boolean booleanOr(String fieldId, boolean defaultValue) {
        return otherModBooleanOr(HostMod.id(), fieldId, defaultValue);
    }

    public static boolean otherModBooleanOr(String modId, String fieldId, boolean defaultValue) {
        Boolean value = LunaSettings.getBoolean(modId, fieldId);
        return value != null ? value : defaultValue;
    }
}
