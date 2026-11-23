package exiledsector.ui;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.ui.decoration.ResearchStationOrbit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchStationViewTest {

    private static final float STATION_Y = 950f;
    private static final float RING_RADIUS = 260f;
    private static final float TOLERANCE = 0.01f;
    private static final float FULL_SPIRAL = ResearchStationView.SPIRAL_SECONDS * 2f;
    private static final float HALF_SPIRAL = ResearchStationView.SPIRAL_SECONDS / 2f;

    private static final SkillType STATION_TYPE = new SkillType.Builder("ai_subsystem_integration", "AI Subsystem Integration", "a.png", SkillTier.NOTABLE).build();
    private static final SkillType SUBSYSTEM_TYPE = new SkillType.Builder("subsystem", "Subsystem", "a.png", SkillTier.NOTABLE).build();
    private static final SkillNode STATION = new SkillNode("ai_subsystem_integration_1", STATION_TYPE, List.of(), 0f, STATION_Y);
    private static final SkillNode EAST = subsystem("east", RING_RADIUS, 0f);
    private static final SkillNode NORTH = subsystem("north", 0f, RING_RADIUS);
    private static final SkillNode WEST = subsystem("west", -RING_RADIUS, 0f);
    private static final SkillNode OUTSIDE = new SkillNode("outside", SUBSYSTEM_TYPE, List.of(), 5000f, 0f);
    private static final TreeViewport VIEWPORT = new TreeViewport(800f, 450f, 0.5f);

    private static SkillNode subsystem(String id, float offsetX, float offsetY) {
        return new SkillNode(id, SUBSYSTEM_TYPE, List.of(), offsetX, STATION_Y + offsetY);
    }

    private static ResearchStationView view() {
        return new ResearchStationView(STATION, List.of(EAST, NORTH, WEST));
    }

    private static void assertDrawnAt(ResearchStationView view, SkillNode node, float treeX, float treeY) {
        TreeViewport shifted = view.layer().viewportFor(node, VIEWPORT);
        assertEquals(VIEWPORT.screenX(treeX), shifted.screenX(node.getX()), TOLERANCE, node.getId() + " x");
        assertEquals(VIEWPORT.screenY(treeY), shifted.screenY(node.getY()), TOLERANCE, node.getId() + " y");
    }

    @Test
    void outsideTheViewTheSubsystemsHideInsideTheStation() {
        ResearchStationView view = view();
        view.advance(FULL_SPIRAL, null);
        ResearchStationLayer layer = view.layer();

        assertEquals(0f, view.expansion());
        assertDrawnAt(view, STATION, view.focusX(), view.focusY());
        assertDrawnAt(view, EAST, view.focusX(), view.focusY());
        assertEquals(0f, layer.alphaFor(EAST));
        assertFalse(layer.isInteractive(EAST));
        assertTrue(layer.isInteractive(STATION));
        assertTrue(layer.isInteractive(OUTSIDE));
        assertEquals(1f, layer.alphaFor(OUTSIDE));
    }

    @Test
    void enteringSpiralsTheSubsystemsOutToAnEvenRingAroundTheOrbitingStation() {
        ResearchStationView view = view();
        view.enter();
        view.advance(FULL_SPIRAL, null);
        ResearchStationLayer layer = view.layer();

        assertEquals(1f, view.expansion());
        assertDrawnAt(view, EAST, view.focusX() + RING_RADIUS, view.focusY());
        assertDrawnAt(view, NORTH, view.focusX(), view.focusY() + RING_RADIUS);
        assertDrawnAt(view, WEST, view.focusX() - RING_RADIUS, view.focusY());
        assertEquals(1f, layer.alphaFor(NORTH));
        assertTrue(layer.isInteractive(NORTH));
        assertTrue(layer.isInteractive(STATION));
        assertFalse(layer.isInteractive(OUTSIDE));
        assertEquals(1f - ResearchStationView.SURROUNDING_DIM, layer.alphaFor(OUTSIDE), TOLERANCE);
    }

    @Test
    void thePickedSubsystemRestsAtTheStationCentreWhileTheOthersVanish() {
        ResearchStationView view = view();
        view.enter();
        view.advance(FULL_SPIRAL, null);
        view.advance(FULL_SPIRAL, NORTH);
        ResearchStationLayer layer = view.layer();

        assertEquals(0f, view.expansion());
        assertDrawnAt(view, NORTH, view.focusX(), view.focusY());
        assertEquals(1f, layer.alphaFor(NORTH));
        assertTrue(layer.isInteractive(NORTH));
        assertEquals(0f, layer.alphaFor(EAST));
        assertFalse(layer.isInteractive(EAST));
        assertEquals(0f, layer.alphaFor(STATION));
    }

    @Test
    void nothingInTheRingTakesInputMidSpiral() {
        ResearchStationView view = view();
        view.enter();
        view.advance(HALF_SPIRAL, null);
        ResearchStationLayer layer = view.layer();

        assertTrue(view.isSpiralling());
        assertFalse(layer.isInteractive(STATION));
        assertFalse(layer.isInteractive(EAST));
        assertFalse(layer.isInteractive(NORTH));
    }

    @Test
    void releasingThePickSpiralsTheRingBackOut() {
        ResearchStationView view = view();
        view.enter();
        view.advance(FULL_SPIRAL, NORTH);
        view.advance(FULL_SPIRAL, null);

        assertEquals(1f, view.expansion());
        assertDrawnAt(view, NORTH, view.focusX(), view.focusY() + RING_RADIUS);
        assertEquals(1f, view.layer().alphaFor(STATION));
    }

    @Test
    void theStationHitBoxScalesWithZoomButNeverShrinksBelowAFewPixels() {
        ResearchStationView view = view();
        view.advance(0f, null);
        ResearchStationLayer layer = view.layer();

        assertEquals(ResearchStationView.STATION_SIZE * ResearchStationView.HIT_SHARE, layer.stationHitHalfSize(1f), TOLERANCE);
        assertEquals(ResearchStationLayer.MIN_HIT_PIXELS, layer.stationHitHalfSize(0.001f), TOLERANCE);
        assertTrue(layer.isStation(STATION));
        assertFalse(layer.isStation(EAST));
    }
    @Test
    void aStationAlreadyIntegratedWhenTheTreeOpensIsAwakeWithoutAFlash() {
        ResearchStationView view = view();
        view.setIntegration(true, 0f);
        view.advance(0.1f, null);

        assertEquals(1f, view.wakeLevel());
        assertEquals(0f, view.flashLevel());
    }

    @Test
    void integratingFlashesTheStationAndWakesItUp() {
        ResearchStationView view = view();
        view.setIntegration(false, 0.9f);
        view.advance(0.1f, null);
        view.setIntegration(true, 0f);
        view.advance(ResearchStationView.WAKE_SECONDS / 2f, null);

        assertTrue(view.flashLevel() > 0f);
        assertEquals(0.5f, view.wakeLevel(), TOLERANCE);

        view.advance(ResearchStationView.FLASH_SECONDS, null);

        assertEquals(1f, view.wakeLevel());
        assertEquals(0f, view.flashLevel());
    }

    @Test
    void theHoldFillsTheStationFromItsHubOutToBothEndsOfItsSpine() {
        assertEquals(ResearchStationView.HUB_FRACTION, ResearchStationView.holdFillBottom(0f), TOLERANCE);
        assertEquals(ResearchStationView.HUB_FRACTION, ResearchStationView.holdFillTop(0f), TOLERANCE);
        assertEquals(ResearchStationView.SPINE_BOTTOM_FRACTION, ResearchStationView.holdFillBottom(1f), TOLERANCE);
        assertEquals(ResearchStationView.SPINE_TOP_FRACTION, ResearchStationView.holdFillTop(1f), TOLERANCE);
    }

    @Test
    void theStationKeepsOrbitingWhileTheViewAdvances() {
        ResearchStationView view = view();
        view.advance(0f, null);
        float startX = view.focusX();
        float startY = view.focusY();
        view.advance(ResearchStationOrbit.ORBIT_SECONDS / 4f, null);

        assertEquals(STATION_Y, (float) Math.hypot(view.focusX(), view.focusY()), TOLERANCE);
        assertEquals(STATION_Y * (float) Math.sqrt(2), (float) Math.hypot(view.focusX() - startX, view.focusY() - startY), 0.1f);
    }
}
