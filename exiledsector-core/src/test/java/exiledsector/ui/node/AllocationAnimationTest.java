package exiledsector.ui.node;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllocationAnimationTest {

    private static final float EPSILON = 1e-4f;

    private final Map<String, List<String>> neighboursById = new HashMap<>();
    private final Set<String> satisfied = new HashSet<>();
    private final List<String> started = new ArrayList<>();
    private final List<String> lit = new ArrayList<>();
    private final AllocationAnimation animation = new AllocationAnimation(new AllocationAnimation.Tree() {
        @Override
        public List<String> neighbours(String nodeId) {
            return neighboursById.getOrDefault(nodeId, List.of());
        }

        @Override
        public boolean isSatisfied(String nodeId) {
            return satisfied.contains(nodeId);
        }
    }, new AllocationAnimation.Listener() {
        @Override
        public void started(String nodeId, boolean allocating) {
            started.add(nodeId);
        }

        @Override
        public void lit(String nodeId) {
            lit.add(nodeId);
        }
    });

    private void link(String a, String b) {
        neighboursById.computeIfAbsent(a, key -> new ArrayList<>()).add(b);
        neighboursById.computeIfAbsent(b, key -> new ArrayList<>()).add(a);
    }

    private List<String> chain(int length) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            ids.add("n" + i);
            if (i > 0) {
                link("n" + (i - 1), "n" + i);
            }
        }
        return ids;
    }

    private boolean shownLit(String nodeId) {
        return animation.shown(nodeId, satisfied.contains(nodeId));
    }

    private float secondsUntilIdle(float frameSeconds) {
        float elapsed = 0f;
        while (!animation.isEmpty() && elapsed < 60f) {
            animation.advance(frameSeconds);
            elapsed += frameSeconds;
        }
        return elapsed;
    }

    @Test
    void aNewNodeStaysDarkWhileItsConnectorFillsAndLightsWhenTheFillArrives() {
        link("old", "new");
        satisfied.add("old");
        satisfied.add("new");

        animation.allocated(List.of("new"), "new");
        assertFalse(shownLit("new"));

        animation.advance(0.25f * AllocationAnimation.FILL_SECONDS);
        assertEquals(List.of("new"), started);
        assertFalse(shownLit("new"));
        AllocationAnimation.FillRange fromOld = animation.animatedRange("old", "new");
        assertEquals(0f, fromOld.startFraction(), EPSILON);
        assertEquals(0.25f, fromOld.endFraction(), EPSILON);
        AllocationAnimation.FillRange fromNew = animation.animatedRange("new", "old");
        assertEquals(0.75f, fromNew.startFraction(), EPSILON);
        assertEquals(1f, fromNew.endFraction(), EPSILON);
        assertTrue(lit.isEmpty());

        animation.advance(0.75f * AllocationAnimation.FILL_SECONDS);
        assertEquals(List.of("new"), lit);
        assertTrue(shownLit("new"));

        animation.advance(0.01f);
        assertNull(animation.animatedRange("old", "new"));
        assertTrue(animation.isEmpty());
    }

    @Test
    void aNewNodeWithNoLitNeighbourLightsAtOnce() {
        satisfied.add("root");

        animation.allocated(List.of("root"), "root");
        animation.advance(0f);

        assertEquals(List.of("root"), lit);
        assertTrue(shownLit("root"));
        assertTrue(animation.isEmpty());
    }

    @Test
    void aReleasedCascadeGoesDarkRingByRingFromTheFarEndAndDrainsTowardsWhatIsLeft() {
        List<String> ids = chain(4);
        satisfied.add("n0");

        animation.deallocated(List.of("n1", "n2", "n3"), "n1");
        float ringSeconds = AllocationAnimation.ringSeconds(3);
        assertTrue(shownLit("n1"));
        assertTrue(shownLit("n2"));
        assertTrue(shownLit("n3"));

        animation.advance(0f);
        assertEquals(List.of("n3"), started);
        assertFalse(shownLit("n3"));
        assertTrue(shownLit("n2"));
        assertEquals(new AllocationAnimation.FillRange(0f, 1f), animation.animatedRange("n2", "n3"));

        animation.advance(ringSeconds);
        assertEquals(List.of("n3", "n2"), started);
        assertFalse(shownLit("n2"));
        AllocationAnimation.FillRange stillDraining = animation.animatedRange("n2", "n3");
        assertEquals(1f - ringSeconds / AllocationAnimation.FILL_SECONDS, stillDraining.endFraction(), EPSILON);

        animation.advance(ringSeconds);
        assertEquals(List.of("n3", "n2", "n1"), started);
        assertFalse(shownLit(ids.get(1)));
        assertTrue(shownLit("n0"));

        animation.advance(0.5f * AllocationAnimation.FILL_SECONDS);
        AllocationAnimation.FillRange drain = animation.animatedRange("n0", "n1");
        assertEquals(0f, drain.startFraction(), EPSILON);
        assertEquals(0.5f, drain.endFraction(), EPSILON);
    }

    @Test
    void anAllocatedPathLightsOutwardsFromTheTreeRingByRing() {
        chain(4);
        satisfied.addAll(List.of("n0", "n1", "n2", "n3"));

        animation.allocated(List.of("n3", "n2", "n1"), "n3");
        animation.advance(0f);

        assertEquals(List.of("n1"), started);
        assertFalse(shownLit("n2"));
        animation.advance(AllocationAnimation.ringSeconds(3));
        assertEquals(List.of("n1", "n2"), started);
        assertTrue(animation.animatedRange("n1", "n2") != null);
    }

    @Test
    void everyRunFinishesWithinThreeSecondsHoweverManyNodesItTouches() {
        for (int length : new int[]{2, 5, 20, 80, 300}) {
            for (float frameSeconds : new float[]{1f / 60f, 1f / 15f}) {
                neighboursById.clear();
                satisfied.clear();
                List<String> ids = chain(length);
                satisfied.add("n0");
                animation.deallocated(ids.subList(1, ids.size()), "n1");
                assertTrue(secondsUntilIdle(frameSeconds) <= AllocationAnimation.MAX_TOTAL_SECONDS + frameSeconds,
                        length + " released at " + frameSeconds);

                satisfied.addAll(ids);
                animation.allocated(ids.subList(1, ids.size()), "n1");
                assertTrue(secondsUntilIdle(frameSeconds) <= AllocationAnimation.MAX_TOTAL_SECONDS + frameSeconds,
                        length + " allocated at " + frameSeconds);
            }
        }
    }

    @Test
    void ringsAreSpacedSoTheLastOneFinishesInsideTheCap() {
        assertEquals(0f, AllocationAnimation.ringSeconds(1), EPSILON);
        assertEquals(AllocationAnimation.BASE_RING_SECONDS, AllocationAnimation.ringSeconds(3), EPSILON);
        float manyRings = AllocationAnimation.ringSeconds(101);
        assertEquals(AllocationAnimation.MAX_TOTAL_SECONDS, 100 * manyRings + AllocationAnimation.FILL_SECONDS, EPSILON);
    }

    @Test
    void reallocatingANodeStillWaitingToGoDarkJustKeepsItLit() {
        chain(3);
        satisfied.add("n0");

        animation.deallocated(List.of("n1", "n2"), "n1");
        satisfied.add("n1");
        animation.allocated(List.of("n1"), "n1");

        assertTrue(shownLit("n1"));
        assertTrue(shownLit("n2"));
        animation.advance(AllocationAnimation.MAX_TOTAL_SECONDS);
        assertTrue(shownLit("n1"));
        assertFalse(shownLit("n2"));
        assertEquals(List.of("n2"), started);
    }

    @Test
    void releasingANodeThatHasNotLitYetDropsItWithoutAFlash() {
        chain(3);
        satisfied.addAll(List.of("n0", "n1", "n2"));
        animation.allocated(List.of("n1", "n2"), "n2");
        animation.advance(0f);

        satisfied.removeAll(List.of("n1", "n2"));
        animation.deallocated(List.of("n1", "n2"), "n1");

        assertFalse(shownLit("n1"));
        assertFalse(shownLit("n2"));
        assertNull(animation.animatedRange("n0", "n1"));
        assertTrue(animation.isEmpty());
    }

    @Test
    void releasedNodesTheRingSearchCannotReachGoDarkFirst() {
        chain(3);
        satisfied.add("n0");

        animation.deallocated(List.of("n1", "n2", "island"), "n1");
        animation.advance(0f);

        assertEquals(List.of("island"), started);
        assertTrue(shownLit("n2"));
    }
}
