package exiledsector.skills.unlock;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.tags.SkillTags;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeGatesDataTest {

    private static final String ABYSS_GATE = "gate_singularity_defeated";

    @BeforeAll
    static void loadRealData() throws Exception {
        RealSkillData.load();
        NodeGates.register(RealSkillData.readJson(RealSkillData.projectRoot().resolve(NodeGates.DATA_PATH)));
    }

    @AfterAll
    static void clear() throws Exception {
        RealSkillData.clear();
        NodeGates.register(new org.json.JSONObject("{}"));
    }

    @Test
    void everyGateTagInTheVocabularyHasConditionsAndNoConfiguredGateIsUnknown() {
        assertEquals(Set.copyOf(SkillTags.GATE), NodeGates.all().keySet());
        NodeGates.all().forEach((gateTag, conditions) -> assertTrue(!conditions.isEmpty(), gateTag));
    }

    @Test
    void theAbyssGateWaitsForTheSingularityToBeDefeated() {
        assertEquals(List.of(UnlockConditionType.RAT_SINGULARITY_DEFEATED),
                NodeGates.all().get(ABYSS_GATE).stream().map(UnlockCondition::getType).toList());
    }

    @Test
    void everyAbyssNodeAndTheWormholeIntoItAreGated() {
        List<String> ungated = SkillTree.getAllNodes().values().stream()
                .filter(node -> SkillTags.ABYSS_REGION.equals(node.getRegion()) || leadsIntoTheAbyss(node))
                .filter(node -> !node.getTags().contains(ABYSS_GATE))
                .map(SkillNode::getId).toList();

        assertTrue(ungated.isEmpty(), "Abyss nodes without the gate: " + ungated);
    }

    @Test
    void everyAbyssNotableAndKeystoneCostsFiftyAbyssalMatter() {
        SkillTree.getAllNodes().values().stream()
                .filter(node -> SkillTags.ABYSS_REGION.equals(node.getRegion()))
                .filter(node -> node.getType().getTier() == SkillTier.NOTABLE || node.getType().getTier() == SkillTier.KEYSTONE)
                .forEach(node -> {
                    assertEquals("rat_abyssal_matter", node.getType().getItemCost().itemId(), node.getId());
                    assertEquals(50f, node.getType().getItemCost().quantity(), node.getId());
                });
    }

    private static boolean leadsIntoTheAbyss(SkillNode node) {
        SkillNode paired = node.getPairedNodeId() == null ? null : SkillTree.get(node.getPairedNodeId());
        return paired != null && SkillTags.ABYSS_REGION.equals(paired.getRegion());
    }
}
