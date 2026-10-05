package exiledsector.ui.node;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class NodeSearch {

    static final float DIM_ALPHA = 0.2f;

    private final Map<String, Boolean> matchesByNodeId = new HashMap<>();
    private String query = "";
    private String needle = "";
    private NodeAllocator.Snapshot matchedTree;
    private boolean socketFocus;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
        this.needle = this.query.toLowerCase(Locale.ROOT);
        matchesByNodeId.clear();
    }

    public boolean isActive() {
        return socketFocus || !query.isEmpty();
    }

    public void setSocketFocus(boolean focus) {
        socketFocus = focus;
        matchesByNodeId.clear();
    }

    public boolean isSocketFocus() {
        return socketFocus;
    }

    public float backgroundAlpha() {
        return isActive() ? DIM_ALPHA : 1f;
    }

    boolean matches(SkillNode node, NodeAllocator.Snapshot tree) {
        if (!isActive() || tree.isHidden(node)) {
            return false;
        }
        if (socketFocus) {
            return node.getType().getTier() == SkillTier.SOCKET && tree.data().isAllocated(node.getId());
        }
        if (tree != matchedTree) {
            matchesByNodeId.clear();
            matchedTree = tree;
        }
        Boolean matched = matchesByNodeId.get(node.getId());
        if (matched == null) {
            matched = nameMatches(node, tree);
            matchesByNodeId.put(node.getId(), matched);
        }
        return matched;
    }

    private boolean nameMatches(SkillNode node, NodeAllocator.Snapshot tree) {
        if (nameContains(node.resolveEffectiveType(tree.data()), needle)) {
            return true;
        }
        for (String optionId : node.getType().getOptionalOptionIds()) {
            if (nameContains(SkillTree.getType(optionId), needle)) {
                return true;
            }
        }
        return false;
    }

    float nodeAlpha(SkillNode node, NodeAllocator.Snapshot tree) {
        return !isActive() || matches(node, tree) ? 1f : DIM_ALPHA;
    }

    float connectorAlpha(SkillNode a, SkillNode b, NodeAllocator.Snapshot tree) {
        return !isActive() || (matches(a, tree) && matches(b, tree)) ? 1f : DIM_ALPHA;
    }

    private static boolean nameContains(SkillType type, String needle) {
        return type != null && (containsIgnoringCase(type.getDisplayName(), needle)
                || containsIgnoringCase(type.getSourceName(), needle) || containsIgnoringCase(type.getId(), needle));
    }

    private static boolean containsIgnoringCase(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }
}
