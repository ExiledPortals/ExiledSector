package exiledsector.skills.npc;

import exiledsector.skills.AllocatedNode;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.NpcArchetypes.Archetype;
import exiledsector.skills.tags.NodeRequirements;
import exiledsector.skills.tags.SkillTags;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcGenerationDataTest {

    private static final List<String> DESIGN_TYPES = Arrays.asList("Low Tech", "Midline", "High Tech", null);
    private static final List<Integer> NODE_COUNTS = List.of(8, 30);
    private static final int FACTION_TASTE_COUNT = 20;
    private static final int HULLMOD_OP = 10;
    private static final int OP_PER_NODE = 2;
    private static final int CONVERTIBLE_HULLMODS = 6;

    @BeforeAll
    static void loadRealTree() throws Exception {
        RealSkillData.load();
    }

    @AfterAll
    static void clear() {
        RealSkillData.clear();
    }

    private static List<String> factions() {
        List<String> factions = new ArrayList<>();
        factions.add(null);
        factions.addAll(SkillTags.FACTION_VOLUMES);
        return factions;
    }

    @Test
    void everyGeneratedTreeIsConnectedUsableAndStaysOutOfOtherVolumes() {
        List<String> problems = new ArrayList<>();
        int combination = 0;
        for (Archetype archetype : NpcArchetypes.ALL) {
            for (String faction : factions()) {
                for (int count : NODE_COUNTS) {
                    String designType = DESIGN_TYPES.get(combination++ % DESIGN_TYPES.size());
                    String label = archetype.name() + " / " + designType + " / " + faction + " / " + count;
                    NpcTreeBuild build = NpcSkillTreeBuilder.generate(new NpcBuildRequest(archetype.profile(), designType, faction,
                            NpcHullMods.NONE, NpcFreedOp.NONE, count), new Random(label.hashCode()));
                    check(label, build, archetype, faction, problems);
                    long charged = build.steps().stream().filter(NpcBuildStep::isCharged).count();
                    if (charged != count) {
                        problems.add(label + ": allocated " + charged + " of " + count);
                    }
                }
            }
        }

        assertTrue(problems.isEmpty(), problems.size() + " problems:\n" + String.join("\n", problems));
    }

    @Test
    void realHullmodsAreConvertedOrKeptAndTheirOpBuysExactlyTheExtraNodes() {
        Set<String> convertible = new TreeSet<>();
        for (SkillNode node : SkillTree.topology().sortedById()) {
            String hullModId = node.getType().getEquivalentHullModId();
            SkillTier tier = node.getType().getTier();
            if (hullModId != null && tier != SkillTier.ROOT && tier != SkillTier.WORMHOLE) {
                convertible.add(hullModId);
            }
        }
        Set<String> removable = new TreeSet<>(new ArrayList<>(convertible).subList(0, Math.min(CONVERTIBLE_HULLMODS, convertible.size())));
        Map<String, Integer> costs = new HashMap<>();
        removable.forEach(id -> costs.put(id, HULLMOD_OP));
        assertFalse(removable.isEmpty());

        List<String> problems = new ArrayList<>();
        for (Archetype archetype : NpcArchetypes.ALL) {
            for (int count : NODE_COUNTS) {
                String label = archetype.name() + " / hullmods " + removable + " / " + count;
                NpcTreeBuild build = NpcSkillTreeBuilder.generate(new NpcBuildRequest(archetype.profile(), "Midline", null,
                        new NpcHullMods(removable, Set.of()), new NpcFreedOp(OP_PER_NODE, costs, NpcSkillTreeBuilder.MAX_NODE_COUNT), count),
                        new Random(label.hashCode()));
                check(label, build, archetype, null, problems);
                long charged = build.steps().stream().filter(NpcBuildStep::isCharged).count();
                long expected = Math.min(NpcSkillTreeBuilder.MAX_NODE_COUNT,
                        count + (long) build.strippedHullModIds().size() * HULLMOD_OP / OP_PER_NODE);
                if (charged != expected) {
                    problems.add(label + ": allocated " + charged + ", expected " + expected + " after stripping " + build.strippedHullModIds());
                }
                for (AllocatedNode node : AllocatedNode.of(build.shipData())) {
                    for (String hullModId : node.exclusiveHullModIds()) {
                        if (removable.contains(hullModId) && !build.strippedHullModIds().contains(hullModId)) {
                            problems.add(label + ": " + node.node().getId() + " clashes with kept hullmod " + hullModId);
                        }
                    }
                }
            }
        }

        assertTrue(problems.isEmpty(), problems.size() + " problems:\n" + String.join("\n", problems));
    }

    private static void check(String label, NpcTreeBuild build, Archetype archetype, String faction, List<String> problems) {
        ShipSkillData data = build.shipData();
        boolean tasted = false;
        for (String nodeId : data.getAllocatedNodeIds()) {
            SkillNode node = SkillTree.get(nodeId);
            SkillTier tier = node.getType().getTier();
            if (tier == SkillTier.ROOT) {
                continue;
            }
            String region = node.getRegion();
            if (region != null && !SkillTags.CORE_REGION.equals(region) && !region.equals(faction)) {
                problems.add(label + ": " + nodeId + " is in " + region);
            }
            tasted |= faction != null && faction.equals(region) && (tier == SkillTier.NOTABLE || tier == SkillTier.KEYSTONE);
            if (node.getConnectedNodeIds().stream().noneMatch(data::isAllocated)) {
                problems.add(label + ": " + nodeId + " is not connected");
            }
            String optionId = data.getOptionalSelection(nodeId);
            SkillType option = optionId == null ? null : SkillTree.getType(optionId);
            String unmet = NodeRequirements.firstUnmet(node.effectiveTags(option), archetype.profile());
            if (unmet != null) {
                problems.add(label + ": " + nodeId + " needs " + unmet);
            }
            SkillType effective = option == null ? node.getType() : option;
            if (!node.getType().allowsHullSize(archetype.profile().hullSize()) || !effective.allowsHullSize(archetype.profile().hullSize())) {
                problems.add(label + ": " + nodeId + " is not allowed on " + archetype.profile().hullSize());
            }
        }
        List<AllocatedNode> allocated = AllocatedNode.of(data);
        for (int i = 0; i < allocated.size(); i++) {
            for (int j = i + 1; j < allocated.size(); j++) {
                if (allocated.get(i).isExclusiveWith(allocated.get(j))) {
                    problems.add(label + ": " + allocated.get(i).node().getId() + " and " + allocated.get(j).node().getId()
                            + " are mutually exclusive");
                }
            }
        }
        if (faction != null && data.getAllocatedNodeIds().size() > FACTION_TASTE_COUNT && !tasted && hasUsableNotable(faction, archetype)) {
            problems.add(label + ": took no notable from its faction volume");
        }
    }

    private static boolean hasUsableNotable(String faction, Archetype archetype) {
        for (SkillNode node : SkillTree.topology().sortedById()) {
            if (faction.equals(node.getRegion()) && (node.getType().getTier() == SkillTier.NOTABLE || node.getType().getTier() == SkillTier.KEYSTONE)
                    && NodeRequirements.firstUnmet(node.effectiveTags((SkillType) null), archetype.profile()) == null) {
                return true;
            }
        }
        return false;
    }
}
