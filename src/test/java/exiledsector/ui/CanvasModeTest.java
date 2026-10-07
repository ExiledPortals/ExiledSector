package exiledsector.ui;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanvasModeTest {

    @Test
    void rootChoiceOutranksEveryOtherMode() {
        assertEquals(CanvasMode.ROOT_CHOICE, CanvasMode.resolve(true, true, true, true, true, true));
    }

    @Test
    void modesResolveInPriorityOrder() {
        assertEquals(CanvasMode.MODAL, CanvasMode.resolve(false, true, true, true, true, true));
        assertEquals(CanvasMode.WORKBENCH, CanvasMode.resolve(false, false, true, true, true, true));
        assertEquals(CanvasMode.HYPERSPACE, CanvasMode.resolve(false, false, false, true, true, true));
        assertEquals(CanvasMode.FLEET_FOLLOW, CanvasMode.resolve(false, false, false, false, true, true));
        assertEquals(CanvasMode.ALLOCATION_RUN, CanvasMode.resolve(false, false, false, false, false, true));
        assertEquals(CanvasMode.TREE, CanvasMode.resolve(false, false, false, false, false, false));
    }

    @Test
    void theStatsToggleWorksWhileChoosingARoot() {
        assertEquals(EnumSet.of(CanvasMode.Chrome.STATS_TOGGLE, CanvasMode.Chrome.READOUTS), enabledIn(CanvasMode.ROOT_CHOICE));
    }

    @Test
    void theTreeAndARunningAllocationEnableAllChrome() {
        assertEquals(EnumSet.allOf(CanvasMode.Chrome.class), enabledIn(CanvasMode.TREE));
        assertEquals(EnumSet.allOf(CanvasMode.Chrome.class), enabledIn(CanvasMode.ALLOCATION_RUN));
    }

    @Test
    void blockingModesLeaveOnlyTheirOwnChromeLive() {
        assertEquals(EnumSet.noneOf(CanvasMode.Chrome.class), enabledIn(CanvasMode.MODAL));
        assertEquals(EnumSet.of(CanvasMode.Chrome.STORAGE_BUTTON), enabledIn(CanvasMode.WORKBENCH));
        assertEquals(EnumSet.of(CanvasMode.Chrome.READOUTS), enabledIn(CanvasMode.HYPERSPACE));
    }

    @Test
    void everyEnabledChromeElementIsAlsoShown() {
        for (CanvasMode mode : CanvasMode.values()) {
            for (CanvasMode.Chrome chrome : enabledIn(mode)) {
                assertTrue(mode.shows(chrome), mode + " enables hidden " + chrome);
            }
        }
    }

    @Test
    void hyperspaceAndRootChoiceHideTheSearchBar() {
        assertFalse(CanvasMode.HYPERSPACE.shows(CanvasMode.Chrome.SEARCH));
        assertFalse(CanvasMode.ROOT_CHOICE.shows(CanvasMode.Chrome.SEARCH));
        assertTrue(CanvasMode.WORKBENCH.shows(CanvasMode.Chrome.SEARCH));
        assertTrue(CanvasMode.MODAL.shows(CanvasMode.Chrome.READOUTS));
    }

    @Test
    void onlyTreeLikeModesHoverNodes() {
        assertTrue(CanvasMode.TREE.hoversTree());
        assertTrue(CanvasMode.ALLOCATION_RUN.hoversTree());
        assertTrue(CanvasMode.ROOT_CHOICE.hoversTree());
        assertFalse(CanvasMode.MODAL.hoversTree());
        assertFalse(CanvasMode.WORKBENCH.hoversTree());
        assertFalse(CanvasMode.HYPERSPACE.hoversTree());
    }

    @Test
    void chromeTooltipsAreHiddenUnderModalsAndTheWorkbench() {
        assertFalse(CanvasMode.MODAL.showsChromeTooltips());
        assertFalse(CanvasMode.WORKBENCH.showsChromeTooltips());
        assertTrue(CanvasMode.HYPERSPACE.showsChromeTooltips());
        assertTrue(CanvasMode.ROOT_CHOICE.showsChromeTooltips());
    }

    @Test
    void storageClosesWhenTheTreeIsUnreachable() {
        assertEquals(EnumSet.of(CanvasMode.ROOT_CHOICE, CanvasMode.MODAL, CanvasMode.HYPERSPACE, CanvasMode.FLEET_FOLLOW),
                matching(CanvasMode::closesStorage));
    }

    @Test
    void onlyTheIdleOrFollowingTreeScrollsOutIntoHyperspace() {
        assertEquals(EnumSet.of(CanvasMode.TREE, CanvasMode.FLEET_FOLLOW), matching(CanvasMode::entersHyperspaceOnScrollOut));
    }

    @Test
    void followingTheFleetHidesTheTemplateBarAndStorageAndStopsDragPanning() {
        assertEquals(EnumSet.of(CanvasMode.Chrome.STATS_TOGGLE, CanvasMode.Chrome.READOUTS, CanvasMode.Chrome.SEARCH),
                enabledIn(CanvasMode.FLEET_FOLLOW));
        assertFalse(CanvasMode.FLEET_FOLLOW.shows(CanvasMode.Chrome.TEMPLATE_BAR));
        assertFalse(CanvasMode.FLEET_FOLLOW.shows(CanvasMode.Chrome.STORAGE_BUTTON));
        assertTrue(CanvasMode.FLEET_FOLLOW.hoversTree());
        assertEquals(EnumSet.of(CanvasMode.FLEET_FOLLOW), matching(mode -> !mode.pansOnDrag()));
    }

    @Test
    void theShipCardTogglesFollowingOnlyFromTheTree() {
        assertEquals(EnumSet.of(CanvasMode.TREE, CanvasMode.ALLOCATION_RUN, CanvasMode.FLEET_FOLLOW),
                matching(CanvasMode::letsShipCardFollowFleet));
    }

    private static Set<CanvasMode.Chrome> enabledIn(CanvasMode mode) {
        Set<CanvasMode.Chrome> enabled = EnumSet.noneOf(CanvasMode.Chrome.class);
        for (CanvasMode.Chrome chrome : CanvasMode.Chrome.values()) {
            if (mode.enables(chrome)) {
                enabled.add(chrome);
            }
        }
        return enabled;
    }

    private static Set<CanvasMode> matching(Predicate<CanvasMode> predicate) {
        Set<CanvasMode> modes = EnumSet.noneOf(CanvasMode.class);
        for (CanvasMode mode : CanvasMode.values()) {
            if (predicate.test(mode)) {
                modes.add(mode);
            }
        }
        return modes;
    }
}
