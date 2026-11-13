package exiledsector.charts.officer.reward;

import exiledsector.charts.atlas.AtlasLine;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.List;

public final class OfficerRewardText {

    private static final String KEY_PREFIX = "chart.officer.reward.";

    private OfficerRewardText() {
    }

    public static AtlasLine carriedLine(List<CarriedItem> carried) {
        if (carried == null || carried.isEmpty()) {
            return null;
        }
        List<String> groups = new ArrayList<>();
        RewardGroup.entryCounts(carried).forEach((group, count) -> groups.add(groupCount(group, count)));
        return new AtlasLine(Translation.msg(KEY_PREFIX + "carries").arg("items", joined(groups)).text(), AtlasLine.Tone.MARKER);
    }

    public static String groupName(RewardGroup group) {
        return Translation.text(group.labelKey());
    }

    public static String groupName(CarriedItem item) {
        return groupName(RewardGroup.of(item));
    }

    public static String groupCount(RewardGroup group, int count) {
        return Translation.msg(KEY_PREFIX + "groupCount").arg("count", count).arg("group", groupName(group)).text();
    }

    static String joined(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        String rest = String.join(Translation.text(KEY_PREFIX + "sep"), names.subList(0, names.size() - 1));
        return Translation.msg(KEY_PREFIX + "and").arg("rest", rest).arg("last", names.get(names.size() - 1)).text();
    }
}
