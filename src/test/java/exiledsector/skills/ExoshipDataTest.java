package exiledsector.skills;

import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExoshipDataTest {

    private static final String BEACON_ITEM_ID = "socketable_beacon";
    private static SkillNode exoship;

    @BeforeAll
    static void loadRealTree() throws Exception {
        RealSkillData.load();
        exoship = ExoshipNodes.exoship();
    }

    @AfterAll
    static void clear() {
        AnchorLinks.install(null);
        RealSkillData.clear();
    }

    @Test
    void theTreeHasOneExoshipAndItIsTheExoTechExit() {
        long exoships = SkillTree.getAllNodes().values().stream().filter(ExoshipNodes::isExoship).count();

        assertEquals(1, exoships);
        assertEquals(SkillTier.WORMHOLE_EXIT, exoship.getType().getTier());
        assertNull(exoship.getPairedNodeId());
        assertTrue(ExoshipNodes.isCarried(exoship));
        assertNull(SkillTree.getType("exotech_beacon"));
    }

    @Test
    void theExoshipNodeIsNotGatedBecauseTheBeaconAnchorCarriesTheRatUnlock() {
        assertEquals(List.of(), exoship.getType().getUnlockConditions());
    }

    @Test
    void noPathLeadsOntoTheExoship() {
        List<String> leaks = new ArrayList<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            for (String connectedId : node.getConnectedNodeIds()) {
                SkillNode connected = SkillTree.get(connectedId);
                if (ExoshipNodes.isCarried(node) != ExoshipNodes.isCarried(connected)) {
                    leaks.add(node.getId() + " -> " + connectedId);
                }
            }
        }

        assertTrue(leaks.isEmpty(), "Links onto the Exoship from outside it: " + leaks);
    }

    @Test
    void everyCarriedNodeCanBeReachedFromTheExoship() {
        Set<String> reached = new HashSet<>();
        Deque<SkillNode> frontier = new ArrayDeque<>(List.of(exoship));
        while (!frontier.isEmpty()) {
            SkillNode current = frontier.poll();
            if (!reached.add(current.getId())) {
                continue;
            }
            for (String connectedId : current.getConnectedNodeIds()) {
                SkillNode connected = SkillTree.get(connectedId);
                if (ExoshipNodes.isCarried(connected)) {
                    frontier.add(connected);
                }
            }
        }

        List<String> stranded = SkillTree.getAllNodes().values().stream().filter(ExoshipNodes::isCarried)
                .map(SkillNode::getId).filter(id -> !reached.contains(id)).toList();
        assertTrue(stranded.isEmpty(), "Carried nodes the Exoship can't reach: " + stranded);
    }

    @Test
    void socketingTheHyperNavBeaconDocksTheExoshipForFreeAndOpensItsBranches() {
        AnchorLinks.install(new AnchorLinks.ExitLookup() {
            @Override
            public String exitNodeId(String socketedItemId) {
                return BEACON_ITEM_ID.equals(socketedItemId) ? exoship.getId() : null;
            }

            @Override
            public boolean canOpen(String socketedItemId) {
                return true;
            }

            @Override
            public int revision() {
                return 0;
            }
        });
        SkillNode socket = SkillTree.getAllNodes().values().stream().filter(node -> node.getType().getTier() == SkillTier.SOCKET)
                .findFirst().orElseThrow();
        ShipSkillData shipData = new ShipSkillData();
        shipData.allocate(socket, 1);

        shipData.socketItem(socket.getId(), BEACON_ITEM_ID);
        shipData.openAnchorWormholes(Integer.MAX_VALUE);

        assertTrue(shipData.isAllocated(exoship.getId()));
        assertTrue(shipData.isFreeNode(exoship.getId()));
        assertEquals(exoship.getId(), shipData.wormholePartner(socket));
        assertEquals(socket.getId(), shipData.wormholePartner(exoship));
        List<SkillNode> hub = exoship.getConnectedNodeIds().stream().map(SkillTree::get).filter(ExoshipNodes::isCarried).toList();
        assertFalse(hub.isEmpty());
        for (SkillNode hubNode : hub) {
            assertTrue(hubNode.getConnectedNodeIds().stream().anyMatch(id -> shipData.isSatisfied(id, null)), hubNode.getId());
        }
    }
}
