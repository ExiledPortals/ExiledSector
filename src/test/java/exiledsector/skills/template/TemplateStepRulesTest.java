package exiledsector.skills.template;

import exiledsector.skills.AllocationGate;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateStepRulesTest {

    private static final BiFunction<SkillNode, SkillType, AllocationGate.Verdict> ALLOWED = (node, option) -> AllocationGate.Verdict.ALLOWED;

    private ShipSkillData data;

    @BeforeEach
    void setUp() {
        RealSkillData.clear();
        SkillNode root = node("root", type("root", SkillTier.ROOT).build());
        node("armor", type("armor_type", SkillTier.SMALL).build());
        node("lobster", type("lobster_type", SkillTier.SMALL).itemCost(new SkillItemCost("lobster", 1000f)).build());
        SkillTree.registerType(type("hull", SkillTier.SMALL).build());
        SkillTree.registerType(type("pricey", SkillTier.SMALL).itemCost(new SkillItemCost("alpha_core", 1f)).build());
        node("slot", type("slot_type", SkillTier.SMALL).optionalOptionIds(List.of("hull", "pricey")).build());
        data = new ShipSkillData();
        data.chooseStartingRoot(root);
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static SkillType.Builder type(String id, SkillTier tier) {
        return new SkillType.Builder(id, id, "a.png", tier).effects(List.of());
    }

    private static SkillNode node(String id, SkillType type) {
        SkillNode node = new SkillNode(id, type, List.of("root"), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    private StepVerdict verdict(String nodeId, String option) {
        return TemplateStepRules.verdict(new TemplateStep(nodeId, option), data, "root", ALLOWED);
    }

    @Test
    void anOrdinaryReachableUnblockedNodeIsAllocated() {
        assertEquals(StepVerdict.ALLOCATE, verdict("armor", null));
        assertEquals(StepVerdict.ALLOCATE, verdict("slot", "hull"));
    }

    @Test
    void unknownRootAndAlreadyAllocatedNodesAreNotAttempted() {
        data.allocate(SkillTree.get("armor"), 1);

        assertEquals(StepVerdict.UNKNOWN_NODE, verdict("removed_by_update", null));
        assertEquals(StepVerdict.ALREADY_ALLOCATED, verdict("root", null));
        assertEquals(StepVerdict.ALREADY_ALLOCATED, verdict("armor", null));
    }

    @Test
    void itemCostsAreNeverSpentEvenWhenNothingElseBlocksTheNode() {
        assertEquals(StepVerdict.ITEM_COST, verdict("lobster", null));
        assertEquals(StepVerdict.ITEM_COST, verdict("slot", "pricey"));
    }

    @Test
    void anOptionalNodeNeedsOneOfItsOwnOptions() {
        assertEquals(StepVerdict.NO_OPTION, verdict("slot", null));
        assertEquals(StepVerdict.NO_OPTION, verdict("slot", "armor_type"));
    }

    @Test
    void aNodeTheGateCannotPlaceIsNotAllocatableWhileAnIneligibleOneIsBlocked() {
        for (AllocationGate.Refusal refusal : List.of(AllocationGate.Refusal.NOT_CONNECTED, AllocationGate.Refusal.NODE_CAP,
                AllocationGate.Refusal.OUT_OF_OP)) {
            assertEquals(StepVerdict.NOT_ALLOCATABLE, TemplateStepRules.verdict(new TemplateStep("armor", null), data, "root", refusing(refusal)));
        }
        assertEquals(StepVerdict.BLOCKED, TemplateStepRules.verdict(new TemplateStep("armor", null), data, "root",
                refusing(AllocationGate.Refusal.INELIGIBLE)));
    }

    @Test
    void theGateIsAskedAboutTheTemplatesChosenOption() {
        List<String> askedOptions = new ArrayList<>();
        BiFunction<SkillNode, SkillType, AllocationGate.Verdict> blockHull = (node, option) -> {
            askedOptions.add(option == null ? null : option.getId());
            return option != null && option.getId().equals("hull") ? new AllocationGate.Verdict(AllocationGate.Refusal.INELIGIBLE, null)
                    : AllocationGate.Verdict.ALLOWED;
        };

        assertEquals(StepVerdict.BLOCKED, TemplateStepRules.verdict(new TemplateStep("slot", "hull"), data, "root", blockHull));
        assertEquals(List.of("hull"), askedOptions);
    }

    private static BiFunction<SkillNode, SkillType, AllocationGate.Verdict> refusing(AllocationGate.Refusal refusal) {
        return (node, option) -> new AllocationGate.Verdict(refusal, null);
    }

    @Test
    void theNodeCountShownForATemplateLeavesOutNodesTheTreeNoLongerHas() {
        SkillTreeTemplate template = new SkillTreeTemplate("id", "Brawler", "root", null, List.of(
                new TemplateStep("armor", null), new TemplateStep("removed_by_update", null), new TemplateStep("slot", "hull")));

        assertEquals(2, template.knownStepCount());
    }
}
