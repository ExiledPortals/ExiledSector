package exiledsector.charts.tree;

import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.atlas.AtlasModifier;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class AtlasTreeEffectGroups {

    public record Group(List<String> anomalyIds, List<AppliedEffect> chartLines, List<AtlasEffects.Line> atlasLines) {

        public Group {
            anomalyIds = List.copyOf(anomalyIds);
            chartLines = List.copyOf(chartLines);
            atlasLines = List.copyOf(atlasLines);
        }

        public boolean everyChart() {
            return anomalyIds.isEmpty();
        }
    }

    private static final class Totals {

        private final Map<ChartModifier, Float> chartTotals = new LinkedHashMap<>();
        private final Map<AtlasModifier, Float> atlasTotals = new EnumMap<>(AtlasModifier.class);

        void add(AtlasTreeType type) {
            type.effects().forEach(line -> chartTotals.merge(line.effect(), line.value(), Float::sum));
            type.atlasEffects().forEach(line -> atlasTotals.merge(line.effect(), line.value(), Float::sum));
        }

        boolean isEmpty() {
            return chartTotals.isEmpty() && atlasTotals.isEmpty();
        }

        Group toGroup(List<String> anomalyIds) {
            List<AppliedEffect> chartLines = new ArrayList<>();
            chartTotals.forEach((effect, value) -> chartLines.add(new AppliedEffect(effect, value)));
            List<AtlasEffects.Line> atlasLines = new ArrayList<>();
            atlasTotals.forEach((effect, value) -> atlasLines.add(new AtlasEffects.Line(effect, value)));
            return new Group(anomalyIds, chartLines, atlasLines);
        }
    }

    private static final Comparator<List<String>> GROUP_ORDER = Comparator.<List<String>>comparingInt(List::size)
            .thenComparing(ids -> String.join("+", ids));

    private AtlasTreeEffectGroups() {
    }

    public static List<Group> of(Collection<AtlasTreeNode> allocatedNodes) {
        Map<List<String>, Totals> byAnomalies = new LinkedHashMap<>();
        for (AtlasTreeNode node : allocatedNodes) {
            if (node == null || node.type() == null) {
                continue;
            }
            byAnomalies.computeIfAbsent(anomalyIdsOf(node), ids -> new Totals()).add(node.type());
        }
        List<Group> groups = new ArrayList<>();
        byAnomalies.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey(GROUP_ORDER))
                .forEach(entry -> groups.add(entry.getValue().toGroup(entry.getKey())));
        return groups;
    }

    static List<String> anomalyIdsOf(AtlasTreeNode node) {
        TreeSet<String> anomalyIds = new TreeSet<>();
        for (String tag : node.getTreeTags().tags()) {
            if (tag != null && ChartAnomalies.byId(tag) != null) {
                anomalyIds.add(tag);
            }
        }
        return List.copyOf(anomalyIds);
    }
}
