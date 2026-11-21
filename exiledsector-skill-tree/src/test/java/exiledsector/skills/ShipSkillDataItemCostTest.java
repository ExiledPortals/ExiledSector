package exiledsector.skills;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ClearedSkillTreeExtension.class)
class ShipSkillDataItemCostTest {

    private static final SkillItemCost LOBSTERS = new SkillItemCost("lobster", 50f);

    private SkillNode root;
    private SkillNode lobster;

    @BeforeEach
    void setUp() {
        root = node("root", SkillTier.ROOT, null);
        lobster = node("lobster", SkillTier.SMALL, LOBSTERS);
    }

    private static SkillNode node(String id, SkillTier tier, SkillItemCost itemCost) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).effects(List.of()).itemCost(itemCost).build();
        SkillNode node = new SkillNode(id, type, tier == SkillTier.ROOT ? List.of() : List.of("root"), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    @Test
    void aRecordedChargeIsTakenExactlyOnce() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, lobster, 3);
        ItemCostLedger.recordPaidItemCost(data, "lobster", LOBSTERS);

        ShipTrees.deallocate(data, lobster);

        assertEquals(LOBSTERS, ItemCostLedger.takePaidItemCost(data, "lobster"));
        assertNull(ItemCostLedger.takePaidItemCost(data, "lobster"));
    }

    @Test
    void reallocatingANodeForgetsAChargeNobodyTookWhenItWasReleased() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, lobster, 3);
        ItemCostLedger.recordPaidItemCost(data, "lobster", LOBSTERS);
        ShipTrees.deallocate(data, lobster);

        ShipTrees.allocate(data, lobster, 3);

        assertNull(ItemCostLedger.paidItemCost(data, "lobster"));
    }

    @Test
    void aReplacedNodeKeepsItsCharge() {
        node("lobster_v2", SkillTier.SMALL, LOBSTERS);
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, lobster, 3);
        ItemCostLedger.recordPaidItemCost(data, "lobster", LOBSTERS);

        assertTrue(ShipTrees.replaceNode(data, "lobster", "lobster_v2"));

        assertNull(ItemCostLedger.paidItemCost(data, "lobster"));
        assertEquals(LOBSTERS, ItemCostLedger.paidItemCost(data, "lobster_v2"));
    }
}
