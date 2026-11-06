package exiledsector.skills.template;

import exiledsector.MockSector;
import exiledsector.items.Item;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TemplateAnchorsTest {

    private static final String ANCHOR_ID = "wormhole_anchor_abyss";

    private MockSector mockSector;
    private SkillNode root;
    private SkillNode socket;
    private SkillNode secondSocket;
    private final List<String> socketedIds = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        SkillTree.clearNodes();
        root = node("root", SkillTier.ROOT);
        socket = node("socket_a", SkillTier.SOCKET, "root");
        secondSocket = node("socket_b", SkillTier.SOCKET, "root");
        node("abyss_exit", SkillTier.WORMHOLE_EXIT);
        WormholeAnchorDefinitions.register(new JSONArray().put(new JSONObject().put("id", ANCHOR_ID).put("volume", "abyss")
                .put("exit_node", "abyss_exit").put("name", "Abyss Wormhole Anchor").put("description", "Opens a wormhole.")));
    }

    @AfterEach
    void tearDown() {
        WormholeAnchorDefinitions.clear();
        ItemDefinitions.clear();
        SkillTree.clearNodes();
        mockSector.close();
    }

    private static SkillNode node(String id, SkillTier tier, String... connectedTo) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).effects(List.of()).build();
        SkillNode node = new SkillNode(id, type, List.of(connectedTo), 0f, 0f, SkillNodeDecoration.NONE, List.of("core"));
        SkillTree.register(node);
        return node;
    }

    private ShipSkillData ship(String shipId, SkillNode... allocated) {
        ShipSkillData shipData = new ShipSkillData();
        ShipTrees.chooseStartingRoot(shipData, root);
        for (SkillNode node : allocated) {
            ShipTrees.allocate(shipData, node, 1);
        }
        ShipSkillDataManager.put(shipId, shipData);
        return shipData;
    }

    private static Item storedAnchor() {
        return ItemStore.get().add(ItemDefinitions.get(ANCHOR_ID), 3L);
    }

    private TemplateAnchors anchorsFor(SkillTreeTemplate template, ShipSkillData shipData) {
        return TemplateAnchors.fromStorage(template, (socketNode, anchor) -> {
            socketedIds.add(anchor.id());
            return shipData.socketItem(socketNode.getId(), anchor.id());
        });
    }

    private static SkillTreeTemplate template(String... socketIds) {
        List<TemplateStep> steps = new ArrayList<>();
        for (String socketId : socketIds) {
            steps.add(new TemplateStep(socketId, null));
        }
        return new SkillTreeTemplate("t1", "Abyss", "root", null, steps, Map.of(socketIds[0], ANCHOR_ID));
    }

    @Test
    void savingATemplateRecordsWhichAnchorEachSocketHolds() {
        ShipSkillData shipData = ship("ship-a", socket, secondSocket);
        Item anchor = storedAnchor();
        shipData.socketItem("socket_a", anchor.id());
        shipData.socketItem("socket_b", "some_subroutine");

        assertEquals(Map.of("socket_a", ANCHOR_ID), TemplateAnchors.socketedOn(shipData));
    }

    @Test
    void anAllocatedEmptySocketTakesAnUnusedAnchorFromStorage() {
        ShipSkillData shipData = ship("ship-a", socket);
        Item anchor = storedAnchor();
        TemplateAnchors anchors = anchorsFor(template("socket_a"), shipData);

        anchors.fill(shipData);

        assertEquals(anchor.id(), shipData.getSocketedItem("socket_a"));
        assertEquals(1, anchors.socketedCount());
    }

    @Test
    void anAnchorAlreadyInstalledOnAnotherShipIsLeftThere() {
        ShipSkillData otherShip = ship("ship-b", socket);
        otherShip.socketItem("socket_a", storedAnchor().id());
        ShipSkillData shipData = ship("ship-a", socket);
        TemplateAnchors anchors = anchorsFor(template("socket_a"), shipData);

        anchors.fill(shipData);

        assertNull(shipData.getSocketedItem("socket_a"));
        assertEquals(0, anchors.socketedCount());
    }

    @Test
    void aFilledSocketOrAVolumeAlreadyAnchoredOnTheShipIsLeftAlone() {
        ShipSkillData shipData = ship("ship-a", socket, secondSocket);
        Item onBoard = storedAnchor();
        storedAnchor();
        shipData.socketItem("socket_b", onBoard.id());
        TemplateAnchors anchors = anchorsFor(template("socket_a", "socket_b"), shipData);

        anchors.fill(shipData);

        assertNull(shipData.getSocketedItem("socket_a"));
        assertEquals(onBoard.id(), shipData.getSocketedItem("socket_b"));
        assertEquals(0, anchors.socketedCount());
    }

    @Test
    void aSocketAllocatedDuringTheRunIsFilledStraightAway() {
        ShipSkillData shipData = ship("ship-a");
        Item anchor = storedAnchor();
        SkillTreeTemplate template = template("socket_a");
        TemplateAnchors anchors = anchorsFor(template, shipData);
        List<String> socketedWhenNextStepRan = new ArrayList<>();

        AutoAllocateRun.run(List.of(new TemplateStep("socket_a", null), new TemplateStep("after", null)), anchors.fillingAfter(step -> {
            if (step.nodeId().equals("socket_a")) {
                ShipTrees.allocate(shipData, socket, 1);
                return StepVerdict.ALLOCATE;
            }
            socketedWhenNextStepRan.add(shipData.getSocketedItem("socket_a"));
            return StepVerdict.UNKNOWN_NODE;
        }, shipData), () -> true);

        assertEquals(List.of(anchor.id()), socketedWhenNextStepRan);
        assertEquals(List.of(anchor.id()), socketedIds);
    }

    @Test
    void anchorsOnSocketsOutsideTheStepsAreDropped() {
        SkillTreeTemplate template = new SkillTreeTemplate("t1", "Abyss", "root", null, List.of(new TemplateStep("socket_a", null)),
                Map.of("socket_a", ANCHOR_ID, "socket_b", ANCHOR_ID));

        assertEquals(Map.of("socket_a", ANCHOR_ID), template.anchorsBySocket());
    }

    @Test
    void storageListsOnlyAnchorsNoShipHolds() {
        ShipSkillData shipData = ship("ship-a", socket);
        Item installed = storedAnchor();
        Item spare = storedAnchor();
        shipData.socketItem("socket_a", installed.id());

        assertEquals(List.of(spare), TemplateAnchors.unusedInStorage());
    }
}
