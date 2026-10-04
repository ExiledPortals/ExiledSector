package exiledsector.skills.npc;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.NpcArchetypes.Archetype;
import exiledsector.skills.tags.NodeRequirements;
import exiledsector.skills.tags.SkillTags;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NpcLayoutValidator {

    public static final int TARGET_NODE_COUNT = 42;
    public static final int MAX_NODE_COUNT = NpcSkillTreeBuilder.MAX_NODE_COUNT;
    public static final String CORE_REGION = "core";
    public static final List<String> NPC_NEVER_SATISFIED = List.of("campaign_only", "player_only");

    public record Milestone(int nodeCount, String nodeId, String displayName, SkillTier tier) {
    }

    public record Ineligible(Archetype archetype, String unmetRequirement) {
    }

    public record ArchetypeRun(Archetype archetype, NpcTreeBuild atTarget, NpcTreeBuild atMax) {

        public int allocatedAtTarget() {
            return atTarget.allocatedCount();
        }

        public int allocatedAtMax() {
            return atMax.allocatedCount();
        }

        public boolean meetsTarget() {
            return allocatedAtTarget() >= TARGET_NODE_COUNT;
        }

        public int unreachedAtMax() {
            return (int) atMax.steps().stream().filter(step -> NpcBuildStep.COUNT_REACHED.equals(step.outcome())).count();
        }

        public List<Milestone> milestones() {
            return NpcLayoutValidator.milestones(atMax);
        }
    }

    public record LayoutReport(NpcLayout layout, List<String> staticProblems, List<ArchetypeRun> eligibleRuns,
                               List<Ineligible> ineligible) {

        public List<String> runProblems() {
            List<String> problems = new ArrayList<>();
            for (ArchetypeRun run : eligibleRuns) {
                if (!run.meetsTarget()) {
                    problems.add(run.archetype().name() + " allocates only " + run.allocatedAtTarget() + " of "
                            + TARGET_NODE_COUNT + " nodes at nodeCount " + TARGET_NODE_COUNT);
                }
            }
            return problems;
        }

        public List<String> problems() {
            List<String> problems = new ArrayList<>(staticProblems);
            problems.addAll(runProblems());
            return problems;
        }

        public boolean passed() {
            return problems().isEmpty();
        }
    }

    private NpcLayoutValidator() {
    }

    public static List<String> fileProblems(JSONObject root) {
        List<String> problems = new ArrayList<>();
        JSONObject layoutsJson = root == null ? null : root.optJSONObject("layouts");
        if (layoutsJson == null) {
            problems.add("the file has no top-level \"layouts\" object");
            return problems;
        }
        for (String id : NpcLayoutLoader.layoutIds(layoutsJson)) {
            try {
                NpcLayoutLoader.parseLayout(id, layoutsJson.opt(id));
            } catch (JSONException e) {
                problems.add("layout " + id + " is malformed and would be skipped: " + e.getMessage());
            }
        }
        return problems;
    }

    public static Map<String, NpcLayout> parse(JSONObject root) {
        return NpcLayoutLoader.parseLayouts(root);
    }

    public static LayoutReport report(NpcLayout layout) {
        List<ArchetypeRun> eligibleRuns = new ArrayList<>();
        List<Ineligible> ineligible = new ArrayList<>();
        for (Archetype archetype : NpcArchetypes.ALL) {
            if (layout.isEligible(archetype.profile())) {
                eligibleRuns.add(run(layout, archetype));
            } else {
                ineligible.add(new Ineligible(archetype, NodeRequirements.firstUnmet(layout.requires(), archetype.profile())));
            }
        }
        return new LayoutReport(layout, staticProblems(layout), eligibleRuns, ineligible);
    }

    public static ArchetypeRun run(NpcLayout layout, Archetype archetype) {
        return new ArchetypeRun(archetype,
                NpcSkillTreeBuilder.build(layout, TARGET_NODE_COUNT, archetype.profile(), NpcHullMods.NONE),
                NpcSkillTreeBuilder.build(layout, MAX_NODE_COUNT, archetype.profile(), NpcHullMods.NONE));
    }

    public static List<Milestone> milestones(NpcTreeBuild build) {
        List<Milestone> milestones = new ArrayList<>();
        int count = 0;
        for (NpcBuildStep step : build.steps()) {
            if (!step.isAllocated()) {
                continue;
            }
            count++;
            SkillNode node = SkillTree.get(step.nodeId());
            SkillType effective = node.resolveEffectiveType(build.data());
            SkillTier tier = notableTier(node.getType().getTier(), effective.getTier());
            if (tier != null) {
                milestones.add(new Milestone(count, node.getId(), effective.getDisplayName(), tier));
            }
        }
        return milestones;
    }

    private static SkillTier notableTier(SkillTier nodeTier, SkillTier effectiveTier) {
        if (nodeTier == SkillTier.KEYSTONE || effectiveTier == SkillTier.KEYSTONE) {
            return SkillTier.KEYSTONE;
        }
        if (nodeTier == SkillTier.NOTABLE || effectiveTier == SkillTier.NOTABLE) {
            return SkillTier.NOTABLE;
        }
        return null;
    }

    public static List<String> staticProblems(NpcLayout layout) {
        List<String> problems = new ArrayList<>();
        addRootProblems(layout, problems);
        addRequiresProblems(layout, problems);
        if (layout.entries().isEmpty()) {
            problems.add("the layout has no nodes");
        }
        Set<String> earlier = new HashSet<>();
        for (int i = 0; i < layout.entries().size(); i++) {
            NpcLayoutEntry entry = layout.entries().get(i);
            addEntryProblems(layout, entry, earlier, "entry " + (i + 1) + " (" + entry.nodeId() + ")", problems);
            earlier.add(entry.nodeId());
        }
        return problems;
    }

    private static void addRootProblems(NpcLayout layout, List<String> problems) {
        SkillNode root = SkillTree.get(layout.rootNodeId());
        if (root == null) {
            problems.add("unknown root node id " + layout.rootNodeId());
        } else if (root.getType().getTier() != SkillTier.ROOT) {
            problems.add("root " + layout.rootNodeId() + " is a " + root.getType().getTier() + " node, not a ROOT node");
        }
    }

    private static void addRequiresProblems(NpcLayout layout, List<String> problems) {
        for (String tag : layout.requires()) {
            if (!SkillTags.isRequirement(tag)) {
                problems.add("requires contains " + tag + ", which is not a requirement tag");
            } else if (NPC_NEVER_SATISFIED.contains(tag)) {
                problems.add("requires contains " + tag + ", which NPC ships never satisfy");
            }
        }
    }

    private static void addEntryProblems(NpcLayout layout, NpcLayoutEntry entry, Set<String> earlier, String label,
                                         List<String> problems) {
        if (entry.nodeId().equals(layout.rootNodeId())) {
            problems.add(label + " repeats the layout root");
        } else if (earlier.contains(entry.nodeId())) {
            problems.add(label + " is a duplicate of an earlier entry");
        }
        SkillNode node = SkillTree.get(entry.nodeId());
        if (node == null) {
            problems.add(label + " is an unknown node id");
            return;
        }
        addTierProblems(node, label, problems);
        addRegionProblems(node, label, problems);
        SkillType option = addOptionProblems(node, entry.optionTypeId(), label, problems);
        for (String tag : node.effectiveTags(option)) {
            if (NPC_NEVER_SATISFIED.contains(tag)) {
                problems.add(label + " is tagged " + tag + ", so NPC ships can never take it");
            }
        }
        if (!entry.nodeId().equals(layout.rootNodeId()) && !isConnectedToEarlier(node, layout.rootNodeId(), earlier)) {
            problems.add(label + " is not connected: its connectedTo " + node.getConnectedNodeIds()
                    + " contains neither the root nor an earlier entry");
        }
    }

    private static void addTierProblems(SkillNode node, String label, List<String> problems) {
        SkillTier tier = node.getType().getTier();
        if (tier == SkillTier.WORMHOLE) {
            problems.add(label + " is a wormhole node");
        } else if (tier == SkillTier.ROOT) {
            problems.add(label + " is a root node");
        }
    }

    private static void addRegionProblems(SkillNode node, String label, List<String> problems) {
        List<String> regions = node.getTags().stream().filter(SkillTags::isRegion).toList();
        if (!regions.contains(CORE_REGION)) {
            problems.add(label + " is not in region " + CORE_REGION + " (regions: " + regions + ")");
        }
    }

    private static SkillType addOptionProblems(SkillNode node, String optionTypeId, String label, List<String> problems) {
        SkillType type = node.getType();
        String reason = NpcSkillTreeBuilder.optionSkipReason(type, optionTypeId);
        if (reason == null) {
            return type.isOptional() ? SkillTree.getType(optionTypeId) : null;
        }
        if (NpcBuildStep.MISSING_OPTION.equals(reason)) {
            problems.add(label + " is an optional node without an option (choose one of " + type.getOptionalOptionIds() + ")");
        } else if (reason.startsWith(NpcBuildStep.UNEXPECTED_OPTION)) {
            problems.add(label + " has option " + optionTypeId + " but is not an optional node");
        } else {
            problems.add(label + " has option " + optionTypeId + ", which is not one of " + type.getOptionalOptionIds());
        }
        return null;
    }

    private static boolean isConnectedToEarlier(SkillNode node, String rootNodeId, Set<String> earlier) {
        for (String connectedId : node.getConnectedNodeIds()) {
            if (connectedId.equals(rootNodeId) || earlier.contains(connectedId)) {
                return true;
            }
        }
        return false;
    }
}
