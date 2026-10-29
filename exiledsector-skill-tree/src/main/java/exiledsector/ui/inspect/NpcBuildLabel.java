package exiledsector.ui.inspect;

import exiledsector.i18n.Translation;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.tags.SkillTags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class NpcBuildLabel {

    static final int THEMES_SHOWN = 2;

    private NpcBuildLabel() {
    }

    static String name(List<String> themes) {
        List<String> themeNames = new ArrayList<>();
        for (String theme : themes) {
            themeNames.add(Translation.text("theme." + theme));
        }
        return String.join(" / ", themeNames);
    }

    static List<String> mainThemes(ShipSkillData skillData) {
        if (skillData == null) {
            return List.of();
        }
        Map<String, Integer> themeCounts = new HashMap<>();
        for (String nodeId : skillData.getAllocatedNodeIds()) {
            SkillNode node = SkillTree.get(nodeId);
            if (node == null || node.getType().isOptional() || node.getType().getTier() == SkillTier.ROOT) {
                continue;
            }
            for (String tag : node.getType().getTags()) {
                if (SkillTags.THEME.contains(tag)) {
                    themeCounts.merge(tag, 1, Integer::sum);
                }
            }
        }
        List<String> rankedThemes = new ArrayList<>(themeCounts.keySet());
        rankedThemes.sort((a, b) -> themeCounts.get(a).equals(themeCounts.get(b))
                ? Integer.compare(SkillTags.THEME.indexOf(a), SkillTags.THEME.indexOf(b))
                : Integer.compare(themeCounts.get(b), themeCounts.get(a)));
        return List.copyOf(rankedThemes.subList(0, Math.min(THEMES_SHOWN, rankedThemes.size())));
    }
}
