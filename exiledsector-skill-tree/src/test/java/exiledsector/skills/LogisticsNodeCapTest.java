package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogisticsNodeCapTest {

    private static final ShipFacts FRIGATE = new ShipFacts(HullSize.FRIGATE, ShieldType.FRONT, false, 100f, hullModId -> false);
    private static final AllocationCheck.Budget AMPLE = new AllocationCheck.Budget(1000, 1, 100);

    private ShipSkillData data;

    @BeforeEach
    void setUp() {
        RealSkillData.clear();
        SkillNode root = node("root", type("root_type", SkillTier.ROOT, List.of()));
        data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static SkillType type(String id, SkillTier tier, List<String> tags) {
        SkillType type = new SkillType.Builder(id, id + " name", "a.png", tier).tags(tags).build();
        SkillTree.registerType(type);
        return type;
    }

    private static SkillNode node(String id, SkillType type) {
        SkillNode node = new SkillNode(id, type, id.equals("root") ? List.of() : List.of("root"), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    private static SkillNode logisticsNotable(String id) {
        return node(id, type(id + "_type", SkillTier.NOTABLE, List.of(LogisticsNodes.TAG)));
    }

    private AllocationCheck gate(boolean docked) {
        NodeEligibility.Context eligibility = NodeEligibility.Context.of(data, FRIGATE, null).withLogisticsChangesAllowed(docked);
        return new AllocationCheck(data, SkillTree.topology(), "root", AMPLE, eligibility);
    }

    @Test
    void onlyLogisticsTaggedNotablesAndKeystonesAreCapped() {
        assertTrue(LogisticsNodes.isLogistics(type("cargo_notable", SkillTier.NOTABLE, List.of(LogisticsNodes.TAG))));
        assertTrue(LogisticsNodes.isLogistics(type("cargo_keystone", SkillTier.KEYSTONE, List.of("campaign_only", LogisticsNodes.TAG))));
        assertFalse(LogisticsNodes.isLogistics(type("cargo_small", SkillTier.SMALL, List.of(LogisticsNodes.TAG))));
        assertFalse(LogisticsNodes.isLogistics(type("armor_notable", SkillTier.NOTABLE, List.of("armour"))));
    }

    @Test
    void aShipAllocatesTwoLogisticsNodesByDefaultAndTheThirdIsBlocked() {
        SkillNode first = logisticsNotable("first");
        SkillNode second = logisticsNotable("second");
        SkillNode third = logisticsNotable("third");
        SkillNode small = node("small", type("small_type", SkillTier.SMALL, List.of(LogisticsNodes.TAG)));

        ShipTrees.allocate(data, first, 1);
        assertTrue(gate(true).allocation(second).allowed());
        ShipTrees.allocate(data, second, 1);

        AllocationCheck.Verdict blocked = gate(true).allocation(third);
        assertEquals(AllocationCheck.Refusal.INELIGIBLE, blocked.refusal());
        assertEquals(NodeEligibility.Kind.LOGISTICS_CAP, blocked.block().kind());
        assertTrue(gate(true).allocation(small).allowed());
        assertEquals(2, gate(true).allocatedLogisticsNodes());
    }

    @Test
    void eachExtraSlotRaisesThisShipsCapByOne() {
        SkillNode first = logisticsNotable("first");
        SkillNode second = logisticsNotable("second");
        SkillNode third = logisticsNotable("third");
        ShipTrees.allocate(data, first, 1);
        ShipTrees.allocate(data, second, 1);

        data.addLogisticsSlot();

        assertEquals(3, LogisticsNodes.cap(data));
        assertEquals(3, LogisticsNodes.cap(ShipTrees.planningCopy(data)));
        assertTrue(gate(true).allocation(third).allowed());
    }

    @Test
    void allocatingOrUnallocatingALogisticsNodeNeedsDocking() {
        SkillNode first = logisticsNotable("first");
        SkillNode second = logisticsNotable("second");
        SkillNode armor = node("armor", type("armor_type", SkillTier.NOTABLE, List.of("armour")));
        ShipTrees.allocate(data, first, 1);
        ShipTrees.allocate(data, armor, 1);

        AllocationCheck undocked = gate(false);
        assertEquals(NodeEligibility.Kind.LOGISTICS_UNDOCKED, undocked.allocation(second).block().kind());
        assertEquals(AllocationCheck.Refusal.LOGISTICS_UNDOCKED, undocked.deallocation(first).refusal());
        assertTrue(undocked.deallocation(armor).allowed());
        assertTrue(gate(true).deallocation(first).allowed());
    }

    @Test
    void anOverCapShipKeepsItsNodesButCanAllocateNoMore() {
        SkillNode first = logisticsNotable("first");
        SkillNode second = logisticsNotable("second");
        SkillNode third = logisticsNotable("third");
        SkillNode fourth = logisticsNotable("fourth");
        ShipTrees.allocate(data, first, 1);
        ShipTrees.allocate(data, second, 1);
        ShipTrees.allocate(data, third, 1);

        AllocationCheck gate = gate(true);
        assertEquals(3, gate.allocatedLogisticsNodes());
        assertEquals(NodeEligibility.Kind.LOGISTICS_CAP, gate.allocation(fourth).block().kind());
        assertTrue(gate.deallocation(third).allowed());
        assertTrue(data.isAllocated("third"));
    }

    @Test
    void contextsWithoutALogisticsRuleIgnoreTheCap() {
        SkillNode first = logisticsNotable("first");
        SkillNode second = logisticsNotable("second");
        SkillNode third = logisticsNotable("third");
        ShipTrees.allocate(data, first, 1);
        ShipTrees.allocate(data, second, 1);

        NodeEligibility.Context withoutRule = new NodeEligibility.Context(AllocatedNode.of(data), ShieldType.FRONT, FRIGATE, null, null);
        assertNull(NodeEligibility.check(third, null, withoutRule));
        assertEquals(NodeEligibility.Kind.LOGISTICS_CAP,
                NodeEligibility.check(third, null, withoutRule.withLogistics(NodeEligibility.LogisticsRule.forShip(data))).kind());
    }
}
