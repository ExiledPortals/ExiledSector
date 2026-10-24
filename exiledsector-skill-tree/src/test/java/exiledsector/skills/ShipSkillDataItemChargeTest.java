package exiledsector.skills;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipSkillDataItemChargeTest {

    private static final SkillItemCost LOBSTERS = new SkillItemCost("lobster", 50f);

    private SkillNode root;
    private SkillNode lobster;

    @BeforeEach
    void setUp() {
        RealSkillData.clear();
        root = node("root", SkillTier.ROOT, null);
        lobster = node("lobster", SkillTier.SMALL, LOBSTERS);
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static SkillNode node(String id, SkillTier tier, SkillItemCost itemCost) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).effects(List.of()).itemCost(itemCost).build();
        SkillNode node = new SkillNode(id, type, tier == SkillTier.ROOT ? List.of() : List.of("root"), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    private static ShipSkillData roundTripWithoutTheLedger(ShipSkillData data) {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        String olderXml = xstream.toXML(data).replaceAll("<chargedItem(Ids|Quantities)[^>]*/>", "")
                .replaceAll("(?s)<chargedItem(Ids|Quantities)[^>]*>.*?</chargedItem\\1>", "");
        return (ShipSkillData) xstream.fromXML(olderXml);
    }

    @Test
    void aRecordedChargeIsTakenExactlyOnce() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, lobster, 3);
        ItemChargeLedger.recordItemCharge(data, "lobster", LOBSTERS);

        ShipTrees.deallocate(data, lobster);

        assertEquals(LOBSTERS, ItemChargeLedger.takeItemCharge(data, "lobster"));
        assertNull(ItemChargeLedger.takeItemCharge(data, "lobster"));
    }

    @Test
    void reallocatingANodeForgetsAChargeNobodyTookWhenItWasReleased() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, lobster, 3);
        ItemChargeLedger.recordItemCharge(data, "lobster", LOBSTERS);
        ShipTrees.deallocate(data, lobster);

        ShipTrees.allocate(data, lobster, 3);

        assertNull(ItemChargeLedger.itemCharge(data, "lobster"));
    }

    @Test
    void aReplacedNodeKeepsItsCharge() {
        node("lobster_v2", SkillTier.SMALL, LOBSTERS);
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, lobster, 3);
        ItemChargeLedger.recordItemCharge(data, "lobster", LOBSTERS);

        assertTrue(ShipTrees.replaceNode(data, "lobster", "lobster_v2"));

        assertNull(ItemChargeLedger.itemCharge(data, "lobster"));
        assertEquals(LOBSTERS, ItemChargeLedger.itemCharge(data, "lobster_v2"));
    }

    @Test
    void aSaveFromBeforeTheLedgerTreatsItsAllocatedItemNodesAsCharged() {
        ShipSkillData written = new ShipSkillData();
        ShipTrees.chooseStartingRoot(written, root);
        ShipTrees.allocate(written, lobster, 3);

        ShipSkillData loaded = roundTripWithoutTheLedger(written);

        assertEquals(LOBSTERS, ItemChargeLedger.itemCharge(loaded, "lobster"));
        assertNull(ItemChargeLedger.itemCharge(loaded, "root"));
    }

    @Test
    void aLegacySavesChargeFollowsANodeReplacedOnLoad() {
        node("lobster_v2", SkillTier.SMALL, null);
        ShipSkillData written = new ShipSkillData();
        ShipTrees.chooseStartingRoot(written, root);
        ShipTrees.allocate(written, lobster, 3);
        ShipSkillData loaded = roundTripWithoutTheLedger(written);

        assertTrue(ShipTrees.replaceNode(loaded, "lobster", "lobster_v2"));

        assertEquals(LOBSTERS, ItemChargeLedger.itemCharge(loaded, "lobster_v2"));
    }

    @Test
    void anNpcBuildFromBeforeTheLedgerWasNeverCharged() {
        ShipSkillData written = new ShipSkillData();
        written.markNpcBuild();
        ShipTrees.chooseStartingRoot(written, root);
        ShipTrees.allocate(written, lobster, 3);

        assertNull(ItemChargeLedger.itemCharge(roundTripWithoutTheLedger(written), "lobster"));
    }

    @Test
    void forgettingNodesFromALegacySaveStillLeavesTheirChargesToRefund() {
        ShipSkillData written = new ShipSkillData();
        ShipTrees.chooseStartingRoot(written, root);
        ShipTrees.allocate(written, lobster, 3);
        ShipSkillData loaded = roundTripWithoutTheLedger(written);

        assertEquals(List.of("lobster"), ShipTrees.forgetUnknownNodes(loaded, Map.of("root", root), SkillTree.getAllTypes()));
        assertEquals(LOBSTERS, ItemChargeLedger.takeItemCharge(loaded, "lobster"));
    }
}
