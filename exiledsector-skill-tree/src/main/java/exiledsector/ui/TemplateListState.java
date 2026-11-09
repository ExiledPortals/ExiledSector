package exiledsector.ui;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateFilter;

import java.util.Collection;
import java.util.List;
import java.util.Set;

final class TemplateListState {

    private final Set<HullSize> selectedHullSizes;
    private Collection<SkillTreeTemplate> allTemplates;
    private List<SkillTreeTemplate> shownTemplates;
    private int scrollOffset;

    TemplateListState(Collection<SkillTreeTemplate> templates, HullSize currentHullSize) {
        this.selectedHullSizes = TemplateFilter.defaultFilter(currentHullSize);
        setTemplates(templates);
    }

    void setTemplates(Collection<SkillTreeTemplate> templates) {
        this.allTemplates = templates;
        refilter();
    }

    void toggle(HullSize hullSize) {
        if (!selectedHullSizes.remove(hullSize)) {
            selectedHullSizes.add(hullSize);
        }
        refilter();
    }

    boolean isSelected(HullSize hullSize) {
        return selectedHullSizes.contains(hullSize);
    }

    boolean hasAny() {
        return !allTemplates.isEmpty();
    }

    List<SkillTreeTemplate> shown() {
        return shownTemplates;
    }

    int scrollOffset() {
        return scrollOffset;
    }

    void scroll(int rows, int visibleRows) {
        scrollOffset = clamp(scrollOffset + rows, visibleRows);
    }

    List<SkillTreeTemplate> window(int visibleRows) {
        scrollOffset = clamp(scrollOffset, visibleRows);
        return shownTemplates.subList(scrollOffset, Math.min(shownTemplates.size(), scrollOffset + Math.max(0, visibleRows)));
    }

    private void refilter() {
        shownTemplates = TemplateFilter.matching(allTemplates, selectedHullSizes);
    }

    private int clamp(int offset, int visibleRows) {
        int max = Math.max(0, shownTemplates.size() - Math.max(0, visibleRows));
        return Math.max(0, Math.min(offset, max));
    }
}
