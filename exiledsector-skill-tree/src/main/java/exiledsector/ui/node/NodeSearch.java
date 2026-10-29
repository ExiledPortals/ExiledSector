package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.i18n.StyledText;
import exiledsector.items.DoctrineOverrides;
import exiledlib.skills.DescriptionLine;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledlib.skills.SkillTypeEffect;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class NodeSearch {

    static final float DIM_ALPHA = 0.2f;

    private final HullSize hullSize;
    private final Map<String, Boolean> matchesByNodeId = new HashMap<>();
    private final Map<String, String> effectTextByTypeId = new HashMap<>();
    private String query = "";
    private String needle = "";
    private NodeAllocator.Snapshot matchedSnapshot;
    private boolean socketFocus;
    private boolean treeSocketFocus;
    private Set<String> statFocusNodeIds = Set.of();
    private int nextMatchIndex;

    public NodeSearch() {
        this(null);
    }

    public NodeSearch(HullSize hullSize) {
        this.hullSize = hullSize;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
        this.needle = this.query.toLowerCase(Locale.ROOT);
        matchesByNodeId.clear();
        nextMatchIndex = 0;
    }

    SkillNode nextMatch(Collection<SkillNode> nodes, NodeAllocator.Snapshot allocation) {
        if (query.isEmpty()) {
            return null;
        }
        List<SkillNode> found = nodes.stream().filter(node -> matches(node, allocation)).toList();
        if (found.isEmpty()) {
            return null;
        }
        int index = nextMatchIndex % found.size();
        nextMatchIndex = index + 1;
        return found.get(index);
    }

    public boolean isActive() {
        return !statFocusNodeIds.isEmpty() || socketFocus || !query.isEmpty();
    }

    public void setStatFocus(Set<String> nodeIds) {
        statFocusNodeIds = nodeIds == null ? Set.of() : nodeIds;
    }

    public void setSocketFocus(boolean focus) {
        setSocketFocus(focus, focus);
    }

    public void setSocketFocus(boolean focus, boolean treeSockets) {
        socketFocus = focus;
        treeSocketFocus = focus && treeSockets;
        matchesByNodeId.clear();
    }

    public float backgroundAlpha() {
        return isActive() ? DIM_ALPHA : 1f;
    }

    boolean matches(SkillNode node, NodeAllocator.Snapshot allocation) {
        if (!isActive() || allocation.isHidden(node)) {
            return false;
        }
        if (!statFocusNodeIds.isEmpty()) {
            return statFocusNodeIds.contains(node.getId());
        }
        if (socketFocus) {
            return treeSocketFocus && node.getType().getTier() == SkillTier.SOCKET && allocation.skillData().isAllocated(node.getId());
        }
        if (allocation != matchedSnapshot) {
            matchesByNodeId.clear();
            matchedSnapshot = allocation;
        }
        Boolean matched = matchesByNodeId.get(node.getId());
        if (matched == null) {
            matched = nodeMatches(node, allocation.skillData());
            matchesByNodeId.put(node.getId(), matched);
        }
        return matched;
    }

    private boolean nodeMatches(SkillNode node, ShipSkillData skillData) {
        if (typeMatches(node.resolveEffectiveType(skillData)) || doctrineEffectsMatch(node, skillData)) {
            return true;
        }
        for (String optionId : node.getType().getOptionalOptionIds()) {
            if (typeMatches(SkillTree.getType(optionId))) {
                return true;
            }
        }
        return false;
    }

    private boolean typeMatches(SkillType type) {
        return type != null && (nameContains(type, needle) || effectText(type).contains(needle));
    }

    private boolean doctrineEffectsMatch(SkillNode node, ShipSkillData skillData) {
        for (SkillTypeEffect effect : DoctrineOverrides.addedEffects(skillData, node, hullSize)) {
            StyledText line = effect.effect().description(effect.magnitude(), hullSize);
            if (line != null && containsIgnoringCase(line.plain(), needle)) {
                return true;
            }
        }
        return false;
    }

    private String effectText(SkillType type) {
        return effectTextByTypeId.computeIfAbsent(type.getId(), id -> {
            StringJoiner lines = new StringJoiner("\n");
            for (DescriptionLine line : SkillNode.describeEffects(type, hullSize)) {
                lines.add(line.plain());
            }
            return lines.toString().toLowerCase(Locale.ROOT);
        });
    }

    float nodeAlpha(SkillNode node, NodeAllocator.Snapshot allocation) {
        return !isActive() || matches(node, allocation) ? 1f : DIM_ALPHA;
    }

    float connectorAlpha(SkillNode a, SkillNode b, NodeAllocator.Snapshot allocation) {
        return !isActive() || (matches(a, allocation) && matches(b, allocation)) ? 1f : DIM_ALPHA;
    }

    private static boolean nameContains(SkillType type, String needle) {
        return type != null && (containsIgnoringCase(type.getDisplayName(), needle)
                || containsIgnoringCase(type.getSourceName(), needle) || containsIgnoringCase(type.getId(), needle));
    }

    private static boolean containsIgnoringCase(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }
}
