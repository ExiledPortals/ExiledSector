package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllocationPathTest {

    private static final ShipFacts FRIGATE = new ShipFacts(HullSize.FRIGATE, ShieldType.FRONT, false, 100f, hullModId -> false);
    private static final RandomGenerator LAST_OPTION = new RandomGenerator() {
        @Override
        public long nextLong() {
            return 0L;
        }

        @Override
        public int nextInt(int bound) {
            return bound - 1;
        }
    };

    private ShipSkillData data;

    @BeforeEach
    void setUp() {
        RealSkillData.clear();
        SkillNode root = node("root", type("root_type", SkillTier.ROOT).build());
        data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
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

    private static SkillNode small(String id, String... connectedTo) {
        return node(id, type(id + "_type", SkillTier.SMALL).build(), connectedTo);
    }

    private static SkillNode wormhole(String id, String pairedNodeId, String... connectedTo) {
        SkillType type = type(id + "_type", SkillTier.WORMHOLE).build();
        SkillNode node = new SkillNode(id, type, Arrays.asList(connectedTo), 0f, 0f, new SkillNodeDecoration(null, null, null, null, pairedNodeId));
        SkillTree.register(node);
        return node;
    }

    private static SkillType choiceType(String id, SkillType... options) {
        return registered(type(id, SkillTier.SMALL).choiceOptionIds(Arrays.stream(options).map(SkillType::getId).toList()).build());
    }

    private AllocationCheck gate() {
        NodeEligibility.Context eligibility = NodeEligibility.Context.of(data, FRIGATE, null, itemId -> 0);
        return new AllocationCheck(data, SkillTree.topology(), "root", new AllocationCheck.Budget(100, 2, 50), eligibility);
    }

    private List<AllocationPath.Step> pathTo(SkillNode target, SkillType preferredOption) {
        return AllocationPath.to(target, gate(), SkillTree.topology(), node -> false, preferredOption, LAST_OPTION);
    }

    private static List<String> ids(List<AllocationPath.Step> path) {
        return path.stream().map(step -> step.node().getId()).toList();
    }

    @Test
    void theShortestRouteFromTheAllocatedTreeIsTaken() {
        small("a", "root");
        small("b", "a");
        small("c", "root");
        SkillNode target = small("target", "b", "c");

        assertEquals(List.of("c", "target"), ids(pathTo(target, null)));
    }

    @Test
    void thePathStartsFromAnyAllocatedNode() {
        SkillNode a = small("a", "root");
        small("b", "a");
        SkillNode target = small("target", "b");
        ShipTrees.allocate(data, a, 2);

        assertEquals(List.of("b", "target"), ids(pathTo(target, null)));
    }

    @Test
    void thePathCrossesAWormholeWithoutAllocatingItsFarSideSeparately() {
        small("p", "root");
        wormhole("near", "far", "p", "far");
        wormhole("far", "near", "near");
        small("beyond", "far");
        small("long1", "root");
        small("long2", "long1");
        small("long3", "long2");
        small("long4", "long3");
        SkillNode target = small("target", "beyond", "long4");

        assertEquals(List.of("p", "near", "beyond", "target"), ids(pathTo(target, null)));
    }

    @Test
    void theFarSideOfAWormholeIsReachedByAllocatingTheNearSide() {
        small("p", "root");
        wormhole("near", "far", "p", "far");
        SkillNode far = wormhole("far", "near", "near");

        assertEquals(List.of("p", "near"), ids(pathTo(far, null)));
    }

    @Test
    void aWormholeCanBeTheTarget() {
        small("p", "root");
        SkillNode near = wormhole("near", "far", "p", "far");
        wormhole("far", "near", "near");

        assertEquals(List.of("p", "near"), ids(pathTo(near, null)));
    }

    @Test
    void anIneligibleNodeIsRoutedAround() {
        node("capital_only", type("capital_type", SkillTier.SMALL).requiredHullSizes(List.of(HullSize.CAPITAL_SHIP)).build(), "root");
        small("a", "root");
        small("b", "a");
        SkillNode target = small("target", "capital_only", "b");

        assertEquals(List.of("a", "b", "target"), ids(pathTo(target, null)));
    }

    @Test
    void anAlreadyAllocatedOrUnreachableTargetHasNoPath() {
        SkillNode a = small("a", "root");
        SkillNode island = small("island", "nowhere");
        ShipTrees.allocate(data, a, 2);

        assertTrue(pathTo(a, null).isEmpty());
        assertTrue(pathTo(island, null).isEmpty());
    }

    @Test
    void thePreferredOptionIsUsedWhereItFits() {
        SkillType first = registered(type("first", SkillTier.SMALL).build());
        SkillType second = registered(type("second", SkillTier.SMALL).build());
        SkillNode choice = node("choice", choiceType("choice_type", first, second), "root");

        assertEquals(first, pathTo(choice, first).get(0).option());
    }

    @Test
    void withoutAFittingPreferenceAnEarlierChoiceOfTheSameTypeIsReused() {
        SkillType first = registered(type("first", SkillTier.SMALL).build());
        SkillType second = registered(type("second", SkillTier.SMALL).build());
        SkillType unrelated = registered(type("unrelated", SkillTier.SMALL).build());
        SkillType shared = choiceType("choice_type", first, second);
        SkillNode earlier = node("earlier", shared, "root");
        SkillNode target = node("target", shared, "root");
        ShipTrees.allocate(data, earlier, first, 2);

        assertEquals(first, pathTo(target, unrelated).get(0).option());
    }

    @Test
    void withNoEarlierChoiceAnOptionIsRolledAndReusedAlongThePath() {
        SkillType first = registered(type("first", SkillTier.SMALL).build());
        SkillType second = registered(type("second", SkillTier.SMALL).build());
        SkillType shared = choiceType("choice_type", first, second);
        node("a", shared, "root");
        SkillNode target = node("target", shared, "a");

        RandomGenerator countingUp = new RandomGenerator() {
            private int calls;

            @Override
            public long nextLong() {
                return 0L;
            }

            @Override
            public int nextInt(int bound) {
                return calls++ % bound;
            }
        };

        List<AllocationPath.Step> path = AllocationPath.to(target, gate(), SkillTree.topology(), node -> false, null, countingUp);

        assertEquals(List.of(first, first), path.stream().map(AllocationPath.Step::option).toList());
    }

    @Test
    void plainNodesCarryNoOption() {
        SkillNode target = small("target", "root");

        assertNull(pathTo(target, null).get(0).option());
    }
}
