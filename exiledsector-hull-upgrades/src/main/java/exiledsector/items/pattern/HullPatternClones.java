package exiledsector.items.pattern;

import exiledsector.items.Item;
import exiledsector.items.ItemDefinition;
import exiledsector.items.hull.ShipSection;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;

import java.util.List;

public final class HullPatternClones {

    public record Source(HullPattern pattern, ShipSkillData patternData, List<ShipSections.UnlockedSection> patternSections) {

        public Item itemFor(ShipSection section) {
            if (!pattern.isSelected(section)) {
                return null;
            }
            for (ShipSections.UnlockedSection patternSection : patternSections) {
                if (patternSection.section() == section) {
                    return patternSection.appliesItem() && !isUnique(patternSection.item()) ? patternSection.item() : null;
                }
            }
            return null;
        }

        public Object stamp() {
            return List.of(HullPatterns.revision(), patternData.revision(), patternSections);
        }
    }

    private HullPatternClones() {
    }

    public static Source sourceFor(ShipSkillData followerData) {
        if (followerData == null) {
            return null;
        }
        HullPatterns patterns = HullPatterns.get();
        if (patterns.isEmpty()) {
            return null;
        }
        for (HullPattern pattern : patterns.all()) {
            for (String followerId : pattern.followerIds()) {
                if (ShipSkillDataManager.find(followerId) == followerData) {
                    return activeSource(pattern);
                }
            }
        }
        return null;
    }

    public static Object stampFor(ShipSkillData followerData) {
        Source source = sourceFor(followerData);
        return source == null ? null : source.stamp();
    }

    static Source activeSource(HullPattern pattern) {
        ShipSkillData patternData = ShipSkillDataManager.find(pattern.patternShipId());
        List<ShipSections.UnlockedSection> patternSections = patternData == null ? null : HullPatternActivity.activePatternSections(pattern);
        return patternSections == null ? null : new Source(pattern, patternData, patternSections);
    }

    public static boolean isUnique(Item item) {
        ItemDefinition definition = item == null ? null : item.definition();
        return definition != null && definition.unique();
    }
}
