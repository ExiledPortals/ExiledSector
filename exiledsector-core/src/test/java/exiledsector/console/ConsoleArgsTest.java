package exiledsector.console;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConsoleArgsTest {

    @Test
    void aCountDefaultsWhenBlankAndMustBeAWholeNumberInRange() {
        assertEquals(5, ConsoleArgs.count("", 5, 1, 200));
        assertEquals(5, ConsoleArgs.count("  ", 5, 1, 200));
        assertEquals(12, ConsoleArgs.count(" 12 ", 5, 1, 200));
        assertEquals(200, ConsoleArgs.count("200", 5, 1, 200));
        assertNull(ConsoleArgs.count("0", 5, 1, 200));
        assertNull(ConsoleArgs.count("201", 5, 1, 200));
        assertNull(ConsoleArgs.count("1.5", 5, 1, 200));
        assertNull(ConsoleArgs.count("lots", 5, 1, 200));
    }

    @Test
    void aWholeNumberHasNoDefault() {
        assertEquals(1, ConsoleArgs.wholeNumber("1", 1, Integer.MAX_VALUE));
        assertNull(ConsoleArgs.wholeNumber("", 1, Integer.MAX_VALUE));
        assertNull(ConsoleArgs.wholeNumber(null, 1, Integer.MAX_VALUE));
        assertNull(ConsoleArgs.wholeNumber("first", 1, Integer.MAX_VALUE));
    }

    @Test
    void countedPicksTheSingularOnlyForOne() {
        assertEquals("1 copy", CampaignCommand.counted(1, "copy", "copies"));
        assertEquals("0 copies", CampaignCommand.counted(0, "copy", "copies"));
        assertEquals("3 copies", CampaignCommand.counted(3, "copy", "copies"));
    }
}
