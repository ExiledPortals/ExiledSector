package exiledsector.ui;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SkillTreeStatPanelTest {

    @Test
    void theStatsStartOpenAndTheButtonTogglesThem() {
        SkillTreeStatPanel panel = new SkillTreeStatPanel(mock(FleetMemberAPI.class));

        assertTrue(panel.isOpen());
        panel.toggle();
        assertFalse(panel.isOpen());
        panel.toggle();
        assertTrue(panel.isOpen());
    }

    @Test
    void openingAndClosingAreIdempotentSoHyperspaceCanCallThemFreely() {
        SkillTreeStatPanel panel = new SkillTreeStatPanel(mock(FleetMemberAPI.class));

        panel.close();
        panel.close();
        assertFalse(panel.isOpen());
        panel.open(false);
        panel.open(false);
        assertTrue(panel.isOpen());
    }
}
