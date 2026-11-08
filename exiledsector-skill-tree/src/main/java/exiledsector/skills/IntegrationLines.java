package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class IntegrationLines {

    private static final Pattern NUMBER = Pattern.compile("[+-]?\\d+(?:[.,]\\d+)*");

    public record State(boolean integrated, int nextCost, boolean showsHint) {

        public static State of(ShipSkillData data, SkillNode node, boolean integrationAvailable) {
            boolean integrated = data.isIntegrated(node.getId());
            return new State(integrated, IntegratedNodes.nextCost(data), integrationAvailable && !data.isNpcBuild() && data.isAllocated(node.getId()));
        }
    }

    private IntegrationLines() {
    }

    public static NodeDescription describe(SkillType type, HullSize hullSize, State state) {
        if (!type.isIntegrable()) {
            return SkillNode.describeMechanics(type, hullSize);
        }
        return state.integrated() ? integrated(type, hullSize) : withHint(type, hullSize, state);
    }

    public static List<DescriptionLine> preview(SkillType type, HullSize hullSize, State state) {
        if (!type.isIntegrable() || state.integrated()) {
            return List.of();
        }
        List<DescriptionLine> lines = new ArrayList<>();
        lines.add(muted(Translation.msg("node.integration.preview").count(state.nextCost()).text()));
        List<DescriptionLine> integratedEffects = SkillNode.describeEffects(type, hullSize, true);
        for (DescriptionLine line : integratedEffects) {
            lines.add(muted(line.plain()));
        }
        lines.addAll(removedLines(SkillNode.describeEffects(type, hullSize, false), integratedEffects));
        return lines;
    }

    static NodeDescription integrated(SkillType type, HullSize hullSize) {
        NodeDescription integratedDescription = SkillNode.describeMechanics(type, hullSize, true);
        List<DescriptionLine> normalEffects = SkillNode.describeEffects(type, hullSize, false);
        Set<String> normalLines = new HashSet<>();
        normalEffects.forEach(line -> normalLines.add(line.plain()));
        List<DescriptionLine> effects = new ArrayList<>();
        effects.add(new DescriptionLine(Translation.styled("node.integration.integrated"), false));
        for (DescriptionLine line : integratedDescription.effects()) {
            effects.add(normalLines.contains(line.plain()) ? line : new DescriptionLine(line.text().withUnstyledAs(Style.BUILT_IN), line.lowerIsBetter()));
        }
        effects.addAll(removedLines(normalEffects, integratedDescription.effects()));
        return new NodeDescription(effects, integratedDescription.details());
    }

    private static List<DescriptionLine> removedLines(List<DescriptionLine> normalEffects, List<DescriptionLine> integratedEffects) {
        int rewordedLineCount = linesWithoutCounterpart(integratedEffects, normalEffects).size();
        return linesWithoutCounterpart(normalEffects, integratedEffects).stream()
                .skip(rewordedLineCount)
                .map(IntegrationLines::removedLine)
                .toList();
    }

    private static List<DescriptionLine> linesWithoutCounterpart(List<DescriptionLine> lines, List<DescriptionLine> otherLines) {
        Set<String> otherWordings = new HashSet<>();
        otherLines.forEach(line -> otherWordings.add(wordingWithoutValues(line)));
        return lines.stream().filter(line -> !otherWordings.contains(wordingWithoutValues(line))).toList();
    }

    private static DescriptionLine removedLine(DescriptionLine normalLine) {
        StyledText removedText = Translation.msg("node.integration.removes").arg("line", normalLine.plain()).styled();
        return new DescriptionLine(removedText.withUnstyledAs(Style.BUILT_IN), false);
    }

    private static String wordingWithoutValues(DescriptionLine line) {
        return NUMBER.matcher(line.plain()).replaceAll("#");
    }

    static NodeDescription withHint(SkillType type, HullSize hullSize, State state) {
        NodeDescription normal = SkillNode.describeMechanics(type, hullSize, false);
        List<DescriptionLine> effects = new ArrayList<>(normal.effects());
        if (state.showsHint()) {
            effects.add(new DescriptionLine(Translation.styled("node.integration.hint"), false));
        }
        return new NodeDescription(effects, normal.details());
    }

    private static DescriptionLine muted(String plain) {
        return new DescriptionLine(StyledText.styled(plain, Style.MUTED), false);
    }
}
