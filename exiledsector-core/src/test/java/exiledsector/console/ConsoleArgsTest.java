package exiledsector.console;

import org.junit.jupiter.api.Test;
import org.lazywizard.console.Console;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

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
    void tokensSplitOnAnyWhitespaceAndAreEmptyForBlankArguments() {
        assertEquals(List.of("3", "12"), ConsoleArgs.tokens(" 3 \t 12 "));
        assertEquals(List.of(), ConsoleArgs.tokens("  "));
        assertEquals(List.of(), ConsoleArgs.tokens(null));
    }

    @Test
    void copiesOutOfRangeAreReported() {
        try (MockedStatic<Console> console = Mockito.mockStatic(Console.class)) {
            assertEquals(3, ConsoleArgs.copiesOrComplain("", 3, 50));
            assertNull(ConsoleArgs.copiesOrComplain("51", 3, 50));
            console.verify(() -> Console.showMessage("Give a number of copies from 1 to 50."));
        }
    }

    @Test
    void countedPicksTheSingularOnlyForOne() {
        assertEquals("1 copy", CampaignCommand.counted(1, "copy", "copies"));
        assertEquals("0 copies", CampaignCommand.counted(0, "copy", "copies"));
        assertEquals("3 copies", CampaignCommand.counted(3, "copy", "copies"));
    }
}
