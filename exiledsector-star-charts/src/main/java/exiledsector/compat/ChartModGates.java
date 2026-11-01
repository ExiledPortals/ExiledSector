package exiledsector.compat;

import java.util.Map;
import java.util.function.BooleanSupplier;

public final class ChartModGates {

    static final Map<String, BooleanSupplier> MOD_SETTING_GATES = Map.of(IndustrialEvolutionCompat.MOD_ID, IndustrialEvolutionCompat::derelictStationsEnabled);

    private ChartModGates() {
    }

    public static boolean met(String modId) {
        if (modId == null || modId.isEmpty()) {
            return true;
        }
        if (!CompatTarget.isModEnabled(modId)) {
            return false;
        }
        BooleanSupplier settingGate = MOD_SETTING_GATES.get(modId);
        return settingGate == null || settingGate.getAsBoolean();
    }
}
