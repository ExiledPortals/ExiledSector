package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.AffinitySteps;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.special.SpecialSkillEffect;
import exiledsector.effects.SkillTreeEffectSource;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AffinityDataTest {

    private static final Map<String, SkillEffect> ROOT_AFFINITIES = Map.of(
            "root_low_tech", SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT,
            "root_high_tech", SpecialSkillEffect.HIGH_TECH_AFFINITY_FLAT,
            "root_midline", SpecialSkillEffect.MIDLINE_AFFINITY_FLAT);

    @BeforeAll
    static void loadRealTree() throws Exception {
        RealSkillData.load();
    }

    @AfterAll
    static void clear() {
        RealSkillData.clear();
    }

    private static SkillNode rootOfType(String typeId) {
        return SkillTree.getAllNodes().values().stream()
                .filter(node -> node.getType().getTier() == SkillTier.ROOT && node.getType().getId().equals(typeId)).findFirst().orElseThrow();
    }

    @Test
    void theAffinityNodeOffersExactlyTheThreeAffinitiesAtFiveEach() {
        SkillType affinityNode = SkillTree.getType("affinity");

        assertEquals(List.of("high_tech_affinity", "midline_affinity", "low_tech_affinity"), affinityNode.getChoiceOptionIds());
        for (String optionId : affinityNode.getChoiceOptionIds()) {
            List<SkillTypeEffect> effects = SkillTree.getType(optionId).getEffects();
            assertEquals(1, effects.size(), optionId);
            assertTrue(AffinitySteps.isAffinity(effects.get(0).effect()), optionId);
            assertEquals(5f, effects.get(0).magnitude(), optionId);
        }
    }

    @Test
    void aShipWithOnlyItsRootHasTenOfThatAffinityAndNoneOfTheOthers() {
        ROOT_AFFINITIES.forEach((rootTypeId, affinity) -> {
            ShipSkillData data = new ShipSkillData();
            assertTrue(ShipTrees.chooseStartingRoot(data, rootOfType(rootTypeId)));

            Map<SkillEffect, Float> totals = AffinitySteps.totals(SkillTreeEffectSource.contributions(data, HullSize.CRUISER));

            assertEquals(Map.of(affinity, 10f), totals, rootTypeId);
        });
    }
}
