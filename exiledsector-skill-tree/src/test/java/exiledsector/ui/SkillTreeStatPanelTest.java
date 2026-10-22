package exiledsector.ui;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SkillTreeStatPanelTest {

    @Test
    void theStatsStartClosedAndTheButtonTogglesThem() {
        SkillTreeStatPanel panel = new SkillTreeStatPanel(mock(FleetMemberAPI.class));

        assertFalse(panel.isOpen());
        panel.toggle();
        assertTrue(panel.isOpen());
        panel.toggle();
        assertFalse(panel.isOpen());
    }

    @Test
    void closingIsIdempotentSoHyperspaceCanCallItFreely() {
        SkillTreeStatPanel panel = new SkillTreeStatPanel(mock(FleetMemberAPI.class));

        panel.toggle();
        panel.close();
        panel.close();
        assertFalse(panel.isOpen());
    }
}
