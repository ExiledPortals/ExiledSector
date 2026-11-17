package exiledsector.skills;

import java.util.Collection;
import java.util.Map;

public final class OptionRenames {

    private OptionRenames() {
    }

    public static boolean apply(ShipSkillData data) {
        if (data == null || data.choiceSelections == null) {
            return false;
        }
        boolean changed = false;
        for (Map.Entry<String, String> selection : data.choiceSelections.entrySet()) {
            String currentId = NodeReplacements.currentTypeId(selection.getValue());
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

    public static int applyAll(Collection<ShipSkillData> ships) {
        int renamed = 0;
        for (ShipSkillData data : ships) {
            if (apply(data)) {
                renamed++;
            }
        }
        return renamed;
    }
}
