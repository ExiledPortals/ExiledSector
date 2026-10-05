package exiledsector.skills;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipSkillDataSocketTest {

    private SkillNode root;
    private SkillNode socket;

    @BeforeEach
    void setUp() {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        root = node("root", SkillTier.ROOT, List.of());
        socket = node("socket", SkillTier.SOCKET, List.of("root"));
        SkillTree.register(root);
        SkillTree.register(socket);
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
    }

    private static SkillNode node(String id, SkillTier tier, List<String> connected) {
        SkillType type = new SkillType.Builder(id + "_type", id, "", tier).build();
        return new SkillNode(id, type, connected, 0f, 0f);
    }

    private ShipSkillData socketed() {
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.allocate(socket, 3);
        assertTrue(data.socketItem("socket", "socketable_1"));
        return data;
    }

    @Test
    void anOldSaveHasNoSocketedItems() {
        ShipSkillData data = new ShipSkillData();

        assertEquals(Map.of(), data.getSocketedItems());
        assertNull(data.getSocketedItem("socket"));
        assertNull(data.unsocketItem("socket"));
    }

    @Test
    void onlyAnAllocatedSocketCanHoldAnItem() {
        ShipSkillData data = new ShipSkillData();

        assertFalse(data.socketItem("socket", "socketable_1"));
        assertEquals(Map.of(), data.getSocketedItems());
    }

    @Test
    void swappingReplacesTheItemAndEmptyingReturnsIt() {
        ShipSkillData data = socketed();

        data.socketItem("socket", "socketable_2");
        assertEquals(Map.of("socket", "socketable_2"), data.getSocketedItems());
        assertEquals("socketable_2", data.unsocketItem("socket"));
        assertTrue(data.isAllocated("socket"));
        assertEquals(Map.of(), data.getSocketedItems());
    }

    @Test
    void deallocatingTheSocketEmptiesIt() {
        ShipSkillData data = socketed();

        data.deallocate(socket);

        assertEquals(Map.of(), data.getSocketedItems());
    }

    @Test
    void aRespecOrResetEmptiesEverySocket() {
        ShipSkillData data = socketed();

        data.resetAllocations();

        assertEquals(Map.of(), data.getSocketedItems());
    }

    @Test
    void anAreaToggleThatRemovesTheSocketEmptiesIt() {
        ShipSkillData data = socketed();

        data.forgetUnknownNodes(Map.of("root", root), Map.of());

        assertEquals(Map.of(), data.getSocketedItems());
    }

    @Test
    void aNodeThatStopsBeingASocketLetsGoOfItsItem() {
        ShipSkillData data = socketed();
        SkillNode retyped = node("socket", SkillTier.SMALL, List.of("root"));

        data.forgetUnknownNodes(Map.of("root", root, "socket", retyped), Map.of());

        assertTrue(data.isAllocated("socket"));
        assertEquals(Map.of(), data.getSocketedItems());
    }

    @Test
    void socketedItemsSurviveASaveAndLoad() {
        ShipSkillData data = socketed();
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);

        ShipSkillData loaded = (ShipSkillData) xstream.fromXML(xstream.toXML(data));

        assertEquals(Map.of("socket", "socketable_1"), loaded.getSocketedItems());
    }
}
