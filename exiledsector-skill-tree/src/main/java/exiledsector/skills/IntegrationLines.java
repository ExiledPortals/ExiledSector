package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IntegrationLines {

    private static final Pattern NUMBER = Pattern.compile("[+-]?\\d+(?:[.,]\\d+)*%?");
    private static final Pattern LEADING_VALUE = Pattern.compile("^[+-]?\\d");
    private static final Set<Style> VALUE_STYLES = Set.of(Style.GOOD, Style.BAD);
    private static final Set<Style> HIGHLIGHT_STYLES = Set.of(Style.HIGHLIGHT);

    public enum TemplateMark {
        NONE(null), MARKED("node.integration.marked"), MARKABLE("node.integration.markHint"), UNMARKABLE("node.integration.unmarkHint");

        private final String lineKey;

        TemplateMark(String lineKey) {
            this.lineKey = lineKey;
        }
    }

    public record State(boolean integrated, int nextCost, boolean showsHint, TemplateMark templateMark) {

        public static State of(ShipSkillData data, SkillNode node, boolean integrationAvailable, TemplateMark templateMark) {
            boolean integrated = data.isIntegrated(node.getId());
            return new State(integrated, IntegratedNodes.nextCost(data), integrationAvailable && !data.isNpcBuild() && data.isAllocated(node.getId()),
                    templateMark);
        }
    }

    private enum Change {
        UNCHANGED, NEW_VALUES, REPLACES, ADDS
    }

    private record ChangedLine(Change change, DescriptionLine line) {
    }

    private record Comparison(List<ChangedLine> integratedLines, List<DescriptionLine> droppedLines) {

        static Comparison of(List<DescriptionLine> normalLines, List<DescriptionLine> integratedLines) {
            List<DescriptionLine> unmatched = new ArrayList<>(normalLines);
            Set<Integer> identical = new HashSet<>();
            for (int index = 0; index < integratedLines.size(); index++) {
                if (takeFirst(unmatched, integratedLines.get(index).plain(), DescriptionLine::plain) != null) {
                    identical.add(index);
                }
            }
            Map<Integer, DescriptionLine> counterparts = new HashMap<>();
            for (int index = 0; index < integratedLines.size(); index++) {
                DescriptionLine counterpart = identical.contains(index)
                        ? null : takeFirst(unmatched, wordingWithoutValues(integratedLines.get(index)), IntegrationLines::wordingWithoutValues);
                if (counterpart != null) {
                    counterparts.put(index, counterpart);
                }
            }

            int replacedCount = 0;
            List<ChangedLine> changedLines = new ArrayList<>();
            for (int index = 0; index < integratedLines.size(); index++) {
                DescriptionLine integratedLine = integratedLines.get(index);
                DescriptionLine counterpart = counterparts.get(index);
                if (identical.contains(index)) {
                    changedLines.add(new ChangedLine(Change.UNCHANGED, integratedLine));
                } else if (counterpart != null) {
                    changedLines.add(new ChangedLine(Change.NEW_VALUES, withNewValuesMarked(counterpart, integratedLine)));
                } else if (replacedCount < unmatched.size()) {
                    replacedCount++;
                    changedLines.add(new ChangedLine(Change.REPLACES, storyPointGreen(integratedLine)));
                } else {
                    changedLines.add(new ChangedLine(Change.ADDS, storyPointGreen(integratedLine)));
                }
            }
            List<DescriptionLine> droppedLines = unmatched.stream().skip(replacedCount).toList();
            return new Comparison(changedLines, droppedLines);
        }

        private static DescriptionLine takeFirst(List<DescriptionLine> lines, String key, Function<DescriptionLine, String> keyOf) {
            for (int index = 0; index < lines.size(); index++) {
                if (keyOf.apply(lines.get(index)).equals(key)) {
                    return lines.remove(index);
                }
            }
            return null;
        }
    }

    private IntegrationLines() {
    }

    public static NodeDescription describe(SkillType type, HullSize hullSize, State state) {
        return describe(type, hullSize, state, KnownHullMods::knownByPlayer);
    }

    public static NodeDescription describe(SkillType type, HullSize hullSize, State state, Predicate<String> listsConflictingHullMod) {
        if (!type.isIntegrable()) {
            return SkillNode.describeMechanics(type, hullSize, false, listsConflictingHullMod);
        }
        return state.integrated() ? integrated(type, hullSize, listsConflictingHullMod) : withHint(type, hullSize, state, listsConflictingHullMod);
    }

    public static List<DescriptionLine> preview(SkillType type, HullSize hullSize, State state) {
        if (!type.isIntegrable() || state.integrated()) {
            return List.of();
        }
        List<DescriptionLine> lines = new ArrayList<>();
        lines.add(new DescriptionLine(StyledText.styled(Translation.msg("node.integration.preview").count(state.nextCost()).text(), Style.MUTED), false));
        Comparison comparison = Comparison.of(SkillNode.describeEffects(type, hullSize, false), SkillNode.describeEffects(type, hullSize, true));
        for (ChangedLine changed : comparison.integratedLines()) {
            if (changed.change() == Change.ADDS) {
                lines.add(grantsLine("node.integration.inAddition", changed.line()));
            } else if (changed.change() != Change.UNCHANGED) {
                lines.add(grantsLine("node.integration.instead", changed.line()));
            }
        }
        comparison.droppedLines().forEach(dropped -> lines.add(removedLine(dropped)));
        return lines;
    }

    static NodeDescription integrated(SkillType type, HullSize hullSize) {
        return integrated(type, hullSize, KnownHullMods::knownByPlayer);
    }

    private static NodeDescription integrated(SkillType type, HullSize hullSize, Predicate<String> listsConflictingHullMod) {
        NodeDescription integratedDescription = SkillNode.describeMechanics(type, hullSize, true, listsConflictingHullMod);
        Comparison comparison = Comparison.of(SkillNode.describeEffects(type, hullSize, false), integratedDescription.effects());
        List<DescriptionLine> effects = new ArrayList<>();
        effects.add(new DescriptionLine(Translation.styled("node.integration.integrated"), false));
        comparison.integratedLines().forEach(changed -> effects.add(changed.line()));
        comparison.droppedLines().forEach(dropped -> effects.add(removedLine(dropped)));
        return new NodeDescription(effects, integratedDescription.details());
    }

    private static DescriptionLine withNewValuesMarked(DescriptionLine normalLine, DescriptionLine integratedLine) {
        List<String> normalValues = NUMBER.matcher(normalLine.plain()).results().map(MatchResult::group).toList();
        List<int[]> newValueRanges = new ArrayList<>();
        Matcher integratedValues = NUMBER.matcher(integratedLine.plain());
        for (int valueIndex = 0; integratedValues.find(); valueIndex++) {
            if (valueIndex >= normalValues.size() || !normalValues.get(valueIndex).equals(integratedValues.group())) {
                newValueRanges.add(new int[]{integratedValues.start(), integratedValues.end()});
            }
        }
        return new DescriptionLine(withRangesAs(integratedLine.text(), newValueRanges, Style.BUILT_IN), integratedLine.lowerIsBetter());
    }

    private static StyledText withRangesAs(StyledText text, List<int[]> ranges, Style style) {
        List<int[]> marked = new ArrayList<>();
        for (int[] range : ranges) {
            int start = range[0];
            int end = range[1];
            for (StyledText.Span span : text.spans()) {
                if (overlaps(span.start(), span.end(), start, end)) {
                    start = Math.min(start, span.start());
                    end = Math.max(end, span.end());
                }
            }
            if (!marked.isEmpty() && marked.get(marked.size() - 1)[1] >= start) {
                marked.get(marked.size() - 1)[1] = Math.max(marked.get(marked.size() - 1)[1], end);
            } else {
                marked.add(new int[]{start, end});
            }
        }
        List<StyledText.Span> spans = new ArrayList<>();
        for (StyledText.Span span : text.spans()) {
            if (marked.stream().noneMatch(range -> overlaps(span.start(), span.end(), range[0], range[1]))) {
                spans.add(span);
            }
        }
        marked.forEach(range -> spans.add(new StyledText.Span(range[0], range[1], style)));
        spans.sort(Comparator.comparingInt(StyledText.Span::start));
        return new StyledText(text.plain(), spans);
    }

    private static boolean overlaps(int start, int end, int otherStart, int otherEnd) {
        return start < otherEnd && otherStart < end;
    }

    private static DescriptionLine storyPointGreen(DescriptionLine line) {
        StyledText text = line.text();
        Set<Style> marked = hasSpanIn(text, VALUE_STYLES) ? VALUE_STYLES : HIGHLIGHT_STYLES;
        if (!hasSpanIn(text, marked)) {
            return new DescriptionLine(text.withUnstyledAs(Style.BUILT_IN), false);
        }
        List<StyledText.Span> spans = text.spans().stream()
                .map(span -> marked.contains(span.style()) ? new StyledText.Span(span.start(), span.end(), Style.BUILT_IN) : span)
                .toList();
        return new DescriptionLine(new StyledText(text.plain(), spans), false);
    }

    private static boolean hasSpanIn(StyledText text, Set<Style> styles) {
        return text.spans().stream().anyMatch(span -> styles.contains(span.style()));
    }

    private static DescriptionLine grantsLine(String key, DescriptionLine line) {
        String messageKey = LEADING_VALUE.matcher(line.plain()).find() ? key : key + "Sentence";
        return new DescriptionLine(Translation.msg(messageKey).arg("line", line.text()).styled(), line.lowerIsBetter());
    }

    private static DescriptionLine removedLine(DescriptionLine droppedLine) {
        return grantsLine("node.integration.removes", droppedLine);
    }

    private static String wordingWithoutValues(DescriptionLine line) {
        return NUMBER.matcher(line.plain()).replaceAll("#");
    }

    private static NodeDescription withHint(SkillType type, HullSize hullSize, State state, Predicate<String> listsConflictingHullMod) {
        NodeDescription normal = SkillNode.describeMechanics(type, hullSize, false, listsConflictingHullMod);
        List<DescriptionLine> effects = new ArrayList<>(normal.effects());
        if (state.templateMark().lineKey != null) {
            effects.add(new DescriptionLine(Translation.styled(state.templateMark().lineKey), false));
        }
        if (state.showsHint()) {
            effects.add(new DescriptionLine(Translation.msg("node.integration.hint").count(state.nextCost()).styled(), false));
        }
        return new NodeDescription(effects, normal.details());
    }
}
