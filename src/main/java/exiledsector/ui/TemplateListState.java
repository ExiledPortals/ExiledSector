package exiledsector.ui;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateFilter;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class TemplateListState {

    private final String rootNodeId;
    private final Set<HullSize> hullSizes;
    private Collection<SkillTreeTemplate> templates;
    private List<SkillTreeTemplate> shown;
    private boolean anyForRoot;
    private int scrollOffset;

    TemplateListState(Collection<SkillTreeTemplate> templates, String rootNodeId, HullSize currentHullSize) {
        this.rootNodeId = rootNodeId;
        this.hullSizes = TemplateFilter.defaultFilter(currentHullSize);
        setTemplates(templates);
    }

    void setTemplates(Collection<SkillTreeTemplate> templates) {
        this.templates = templates;
        this.anyForRoot = !TemplateFilter.matching(templates, rootNodeId, EnumSet.copyOf(TemplateFilter.FILTERABLE)).isEmpty();
        refilter();
    }

    void toggle(HullSize hullSize) {
        if (!hullSizes.remove(hullSize)) {
            hullSizes.add(hullSize);
        }
        refilter();
    }

    boolean isSelected(HullSize hullSize) {
        return hullSizes.contains(hullSize);
    }

    boolean hasAnyForRoot() {
        return anyForRoot;
    }

    List<SkillTreeTemplate> shown() {
        return shown;
    }

    int scrollOffset() {
        return scrollOffset;
    }

    void scroll(int rows, int visibleRows) {
        scrollOffset = clamp(scrollOffset + rows, visibleRows);
    }

    List<SkillTreeTemplate> window(int visibleRows) {
        scrollOffset = clamp(scrollOffset, visibleRows);
        return shown.subList(scrollOffset, Math.min(shown.size(), scrollOffset + Math.max(0, visibleRows)));
    }

    private void refilter() {
        shown = TemplateFilter.matching(templates, rootNodeId, hullSizes);
    }

    private int clamp(int offset, int visibleRows) {
        int max = Math.max(0, shown.size() - Math.max(0, visibleRows));
        return Math.max(0, Math.min(offset, max));
    }
}
