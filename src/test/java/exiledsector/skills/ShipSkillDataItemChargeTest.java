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
        data.chooseStartingRoot(root);
        data.allocate(lobster, 3);
        data.recordItemCharge("lobster", LOBSTERS);

        data.deallocate(lobster);

        assertEquals(LOBSTERS, data.takeItemCharge("lobster"));
        assertNull(data.takeItemCharge("lobster"));
    }

    @Test
    void reallocatingANodeForgetsAChargeNobodyTookWhenItWasReleased() {
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.allocate(lobster, 3);
        data.recordItemCharge("lobster", LOBSTERS);
        data.deallocate(lobster);

        data.allocate(lobster, 3);

        assertNull(data.itemCharge("lobster"));
    }

    @Test
    void aReplacedNodeKeepsItsCharge() {
        node("lobster_v2", SkillTier.SMALL, LOBSTERS);
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.allocate(lobster, 3);
        data.recordItemCharge("lobster", LOBSTERS);

        assertTrue(data.replaceNode("lobster", "lobster_v2"));

        assertNull(data.itemCharge("lobster"));
        assertEquals(LOBSTERS, data.itemCharge("lobster_v2"));
    }

    @Test
    void aSaveFromBeforeTheLedgerTreatsItsAllocatedItemNodesAsCharged() {
        ShipSkillData written = new ShipSkillData();
        written.chooseStartingRoot(root);
        written.allocate(lobster, 3);

        ShipSkillData loaded = roundTripWithoutTheLedger(written);

        assertEquals(LOBSTERS, loaded.itemCharge("lobster"));
        assertNull(loaded.itemCharge("root"));
    }

    @Test
    void anNpcBuildFromBeforeTheLedgerWasNeverCharged() {
        ShipSkillData written = new ShipSkillData();
        written.markNpcBuild();
        written.chooseStartingRoot(root);
        written.allocate(lobster, 3);

        assertNull(roundTripWithoutTheLedger(written).itemCharge("lobster"));
    }

    @Test
    void forgettingNodesFromALegacySaveStillLeavesTheirChargesToRefund() {
        ShipSkillData written = new ShipSkillData();
        written.chooseStartingRoot(root);
        written.allocate(lobster, 3);
        ShipSkillData loaded = roundTripWithoutTheLedger(written);

        assertEquals(List.of("lobster"), loaded.forgetUnknownNodes(Map.of("root", root), SkillTree.getAllTypes()));
        assertEquals(LOBSTERS, loaded.takeItemCharge("lobster"));
    }
}
