package exiledsector.charts.officer.scar;

import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ScarText {

    static final String NAME_KEY_PREFIX = "chart.officer.scar.name.";

    private ScarText() {
    }

    public static String name(String scarId) {
        return I18n.forGameText(() -> nameText(scarId));
    }

    private static String nameText(String scarId) {
        String key = NAME_KEY_PREFIX + scarId;
        return Translation.has(key) ? Translation.text(key) : scarId;
    }

    public static String epithet(String officerName, Map<String, Integer> scars) {
        String scarId = mostStacked(scars);
        if (scarId == null) {
            return officerName;
        }
        return I18n.forGameText(() -> Translation.msg("chart.officer.scar.epithet")
                .arg("name", officerName == null ? "" : officerName).arg("scar", nameText(scarId)).text());
    }

    static String mostStacked(Map<String, Integer> scars) {
        String best = null;
        int bestStacks = 0;
        if (scars != null) {
            for (Map.Entry<String, Integer> scar : scars.entrySet()) {
                if (scar.getValue() != null && scar.getValue() > bestStacks) {
                    best = scar.getKey();
                    bestStacks = scar.getValue();
                }
            }
        }
        return best;
    }

    public static List<StyledText> tooltipLines(Map<String, Integer> scars, HullFacts hull) {
        if (scars == null || scars.isEmpty()) {
            return List.of();
        }
        return I18n.forGameText(() -> {
            List<StyledText> lines = new ArrayList<>();
            scars.forEach((scarId, stacks) -> {
                if (stacks != null && stacks > 0) {
                    lines.add(line(scarId, stacks, hull));
                }
            });
            return lines;
        });
    }

    private static StyledText line(String scarId, int stacks, HullFacts hull) {
        StyledText title = stacks > 1
                ? Translation.msg("chart.officer.scar.stacked").arg("scar", nameText(scarId)).arg("stacks", stacks).styled()
                : StyledText.of(nameText(scarId));
        List<StyledText> effects = new ArrayList<>();
        ScarRow row = ScarTable.row(scarId);
        if (row != null) {
            for (ScarRow.Effect effect : row.fittingEffects(hull)) {
                effects.add(effect.effect().description(effect.magnitudePerStack() * stacks));
            }
        }
        if (effects.isEmpty()) {
            return title;
        }
        return Translation.msg("chart.officer.scar.effects").arg("scar", title).arg("effects", Translation.list(effects)).styled();
    }
}
