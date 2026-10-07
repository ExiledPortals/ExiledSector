package exiledsector.skills.template;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class TemplateFilter {

    public static final List<HullSize> FILTERABLE = List.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP);

    private static final Comparator<SkillTreeTemplate> BY_NAME = Comparator
            .comparing(SkillTreeTemplate::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(SkillTreeTemplate::id);

    private TemplateFilter() {
    }

    public static Set<HullSize> defaultFilter(HullSize currentHullSize) {
        return isFilterable(currentHullSize) ? EnumSet.of(currentHullSize) : EnumSet.copyOf(FILTERABLE);
    }

    public static List<SkillTreeTemplate> matching(Collection<SkillTreeTemplate> templates, String rootNodeId, Set<HullSize> hullSizes) {
        List<SkillTreeTemplate> matches = new ArrayList<>();
        for (SkillTreeTemplate template : templates) {
            if (template.rootNodeId().equals(rootNodeId) && shownFor(template.hullSize(), hullSizes)) {
                matches.add(template);
            }
        }
        matches.sort(BY_NAME);
        return matches;
    }

    private static boolean shownFor(HullSize hullSize, Set<HullSize> hullSizes) {
        return !isFilterable(hullSize) || hullSizes.contains(hullSize);
    }

    public static boolean isFilterable(HullSize hullSize) {
        return hullSize != null && FILTERABLE.contains(hullSize);
    }
}
