package exiledsector.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.tags.SkillTags;
import exiledsector.ui.exoship.ExoshipVoyage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExoshipViewTest {

    private static final SkillType SMALL = new SkillType.Builder("small", "Small", "a.png", SkillTier.SMALL).build();
    private static final SkillType NOTABLE = new SkillType.Builder("notable", "Notable", "a.png", SkillTier.NOTABLE).build();
    private static final SkillType EXOSHIP = new SkillType.Builder(ExoshipNodes.EXOSHIP_TYPE_ID, "Exoship", "", SkillTier.WORMHOLE).build();

    private static final SkillNode EXOSHIP_NODE = node("exoship", EXOSHIP, -8000f, -15000f, SkillTags.EXOTECH_REGION);
    private static final SkillNode CARRIED_NODE = node("carried", SMALL, -7000f, -15000f, SkillTags.EXOTECH_REGION);
    private static final SkillNode CARRIED_NOTABLE = node("carried_notable", NOTABLE, -8000f, -14500f, SkillTags.EXOTECH_REGION);
    private static final SkillNode CORE_NOTABLE = node("core_notable", NOTABLE, 0f, 0f, SkillTags.CORE_REGION);
    private static final SkillNode FAR_NOTABLE = node("far_notable", NOTABLE, 9000f, 0f, "pirate");
    private static final SkillNode ABYSS_NOTABLE = node("abyss_notable", NOTABLE, -45000f, 27000f, SkillTags.ABYSS_REGION);
    private static final SkillNode CORE_SMALL = node("core_small", SMALL, 100f, 100f, SkillTags.CORE_REGION);
    private static final List<SkillNode> NODES = List.of(EXOSHIP_NODE, CARRIED_NODE, CARRIED_NOTABLE, CORE_NOTABLE, FAR_NOTABLE,
            ABYSS_NOTABLE, CORE_SMALL);

    private static SkillNode node(String id, SkillType type, float treeX, float treeY, String region) {
        return new SkillNode(id, type, List.of(), treeX, treeY, SkillNodeDecoration.NONE, List.of(region));
    }

    private static ExoshipView view() {
        return new ExoshipView(EXOSHIP_NODE, NODES, new Random(11));
    }

    @Test
    void theExoshipOrbitsOnlyNotablesOutsideItsOwnBranchesAndTheAbyss() {
        List<ExoshipVoyage.Anchor> anchors = ExoshipView.orbitAnchors(NODES);

        assertEquals(List.of(new ExoshipVoyage.Anchor(0f, 0f), new ExoshipVoyage.Anchor(9000f, 0f)), anchors);
    }

    @Test
    void beforeBoardingTheCarriedBranchesAreHiddenAndUntouchable() {
        ExoshipView view = view();
        TreeViewport viewport = new TreeViewport(800f, 450f, 1f);

        TreeLayers layers = view.layers(viewport, 0.8f);

        assertEquals(0.8f, layers.treeAlpha());
        assertEquals(0f, layers.carriedAlpha());
        assertFalse(layers.isInteractive(CARRIED_NODE));
        assertTrue(layers.isInteractive(CORE_SMALL));
    }

    @Test
    void onBoardTheCarriedBranchesSitAroundTheShipAndTheRestOfTheTreeFadesOut() {
        ExoshipView view = view();
        view.enter();
        view.advance(ExoshipView.VIEW_FADE_SECONDS + 0.1f, true);
        TreeViewport viewport = new TreeViewport(800f, 450f, 0.5f);

        TreeLayers layers = view.layers(viewport, 1f);
        TreeViewport carried = layers.viewportFor(CARRIED_NODE);

        assertEquals(0f, layers.treeAlpha(), 1e-6f);
        assertEquals(1f, layers.carriedAlpha(), 1e-6f);
        assertTrue(layers.isInteractive(CARRIED_NODE));
        assertFalse(layers.isInteractive(CORE_SMALL));
        assertSame(viewport, layers.viewportFor(CORE_SMALL));
        assertEquals(viewport.screenX(view.focusX()), carried.screenX(EXOSHIP_NODE.getX()), 1e-2f);
        assertEquals(viewport.screenY(view.focusY()), carried.screenY(EXOSHIP_NODE.getY()), 1e-2f);
        assertEquals(viewport.screenX(view.focusX() + 1000f), carried.screenX(CARRIED_NODE.getX()), 1e-2f);
        assertEquals(ExoshipView.VIEWED_SHIP_LENGTH / 2f, layers.exoshipRadius(), 1e-3f);
    }

    @Test
    void leavingFadesTheBranchesBackOntoTheShip() {
        ExoshipView view = view();
        view.enter();
        view.advance(ExoshipView.VIEW_FADE_SECONDS, false);
        view.leave();
        view.advance(ExoshipView.VIEW_FADE_SECONDS, false);

        TreeLayers layers = view.layers(new TreeViewport(0f, 0f, 1f), 1f);

        assertFalse(view.isActive());
        assertEquals(0f, layers.carriedAlpha());
        assertEquals(ExoshipView.ROAMING_SHIP_LENGTH, view.shipLength(), 1e-3f);
    }

    @Test
    void boardingZoomsSoEveryCarriedBranchFitsOnScreen() {
        PositionAPI canvas = Mockito.mock(PositionAPI.class);
        Mockito.when(canvas.getWidth()).thenReturn(1600f);
        Mockito.when(canvas.getHeight()).thenReturn(900f);

        float zoom = view().fitZoom(canvas);

        assertEquals(450f * ExoshipView.FIT_SHARE / (1000f + ExoshipView.LAYOUT_MARGIN), zoom, 1e-4f);
    }

    @Test
    void theRoamingShipCanBeClickedOnItsHullButNotBesideIt() {
        ExoshipView view = view();
        TreeViewport viewport = new TreeViewport(800f, 450f, 1f);
        view.render(viewport, 0f);
        assertFalse(view.shipContains(viewport, viewport.screenX(view.focusX()), viewport.screenY(view.focusY())));

        view.render(viewport, 1f);
        float shipScreenX = viewport.screenX(view.focusX());
        float shipScreenY = viewport.screenY(view.focusY());

        assertTrue(view.shipContains(viewport, shipScreenX, shipScreenY));
        assertFalse(view.shipContains(viewport, shipScreenX + ExoshipView.ROAMING_SHIP_LENGTH, shipScreenY + ExoshipView.ROAMING_SHIP_LENGTH));
    }

    @Test
    void aBoardedShipIsNotAClickTarget() {
        ExoshipView view = view();
        TreeViewport viewport = new TreeViewport(800f, 450f, 1f);
        view.render(viewport, 1f);
        view.enter();

        assertFalse(view.shipContains(viewport, viewport.screenX(view.focusX()), viewport.screenY(view.focusY())));
    }

    @Test
    void untilTheBeaconIsUnlockedTheExoshipRoamsButCannotBeClickedOrBoarded() {
        ExoshipView view = view();
        TreeViewport viewport = new TreeViewport(800f, 450f, 1f);
        view.render(viewport, 1f);
        view.enter();
        view.advance(ExoshipView.VIEW_FADE_SECONDS, false);

        view.setBoardable(false);
        view.enter();

        assertTrue(view.isAvailable());
        assertFalse(view.isBoardable());
        assertFalse(view.isActive());
        assertEquals(0f, view.viewLevel());
        assertFalse(view.shipContains(viewport, viewport.screenX(view.focusX()), viewport.screenY(view.focusY())));
        assertEquals(0f, view.layers(viewport, 1f).carriedAlpha());
    }

    @Test
    void aTreeWithoutTheExoshipCannotBeBoarded() {
        ExoshipView view = new ExoshipView(null, List.of(CORE_NOTABLE, FAR_NOTABLE), new Random(3));

        view.enter();
        view.advance(1f, false);

        assertFalse(view.isAvailable());
        assertFalse(view.isActive());
    }
}
