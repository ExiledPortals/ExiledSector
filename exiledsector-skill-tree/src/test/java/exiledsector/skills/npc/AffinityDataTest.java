package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.AffinitySteps;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.special.SpecialSkillEffect;
import exiledsector.effects.SkillTreeEffectSource;
import exiledsector.skills.AffinityOptions;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateCodec;
import exiledsector.skills.template.TemplateStep;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        SkillType affinityNode = SkillTree.getType("small_flux_optional");

        assertEquals(List.of("affinity_low_tech", "affinity_high_tech", "affinity_midline"), affinityNode.getOptionalOptionIds());
        for (String optionId : affinityNode.getOptionalOptionIds()) {
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

    @Test
    void oldSelectionsAreMappedOnceAndNewOnesLeftAlone() {
        assertEquals("affinity_low_tech", AffinityOptions.currentOptionId("hull"));
        assertEquals("affinity_high_tech", AffinityOptions.currentOptionId("flux_capacity"));
        assertEquals("affinity_high_tech", AffinityOptions.currentOptionId("flux_dissipation"));
        assertEquals("affinity_midline", AffinityOptions.currentOptionId("fuel_crew_cargo"));
        assertEquals("affinity_midline", AffinityOptions.currentOptionId("affinity_midline"));
        assertEquals("shield_arc", AffinityOptions.currentOptionId("shield_arc"));
    }

    @Test
    void aSavedShipsOldSelectionsAreMigratedAndSurviveTheRepairPass() {
        NpcTreeBuild build = NpcSkillTreeBuilder.generate(new NpcBuildRequest(NpcArchetypes.ALL.get(0).profile(), "Low Tech", null,
                NpcHullMods.NONE, NpcFreedOp.NONE, 30, null), new Random(5L));
        String oldTag = NpcTreeTag.encode(build.shipData()).replace("=affinity_low_tech", "=hull");
        assertTrue(oldTag.contains("=hull"));

        ShipSkillData decoded = NpcTreeTag.decode(oldTag);

        assertNotNull(decoded);
        assertEquals(build.shipData().getAllocatedNodeIds(), decoded.getAllocatedNodeIds());
        for (String nodeId : decoded.getAllocatedNodeIds()) {
            assertEquals(build.shipData().getOptionalSelection(nodeId), decoded.getOptionalSelection(nodeId), nodeId);
        }
    }

    @Test
    void aTemplateNamingAnOldOptionReadsBackAsItsAffinity() {
        SkillTreeTemplate old = new SkillTreeTemplate("t", "Old", "root_low_tech_1", HullSize.CRUISER,
                List.of(new TemplateStep("small_flux_optional_1", "flux_dissipation")));

        SkillTreeTemplate decoded = TemplateCodec.decode(TemplateCodec.encode(old));

        assertEquals("affinity_high_tech", decoded.steps().get(0).optionTypeId());
        assertFalse(AffinityOptions.migrate(null));
    }
}
