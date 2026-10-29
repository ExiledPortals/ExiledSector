package exiledsector.ui;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SkillTreeStatPanelTest {

    @Test
    void theStatsStartClosedAndTheButtonTogglesThem() {
        SkillTreeStatPanel panel = panel();

        assertFalse(panel.isOpen());
        panel.toggle();
        assertTrue(panel.isOpen());
        panel.toggle();
        assertFalse(panel.isOpen());
    }

    @Test
    void closingIsIdempotentSoHyperspaceCanCallItFreely() {
        SkillTreeStatPanel panel = panel();

        panel.toggle();
        panel.close();
        panel.close();
        assertFalse(panel.isOpen());
    }

    @Test
    void theFullSheetOnlyOpensFromAnOpenPanelAndEscapeCollapsesItFirst() {
        SkillTreeStatPanel panel = panel();

        panel.toggleExpanded();
        assertFalse(panel.isExpanded());

        panel.toggle();
        panel.toggleExpanded();
        assertTrue(panel.isExpanded());
        assertTrue(panel.collapse());
        assertFalse(panel.isExpanded());
        assertTrue(panel.isOpen());
        assertFalse(panel.collapse());
    }

    @Test
    void closingThePanelAlsoClosesTheFullSheet() {
        SkillTreeStatPanel panel = panel();

        panel.toggle();
        panel.toggleExpanded();
        panel.close();
        assertFalse(panel.isExpanded());
        panel.toggle();
        assertFalse(panel.isExpanded());
    }

    @Test
    void nothingIsHighlightedUntilAStatRowIsHovered() {
        assertTrue(panel().hoveredNodeIds().isEmpty());
    }

    private static SkillTreeStatPanel panel() {
        return new SkillTreeStatPanel(mock(FleetMemberAPI.class), null, null);
    }
}
