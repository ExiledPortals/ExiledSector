package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasNames;
import exiledsector.charts.tree.AtlasTreeNode;
import exiledsector.i18n.StyledText;
import exiledsector.skills.tags.ThemeTags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class AtlasTreeSearchTerms {

    private AtlasTreeSearchTerms() {
    }

    static List<AtlasTreeSearch.Entry> of(Collection<AtlasTreeNode> nodes, int luckyRolls) {
        List<AtlasTreeSearch.Entry> entries = new ArrayList<>();
        for (AtlasTreeNode node : nodes) {
            entries.add(new AtlasTreeSearch.Entry(node.getId(), node.getTags(), terms(node, luckyRolls)));
        }
        return entries;
    }

    private static List<String> terms(AtlasTreeNode node, int luckyRolls) {
        List<String> terms = new ArrayList<>();
        terms.add(node.getDisplayName());
        for (StyledText line : AtlasTreeNodeText.tooltipBody(node.type(), luckyRolls)) {
            terms.add(line.plain());
        }
        for (String tag : node.getTags()) {
            terms.add(tag);
            terms.add(AtlasNames.anomaly(tag));
        }
        for (String tag : node.type().tags()) {
            terms.add(tag);
            terms.add(ThemeTags.isThemeTag(tag) ? ThemeTags.displayName(tag) : AtlasNames.name("tag", tag));
        }
        return terms;
    }
}
