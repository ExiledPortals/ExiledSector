package exiledsector.skills;

import java.util.Collection;
import java.util.Map;

public final class AffinityOptions {

    static final Map<String, String> RETIRED_OPTIONS = Map.of(
            "hull", "low_tech_affinity",
            "flux_capacity", "high_tech_affinity",
            "flux_dissipation", "high_tech_affinity",
            "fuel_crew_cargo", "midline_affinity");

    private AffinityOptions() {
    }

    public static String currentOptionId(String optionTypeId) {
        return optionTypeId == null ? null : NodeReplacements.currentTypeId(RETIRED_OPTIONS.getOrDefault(optionTypeId, optionTypeId));
    }

    public static boolean migrate(ShipSkillData data) {
        if (data == null || data.optionalSelections == null) {
            return false;
        }
        boolean changed = false;
        for (Map.Entry<String, String> selection : data.optionalSelections.entrySet()) {
            String currentId = currentOptionId(selection.getValue());
            if (currentId != null && !currentId.equals(selection.getValue())) {
                selection.setValue(currentId);
                changed = true;
            }
        }
        if (changed) {
            data.revision++;
        }
        return changed;
    }

    public static int migrateAll(Collection<ShipSkillData> ships) {
        int migrated = 0;
        for (ShipSkillData data : ships) {
            if (migrate(data)) {
                migrated++;
            }
        }
        return migrated;
    }
}
