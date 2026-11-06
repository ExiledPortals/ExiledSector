package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnchorRouteTest {

    private static final ShipFacts FRIGATE = new ShipFacts(HullSize.FRIGATE, ShieldType.FRONT, false, 100f, hullModId -> false);
    private static final BiPredicate<SkillNode, AnchorRoute.Anchor> ANY_SOCKET = (socket, anchor) -> true;

    private ShipSkillData data;

    @BeforeEach
    void setUp() {
        RealSkillData.clear();
        SkillNode root = node("root", SkillTier.ROOT);
        data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static SkillNode node(String id, SkillTier tier, String... connectedTo) {
        SkillType type = new SkillType.Builder(id + "_type", id + " name", "a.png", tier).build();
        SkillNode node = new SkillNode(id, type, Arrays.asList(connectedTo), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    private static SkillNode small(String id, String... connectedTo) {
        return node(id, SkillTier.SMALL, connectedTo);
    }

    private static SkillNode socket(String id, String... connectedTo) {
        return node(id, SkillTier.SOCKET, connectedTo);
    }

    private AnchorRoute.Route routeTo(SkillNode target, BiPredicate<SkillNode, AnchorRoute.Anchor> socketAccepts, AnchorRoute.Anchor... anchors) {
        NodeEligibility.Context eligibility = NodeEligibility.Context.of(data, FRIGATE, null, itemId -> 0);
        AllocationCheck gate = new AllocationCheck(data, SkillTree.topology(), "root", new AllocationCheck.Budget(100, 2, 50), eligibility);
        return AnchorRoute.cheapest(target, List.of(anchors), socketAccepts, gate, SkillTree.topology(), node -> false);
    }

    private void buildExitRegion() {
        node("exit", SkillTier.WORMHOLE_EXIT, "beyond");
        small("beyond", "exit");
    }

    @Test
    void theSocketClosestToTheTreeTakesTheAnchorWhoseExitLeadsToTheTarget() {
        small("a", "root");
        small("b", "a");
        socket("far_socket", "b");
        socket("near_socket", "root");
        buildExitRegion();
        SkillNode target = small("target", "beyond");
        AnchorRoute.Anchor anchor = new AnchorRoute.Anchor("anchor-1", "exit");

        AnchorRoute.Route route = routeTo(target, ANY_SOCKET, anchor);

        assertEquals(anchor, route.anchor());
        assertEquals("near_socket", route.socket().getId());
    }

    @Test
    void anAllocatedEmptySocketBeatsAnyUnallocatedOne() {
        SkillNode a = small("a", "root");
        SkillNode allocatedSocket = socket("allocated_socket", "a");
        socket("open_socket", "root");
        ShipTrees.allocate(data, a, 2);
        ShipTrees.allocate(data, allocatedSocket, 2);
        buildExitRegion();
        SkillNode target = small("target", "beyond");

        AnchorRoute.Route route = routeTo(target, ANY_SOCKET, new AnchorRoute.Anchor("anchor-1", "exit"));

        assertEquals("allocated_socket", route.socket().getId());
    }

    @Test
    void anOccupiedOrRefusingSocketIsPassedOver() {
        SkillNode occupied = socket("occupied", "root");
        ShipTrees.allocate(data, occupied, 2);
        data.socketItem("occupied", "other-item");
        socket("refusing", "root");
        small("a", "root");
        socket("accepting", "a");
        buildExitRegion();
        SkillNode target = small("target", "beyond");

        AnchorRoute.Route route = routeTo(target, (socket, anchor) -> !socket.getId().equals("refusing"), new AnchorRoute.Anchor("anchor-1", "exit"));

        assertEquals("accepting", route.socket().getId());
    }

    @Test
    void theAnchorWithTheShorterWayToTheTargetIsChosen() {
        socket("socket", "root");
        node("long_exit", SkillTier.WORMHOLE_EXIT, "long1");
        small("long1", "long_exit");
        small("long2", "long1");
        node("short_exit", SkillTier.WORMHOLE_EXIT, "target");
        SkillNode target = small("target", "long2", "short_exit");
        AnchorRoute.Anchor shortAnchor = new AnchorRoute.Anchor("short", "short_exit");

        AnchorRoute.Route route = routeTo(target, ANY_SOCKET, new AnchorRoute.Anchor("long", "long_exit"), shortAnchor);

        assertEquals(shortAnchor, route.anchor());
    }

    @Test
    void anExitCanBeTheTarget() {
        socket("socket", "root");
        SkillNode exit = node("exit", SkillTier.WORMHOLE_EXIT);
        AnchorRoute.Anchor anchor = new AnchorRoute.Anchor("anchor-1", "exit");

        assertEquals(anchor, routeTo(exit, ANY_SOCKET, anchor).anchor());
    }

    @Test
    void withoutAUsableAnchorOrSocketThereIsNoRoute() {
        buildExitRegion();
        node("unrelated_exit", SkillTier.WORMHOLE_EXIT);
        SkillNode target = small("target", "beyond");

        assertNull(routeTo(target, ANY_SOCKET));
        assertNull(routeTo(target, ANY_SOCKET, new AnchorRoute.Anchor("anchor-1", "exit")));
        socket("socket", "root");
        assertNull(routeTo(target, ANY_SOCKET, new AnchorRoute.Anchor("anchor-2", "unrelated_exit")));
        assertNull(routeTo(target, (socket, anchor) -> false, new AnchorRoute.Anchor("anchor-1", "exit")));
    }
}
