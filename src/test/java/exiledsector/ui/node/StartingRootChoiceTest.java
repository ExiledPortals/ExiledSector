package exiledsector.ui.node;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartingRootChoiceTest {

    private static final float EPSILON = 0.001f;

    private static SkillNode node(String id, SkillTier tier, float x, float y) {
        SkillType type = new SkillType.Builder(id, id, "a.png", tier)
                .effects(List.of())
                .build();
        return new SkillNode(id, type, List.of(), x, y);
    }

    private final SkillNode lowTech = node("root_low_tech_1", SkillTier.ROOT, 1082f, 616f);
    private final SkillNode midline = node("root_midline_1", SkillTier.ROOT, -1078f, 623f);
    private final SkillNode highTech = node("root_high_tech_1", SkillTier.ROOT, 0f, -1245f);
    private final SkillNode small = node("small_1", SkillTier.SMALL, 500f, 500f);

    private StartingRootChoice pending() {
        return StartingRootChoice.pending(List.of(lowTech, midline, highTech));
    }

    @Test
    void pendingChoiceClustersEachRootNearTheSunInTheDirectionOfItsRealPosition() {
        StartingRootChoice choice = pending();

        assertEquals(StartingRootChoice.CLUSTER_RADIUS, (float) Math.hypot(choice.offsetX(highTech), choice.offsetY(highTech)), EPSILON);
        assertEquals(0f, choice.offsetX(highTech), EPSILON);
        assertTrue(choice.offsetY(highTech) < 0f);
        assertTrue(choice.offsetX(lowTech) > 0f);
        assertTrue(choice.offsetX(midline) < 0f);
    }

    @Test
    void nonRootNodesKeepTheirRealPositionAndTheTreeIsDimmedWhileChoosing() {
        StartingRootChoice choice = pending();

        assertEquals(500f, choice.offsetX(small), EPSILON);
        assertEquals(500f, choice.offsetY(small), EPSILON);
        assertEquals(NodeSearch.DIM_ALPHA, choice.treeAlpha(), EPSILON);
        assertTrue(choice.isInputLocked());
        assertNull(choice.chosen());
    }

    @Test
    void choosingARootFliesEveryRootToItsRealPositionThenUnlocks() {
        StartingRootChoice choice = pending();
        choice.choose(highTech);

        assertEquals(StartingRootChoice.Phase.FLYING, choice.phase());
        assertSame(highTech, choice.chosen());

        choice.advance(StartingRootChoice.FLIGHT_SECONDS / 2f);
        assertTrue(choice.offsetY(highTech) < 0f && choice.offsetY(highTech) > -1245f);
        assertTrue(choice.treeAlpha() > NodeSearch.DIM_ALPHA && choice.treeAlpha() < 1f);
        assertTrue(choice.isInputLocked());

        choice.advance(StartingRootChoice.FLIGHT_SECONDS);
        assertEquals(StartingRootChoice.Phase.CHOSEN, choice.phase());
        assertFalse(choice.isInputLocked());
        assertEquals(1f, choice.treeAlpha(), EPSILON);
        assertEquals(1082f, choice.offsetX(lowTech), EPSILON);
        assertEquals(623f, choice.offsetY(midline), EPSILON);
        assertEquals(-1245f, choice.offsetY(highTech), EPSILON);
    }

    @Test
    void flyingOutTheCameraHeadsForTheChosenRootWithEasedProgress() {
        StartingRootChoice choice = pending();
        choice.choose(lowTech);
        assertEquals(0f, choice.cameraProgress(), EPSILON);
        assertTrue(choice.isMoving());

        choice.advance(StartingRootChoice.FLIGHT_SECONDS / 2f);
        assertEquals(0.5f, choice.cameraProgress(), EPSILON);
        assertEquals(choice.offsetX(lowTech), choice.cameraTargetX(), EPSILON);
        assertEquals(choice.offsetY(lowTech), choice.cameraTargetY(), EPSILON);

        choice.advance(StartingRootChoice.FLIGHT_SECONDS);
        assertEquals(1f, choice.cameraProgress(), EPSILON);
        assertEquals(lowTech.getOffsetX(), choice.cameraTargetX(), EPSILON);
        assertFalse(choice.isMoving());
    }

    @Test
    void returningFliesEveryRootBackIntoTheClusterAndReopensTheChoice() {
        StartingRootChoice clustered = pending();
        StartingRootChoice choice = StartingRootChoice.returning(List.of(lowTech, midline, highTech), lowTech);

        assertEquals(StartingRootChoice.Phase.RETURNING, choice.phase());
        assertTrue(choice.isInputLocked());
        assertEquals(lowTech.getOffsetX(), choice.offsetX(lowTech), EPSILON);
        assertEquals(1f, choice.treeAlpha(), EPSILON);
        assertNull(choice.rootForAllocation());

        choice.advance(StartingRootChoice.FLIGHT_SECONDS);

        assertEquals(StartingRootChoice.Phase.CHOOSING, choice.phase());
        assertNull(choice.chosen());
        assertEquals(clustered.offsetX(highTech), choice.offsetX(highTech), EPSILON);
        assertEquals(clustered.offsetY(highTech), choice.offsetY(highTech), EPSILON);
        assertEquals(NodeSearch.DIM_ALPHA, choice.treeAlpha(), EPSILON);
        choice.choose(midline);
        assertEquals(midline, choice.chosen());
    }

    @Test
    void returningPansTheCameraBackToTheTreeCentreWithEasedProgress() {
        StartingRootChoice choice = StartingRootChoice.returning(List.of(lowTech, midline, highTech), lowTech);
        assertEquals(0f, choice.cameraProgress(), EPSILON);
        assertEquals(0f, choice.cameraTargetX(), EPSILON);
        assertEquals(0f, choice.cameraTargetY(), EPSILON);

        choice.advance(StartingRootChoice.FLIGHT_SECONDS / 2f);
        assertEquals(0.5f, choice.cameraProgress(), EPSILON);

        choice.advance(StartingRootChoice.FLIGHT_SECONDS);
        assertEquals(1f, choice.cameraProgress(), EPSILON);
        assertEquals(0f, choice.cameraTargetX(), EPSILON);
    }

    @Test
    void onlyAFlyingOrChosenRootCountsAsTheAllocatorsRoot() {
        StartingRootChoice choice = pending();
        assertNull(choice.rootForAllocation());

        choice.choose(lowTech);
        assertEquals(lowTech, choice.rootForAllocation());
        choice.advance(StartingRootChoice.FLIGHT_SECONDS);
        assertEquals(lowTech, choice.rootForAllocation());
    }

    @Test
    void aSecondChoiceIsIgnored() {
        StartingRootChoice choice = pending();
        choice.choose(highTech);
        choice.choose(lowTech);

        assertSame(highTech, choice.chosen());
    }

    @Test
    void aNodeOutsideTheClusterCannotBeChosen() {
        StartingRootChoice choice = pending();
        choice.choose(small);

        assertEquals(StartingRootChoice.Phase.CHOOSING, choice.phase());
        assertNull(choice.chosen());
    }

    @Test
    void anAlreadyChosenRootStartsUnlockedAtItsRealPosition() {
        StartingRootChoice choice = StartingRootChoice.alreadyChosen(lowTech);

        assertEquals(StartingRootChoice.Phase.CHOSEN, choice.phase());
        assertFalse(choice.isInputLocked());
        assertEquals(1082f, choice.offsetX(lowTech), EPSILON);
        assertEquals(1f, choice.treeAlpha(), EPSILON);
    }

    @Test
    void aTreeWithoutRootsNeverLocksInput() {
        assertFalse(StartingRootChoice.pending(List.of()).isInputLocked());
    }

    @Test
    void thePromptSitsAboveTheHighestClusteredRoot() {
        StartingRootChoice choice = pending();

        float expected = choice.offsetY(highTech) - StartingRootChoice.ROOT_FOOTPRINT / 2f - StartingRootChoice.PROMPT_GAP;
        assertEquals(expected, choice.promptOffsetY(), EPSILON);
    }

    @Test
    void rootsSittingOnTheSunAreSpreadEvenly() {
        SkillNode a = node("a", SkillTier.ROOT, 0f, 0f);
        SkillNode b = node("b", SkillTier.ROOT, 0f, 0f);
        StartingRootChoice choice = StartingRootChoice.pending(List.of(a, b));

        assertEquals(-choice.offsetX(a), choice.offsetX(b), EPSILON);
        assertEquals(StartingRootChoice.CLUSTER_RADIUS, Math.abs(choice.offsetX(a)), EPSILON);
    }
}
