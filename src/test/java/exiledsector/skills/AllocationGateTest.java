package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.skilleffect.ShieldSkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllocationGateTest {

    private static final int AMPLE_OP = 100;
    private static final int OP_PER_NODE = 2;
    private static final int AMPLE_NODE_CAP = 50;
    private static final ShipFacts FRIGATE = new ShipFacts(HullSize.FRIGATE, ShieldType.FRONT, false, 100f, hullModId -> false);

    private ShipSkillData data;
    private SkillNode root;

    @BeforeEach
    void setUp() {
        RealSkillData.clear();
        root = node("root", type("root_type", SkillTier.ROOT).build());
        data = new ShipSkillData();
        data.chooseStartingRoot(root);
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static SkillType.Builder type(String id, SkillTier tier) {
        return new SkillType.Builder(id, id + " name", "a.png", tier);
    }

    private static SkillType registered(SkillType type) {
        SkillTree.registerType(type);
        return type;
    }

    private static SkillNode node(String id, SkillType type, String... connectedTo) {
        SkillNode node = new SkillNode(id, type, Arrays.asList(connectedTo), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    private static SkillNode optional(String id, SkillType... options) {
        return node(id, registered(type(id + "_type", SkillTier.SMALL).optionalOptionIds(Arrays.stream(options).map(SkillType::getId).toList())
                .build()), "root");
    }

    private AllocationGate gate(int totalOp, int maxNodes, ToDoubleFunction<String> heldItems, Predicate<SkillType> locked) {
        NodeEligibility.Context eligibility = NodeEligibility.Context.of(data, FRIGATE, locked, heldItems);
        return new AllocationGate(data, SkillTree.topology(), "root", new AllocationGate.Budget(totalOp, OP_PER_NODE, maxNodes), eligibility);
    }

    private AllocationGate gate() {
        return gate(AMPLE_OP, AMPLE_NODE_CAP, itemId -> 0, null);
    }

    private static AllocationGate.Refusal refusal(AllocationGate.Verdict verdict) {
        return verdict.refusal();
    }

    @Test
    void aConnectedEligibleNodeWithinBudgetIsAllowed() {
        SkillNode armor = node("armor", type("armor_type", SkillTier.SMALL).build(), "root");

        assertTrue(gate().allocation(armor).allowed());
        assertEquals(OP_PER_NODE, gate().opCostFor(armor));
        assertEquals(0, gate().opCostFor(root));
    }

    @Test
    void anUnconnectedNodeIsRefusedBeforeItsBudgetOrEligibilityIsConsidered() {
        SkillNode far = node("far", type("far_type", SkillTier.SMALL).requiredHullSizes(List.of(HullSize.CAPITAL_SHIP)).build(), "elsewhere");

        assertEquals(AllocationGate.Refusal.NOT_CONNECTED, refusal(gate(0, 0, itemId -> 0, null).allocation(far)));
    }

    @Test
    void theNodeCapAndTheOpBudgetAreCheckedBeforeEligibility() {
        SkillNode capital = node("capital", type("capital_type", SkillTier.SMALL).requiredHullSizes(List.of(HullSize.CAPITAL_SHIP)).build(),
                "root");

        assertEquals(AllocationGate.Refusal.NODE_CAP, refusal(gate(AMPLE_OP, 1, itemId -> 0, null).allocation(capital)));
        assertEquals(AllocationGate.Refusal.OUT_OF_OP, refusal(gate(OP_PER_NODE - 1, AMPLE_NODE_CAP, itemId -> 0, null).allocation(capital)));
        AllocationGate.Verdict ineligible = gate().allocation(capital);
        assertEquals(AllocationGate.Refusal.INELIGIBLE, refusal(ineligible));
        assertEquals(NodeEligibility.Kind.WRONG_HULL_SIZE, ineligible.block().kind());
    }

    @Test
    void aBankedFreeAllocationIgnoresTheOpBudget() {
        SkillNode armor = node("armor", type("armor_type", SkillTier.SMALL).build(), "root");
        data.addFreeAllocationCredit();

        assertTrue(gate(0, AMPLE_NODE_CAP, itemId -> 0, null).allocation(armor).allowed());
    }

    @Test
    void anOptionalNodeIsAllocatableWhenAnyOfItsOptionsIs() {
        SkillType capitalOption = registered(type("capital_option", SkillTier.SMALL).requiredHullSizes(List.of(HullSize.CAPITAL_SHIP)).build());
        SkillType anyOption = registered(type("any_option", SkillTier.SMALL).build());
        SkillNode choice = optional("choice", capitalOption, anyOption);

        AllocationGate gate = gate();
        assertTrue(gate.allocation(choice).allowed());
        assertEquals(AllocationGate.Refusal.INELIGIBLE, refusal(gate.allocation(choice, capitalOption)));
        assertTrue(gate.allocation(choice, anyOption).allowed());
        assertEquals(NodeEligibility.Kind.INVALID_OPTION, gate.allocation(choice, null).block().kind());
    }

    @Test
    void anOptionalNodeWhoseOptionsAreAllRefusedReportsTheFirstOptionsReason() {
        SkillType capitalOption = registered(type("capital_option", SkillTier.SMALL).requiredHullSizes(List.of(HullSize.CAPITAL_SHIP)).build());
        SkillType pricedOption = registered(type("priced_option", SkillTier.SMALL).itemCost(new SkillItemCost("alpha_core", 1f)).build());
        SkillNode choice = optional("choice", capitalOption, pricedOption);

        assertEquals(NodeEligibility.Kind.WRONG_HULL_SIZE, gate().allocation(choice).block().kind());
    }

    @Test
    void anItemCostNeedsTheItemsInHandAndNpcBuildsNeverHaveThem() {
        SkillNode lobster = node("lobster", type("lobster_type", SkillTier.SMALL).itemCost(new SkillItemCost("lobster", 10f)).build(), "root");

        assertEquals(NodeEligibility.Kind.ITEM_COST, gate(AMPLE_OP, AMPLE_NODE_CAP, itemId -> 9, null).allocation(lobster).block().kind());
        assertTrue(gate(AMPLE_OP, AMPLE_NODE_CAP, itemId -> 10, null).allocation(lobster).allowed());
        assertEquals(NodeEligibility.Kind.ITEM_COST, gate(AMPLE_OP, AMPLE_NODE_CAP, null, null).allocation(lobster).block().kind());
    }

    @Test
    void theContainersOwnLockAppliesToEveryOption() {
        SkillType option = registered(type("plain_option", SkillTier.SMALL).build());
        SkillNode choice = optional("choice", option);
        SkillType container = choice.getType();

        AllocationGate.Verdict verdict = gate(AMPLE_OP, AMPLE_NODE_CAP, itemId -> 0, type -> type == container).allocation(choice, option);

        assertEquals(NodeEligibility.Kind.LOCKED, verdict.block().kind());
    }

    @Test
    void theStartingRootAndNodesOthersDependOnCannotBeRemoved() {
        SkillNode bridge = node("bridge", type("bridge_type", SkillTier.SMALL).build(), "root", "leaf");
        SkillNode leaf = node("leaf", type("leaf_type", SkillTier.SMALL).build(), "bridge");
        SkillNode loose = node("loose", type("loose_type", SkillTier.SMALL).build(), "root");
        data.allocate(bridge, OP_PER_NODE);
        data.allocate(leaf, OP_PER_NODE);

        AllocationGate gate = gate();
        assertEquals(AllocationGate.Refusal.STARTING_ROOT, refusal(gate.deallocation(root)));
        assertEquals(AllocationGate.Refusal.STRANDS_NODES, refusal(gate.deallocation(bridge)));
        assertTrue(gate.deallocation(leaf).allowed());
        assertEquals(AllocationGate.Refusal.NOT_ALLOCATED, refusal(gate.deallocation(loose)));
        assertEquals(AllocationGate.Refusal.ALREADY_ALLOCATED, refusal(gate.allocation(leaf)));
    }

    @Test
    void switchingToTheSameOptionIsRefusedAndAnUnallocatedNodeCannotSwitch() {
        SkillType first = registered(type("first_option", SkillTier.SMALL).build());
        SkillType second = registered(type("second_option", SkillTier.SMALL).build());
        SkillNode choice = optional("choice", first, second);
        SkillNode other = optional("other", first, second);
        data.selectOption(choice, first, OP_PER_NODE);

        AllocationGate gate = gate();
        assertEquals(AllocationGate.Refusal.SAME_OPTION, refusal(gate.optionSwitch(choice, first)));
        assertTrue(gate.optionSwitch(choice, second).allowed());
        assertEquals(AllocationGate.Refusal.NOT_ALLOCATED, refusal(gate.optionSwitch(other, second)));
    }

    @Test
    void anOptionSwitchIgnoresTheShieldTheReplacedOptionGranted() {
        SkillType frontConversion = registered(type("front_conversion", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT, 1f))).build());
        SkillType otherFrontConversion = registered(type("other_front_conversion", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT, 1f))).build());
        SkillNode choice = optional("choice", frontConversion, otherFrontConversion);
        data.selectOption(choice, frontConversion, OP_PER_NODE);
        NodeEligibility.Context eligibility = NodeEligibility.Context.of(data,
                new ShipFacts(HullSize.FRIGATE, ShieldType.OMNI, false, 100f, hullModId -> false), null, itemId -> 0);
        AllocationGate gate = new AllocationGate(data, SkillTree.topology(), "root", new AllocationGate.Budget(AMPLE_OP, OP_PER_NODE,
                AMPLE_NODE_CAP), eligibility);

        assertEquals(ShieldType.FRONT, eligibility.shieldType());
        assertTrue(gate.optionSwitch(choice, otherFrontConversion).allowed());
    }

    @Test
    void anOptionSwitchCountsTheRefundOfTheReplacedOptionsItem() {
        SkillType first = registered(type("first_option", SkillTier.SMALL).itemCost(new SkillItemCost("alpha_core", 1f)).build());
        SkillType second = registered(type("second_option", SkillTier.SMALL).itemCost(new SkillItemCost("alpha_core", 1f)).build());
        SkillNode choice = optional("choice", first, second);
        data.selectOption(choice, first, OP_PER_NODE);
        data.recordItemCharge("choice", first.getItemCost());
        Map<String, Double> cargo = Map.of("alpha_core", 0d);

        assertTrue(gate(AMPLE_OP, AMPLE_NODE_CAP, itemId -> cargo.getOrDefault(itemId, 0d), null).optionSwitch(choice, second).allowed());
    }
}
