package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.skilleffect.SkillEffect;

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

    public static List<StyledText> tooltipLines(Map<String, Integer> scars, Map<String, List<String>> dModsByScar, HullFacts hull) {
        if (scars == null || scars.isEmpty()) {
            return List.of();
        }
        return I18n.forGameText(() -> {
            List<StyledText> lines = new ArrayList<>();
            scars.forEach((scarId, stacks) -> {
                if (stacks != null && stacks > 0) {
                    lines.add(line(scarId, stacks, dModsByScar.getOrDefault(scarId, List.of()), hull));
                }
            });
            return lines;
        });
    }

    private static StyledText line(String scarId, int stacks, List<String> dModIds, HullFacts hull) {
        StyledText title = stacks > 1
                ? Translation.msg("chart.officer.scar.stacked").arg("scar", nameText(scarId)).arg("stacks", stacks).styled()
                : StyledText.of(nameText(scarId));
        List<StyledText> effects = new ArrayList<>();
        ScarRow row = ScarTable.row(scarId);
        if (row != null) {
            for (ScarRow.Effect effect : row.fittingEffects(hull)) {
                effects.add(listItem(playerView(effect.effect(), effect.magnitudePerStack() * stacks)));
            }
        }
        effects.add(Translation.msg("chart.officer.scar.levels").arg("levels", ScarLevels.extraLevels(Map.of(scarId, stacks))).styled());
        for (String dModId : dModIds) {
            effects.add(Translation.msg("chart.officer.scar.dmod").arg("dmod", dModName(dModId)).styled());
        }
        return Translation.msg("chart.officer.scar.effects").arg("scar", title).arg("effects", Translation.list(effects)).styled();
    }

    public static String dModName(String dModId) {
        SettingsAPI settings = Global.getSettings();
        HullModSpecAPI spec = settings == null ? null : settings.getHullModSpec(dModId);
        return spec == null ? dModId : spec.getDisplayName();
    }

    static StyledText listItem(StyledText sentence) {
        String plain = sentence.plain();
        int end = plain.endsWith(".") || plain.endsWith("。") ? plain.length() - 1 : plain.length();
        String item = startsWithCapitalisedWord(plain) ? Character.toLowerCase(plain.charAt(0)) + plain.substring(1, end) : plain.substring(0, end);
        List<StyledText.Span> spans = new ArrayList<>();
        for (StyledText.Span span : sentence.spans()) {
            if (span.start() < end) {
                spans.add(new StyledText.Span(span.start(), Math.min(span.end(), end), span.style()));
            }
        }
        return new StyledText(item, spans);
    }

    private static boolean startsWithCapitalisedWord(String plain) {
        return plain.length() > 1 && Character.isUpperCase(plain.charAt(0)) && Character.isLowerCase(plain.charAt(1));
    }

    static StyledText playerView(SkillEffect effect, float magnitude) {
        StyledText shipView = new DescriptionLine(effect.description(magnitude), effect.invertsDescriptionColours()).display();
        return shipView.inverted();
    }
}
