package exiledsector.skills.template;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateStepRulesTest {

    private static final Predicate<SkillNode> ALLOCATABLE = node -> true;
    private static final BiFunction<SkillNode, SkillType, String> UNBLOCKED = (node, option) -> null;

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
        return TemplateStepRules.verdict(new TemplateStep(nodeId, option), data, "root", ALLOCATABLE, UNBLOCKED);
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
    void aNodeThatCannotBeAllocatedIsSkippedWithoutConsultingTheBlockRules() {
        AtomicInteger blockChecks = new AtomicInteger();
        BiFunction<SkillNode, SkillType, String> countingBlock = (node, option) -> {
            blockChecks.incrementAndGet();
            return null;
        };

        StepVerdict result = TemplateStepRules.verdict(new TemplateStep("armor", null), data, "root", node -> false, countingBlock);

        assertEquals(StepVerdict.NOT_ALLOCATABLE, result);
        assertEquals(0, blockChecks.get());
    }

    @Test
    void aBlockReasonOnTheNodeOrItsChosenOptionSkipsIt() {
        BiFunction<SkillNode, SkillType, String> blockHull = (node, option) -> option != null && option.getId().equals("hull") ? "blocked" : null;

        assertEquals(StepVerdict.BLOCKED, TemplateStepRules.verdict(new TemplateStep("slot", "hull"), data, "root", ALLOCATABLE, blockHull));
        assertEquals(StepVerdict.BLOCKED, TemplateStepRules.verdict(new TemplateStep("armor", null), data, "root", ALLOCATABLE, (node, option) -> "x"));
    }

    @Test
    void theNodeCountShownForATemplateLeavesOutNodesTheTreeNoLongerHas() {
        SkillTreeTemplate template = new SkillTreeTemplate("id", "Brawler", "root", null, List.of(
                new TemplateStep("armor", null), new TemplateStep("removed_by_update", null), new TemplateStep("slot", "hull")));

        assertEquals(2, template.knownStepCount());
    }
}
