package exiledsector.skills;

import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.unlock.UnlockCondition;
import exiledsector.skills.unlock.UnlockConditionType;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExoshipDataTest {

    private static SkillNode exoship;
    private static SkillNode beacon;

    @BeforeAll
    static void loadRealTree() throws Exception {
        RealSkillData.load();
        exoship = ExoshipNodes.exoship();
        beacon = exoship == null ? null : SkillTree.get(exoship.getPairedNodeId());
    }

    @AfterAll
    static void clear() {
        RealSkillData.clear();
    }

    @Test
    void theTreeHasOneExoshipPairedWithOneHyperNavBeacon() {
        long exoships = SkillTree.getAllNodes().values().stream().filter(ExoshipNodes::isExoship).count();
        long beacons = SkillTree.getAllNodes().values().stream().filter(ExoshipNodes::isBeacon).count();

        assertEquals(1, exoships);
        assertEquals(1, beacons);
        assertNotNull(beacon);
        assertTrue(ExoshipNodes.isBeacon(beacon));
        assertEquals(exoship.getId(), beacon.getPairedNodeId());
        assertTrue(ExoshipNodes.isCarried(exoship));
        assertFalse(ExoshipNodes.isCarried(beacon));
    }

    @Test
    void bothEndsOfTheDockingLinkWaitForThePlayerToOwnAnExoshipInRat() {
        for (SkillNode end : List.of(beacon, exoship)) {
            assertEquals(List.of(UnlockConditionType.RAT_PLAYER_EXOSHIP),
                    end.getType().getUnlockConditions().stream().map(UnlockCondition::getType).toList(), end.getId());
        }
    }

    @Test
    void theBeaconIsTheOnlyWayOntoTheExoship() {
        List<String> leaks = new ArrayList<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            for (String connectedId : node.getConnectedNodeIds()) {
                SkillNode connected = SkillTree.get(connectedId);
                boolean crossesOver = ExoshipNodes.isCarried(node) != ExoshipNodes.isCarried(connected);
                boolean dockingLink = node == exoship && connected == beacon || node == beacon && connected == exoship;
                if (crossesOver && !dockingLink) {
                    leaks.add(node.getId() + " -> " + connectedId);
                }
            }
        }

        assertTrue(leaks.isEmpty(), "Links onto the Exoship that skip the beacon: " + leaks);
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
    void allocatingTheBeaconDocksTheExoshipForFreeAndOpensItsBranches() {
        ShipSkillData shipData = new ShipSkillData();

        shipData.allocate(beacon, 1);

        assertTrue(shipData.isAllocated(exoship.getId()));
        List<SkillNode> hub = exoship.getConnectedNodeIds().stream().map(SkillTree::get).filter(ExoshipNodes::isCarried).toList();
        assertFalse(hub.isEmpty());
        for (SkillNode hubNode : hub) {
            assertTrue(hubNode.getConnectedNodeIds().stream().anyMatch(id -> shipData.isSatisfied(id, null)), hubNode.getId());
        }
    }
}
