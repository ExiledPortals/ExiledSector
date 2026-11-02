package exiledsector.items;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class ModifierExclusivity {

    private ModifierExclusivity() {
    }

    public static boolean clashes(String modifierId, Collection<String> presentIds) {
        Set<String> groups = ItemModifiers.exclusiveGroups(modifierId);
        if (groups.isEmpty()) {
            return false;
        }
        for (String presentId : presentIds) {
            if (!presentId.equals(modifierId) && !Collections.disjoint(groups, ItemModifiers.exclusiveGroups(presentId))) {
                return true;
            }
        }
        return false;
    }

    public static boolean clashesWithRolled(String modifierId, List<RolledModifier> rolledModifiers) {
        return clashes(modifierId, rolledModifiers.stream().map(RolledModifier::modifierId).toList());
    }
}
