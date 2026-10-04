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
        List<String> names = new ArrayList<>();
        for (String theme : themes) {
            names.add(Translation.text("theme." + theme));
        }
        return String.join(" / ", names);
    }

    static List<String> mainThemes(ShipSkillData data) {
        if (data == null) {
            return List.of();
        }
        Map<String, Integer> counts = new HashMap<>();
        for (String nodeId : data.getAllocatedNodeIds()) {
            SkillNode node = SkillTree.get(nodeId);
            if (node == null || node.getType().isOptional() || node.getType().getTier() == SkillTier.ROOT) {
                continue;
            }
            for (String tag : node.getType().getTags()) {
                if (SkillTags.THEME.contains(tag)) {
                    counts.merge(tag, 1, Integer::sum);
                }
            }
        }
        List<String> ranked = new ArrayList<>(counts.keySet());
        ranked.sort((a, b) -> counts.get(a).equals(counts.get(b))
                ? Integer.compare(SkillTags.THEME.indexOf(a), SkillTags.THEME.indexOf(b))
                : Integer.compare(counts.get(b), counts.get(a)));
        return List.copyOf(ranked.subList(0, Math.min(THEMES_SHOWN, ranked.size())));
    }
}
