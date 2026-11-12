package exiledsector.console;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OfficerRewardPickerCommandTest {

    @Test
    void theTierDefaultsToTheLowestAndMustBeAChartTier() {
        assertEquals(1, OfficerRewardPickerCommand.tier(""));
        assertEquals(1, OfficerRewardPickerCommand.tier(null));
        assertEquals(12, OfficerRewardPickerCommand.tier(" 12 "));
        assertEquals(15, OfficerRewardPickerCommand.tier("15"));
        assertNull(OfficerRewardPickerCommand.tier("0"));
        assertNull(OfficerRewardPickerCommand.tier("16"));
        assertNull(OfficerRewardPickerCommand.tier("high"));
        assertNull(OfficerRewardPickerCommand.tier("3 4"));
    }
}
