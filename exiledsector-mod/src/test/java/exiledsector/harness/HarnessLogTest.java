package exiledsector.harness;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HarnessLogTest {

    private static final Logger SOURCE = Logger.getLogger("exiledsector.Source");

    static LoggingEvent event(Level level, String message) {
        return new LoggingEvent(Logger.class.getName(), SOURCE, level, message, null);
    }

    @Test
    void linesCarryTheirLevelLoggerAndThrowable() {
        LoggingEvent failed = new LoggingEvent(Logger.class.getName(), SOURCE, Level.ERROR, "broke", new IllegalStateException("bad"));

        assertEquals("ERROR exiledsector.Source - broke | java.lang.IllegalStateException: bad", HarnessLog.line(failed));
        assertEquals("INFO exiledsector.Source - fine", HarnessLog.line(event(Level.INFO, "fine")));
    }

    @Test
    void errorsAreKeptApartFromOtherLines() {
        HarnessLog log = new HarnessLog();
        log.doAppend(event(Level.INFO, "loaded"));
        log.doAppend(event(Level.ERROR, "first"));
        log.doAppend(event(Level.WARN, "careful"));
        log.doAppend(event(Level.FATAL, "second"));

        assertEquals(4, log.lineCount());
        assertEquals(List.of("ERROR exiledsector.Source - first", "FATAL exiledsector.Source - second"), log.errorsFrom(0));
        assertEquals(List.of("FATAL exiledsector.Source - second"), log.errorsFrom(1));
        assertEquals(List.of(), log.errorsFrom(9));
    }

    @Test
    void searchesStartAtTheGivenLine() {
        HarnessLog log = new HarnessLog();
        log.doAppend(event(Level.INFO, "[perf] Atlas opened 1"));
        log.doAppend(event(Level.INFO, "[perf] Atlas opened 2"));
        log.doAppend(event(Level.INFO, "[perf] Atlas opened 3"));

        assertEquals("INFO exiledsector.Source - [perf] Atlas opened 2", log.firstLineContaining("Atlas opened", 1));
        assertEquals("INFO exiledsector.Source - [perf] Atlas opened 3", log.lastLineContaining("Atlas opened", 1));
        assertNull(log.firstLineContaining("Atlas opened", 3));
        assertNull(log.lastLineContaining("Skill tree", 0));
    }

    @Test
    void aFullLogDropsItsOldestLinesAndKeepsLineNumbersSteady() {
        HarnessLog log = new HarnessLog();
        for (int line = 0; line < HarnessLog.MAX_KEPT_LINES; line++) {
            log.doAppend(event(Level.INFO, "line " + line));
        }
        log.doAppend(event(Level.INFO, "newest"));

        assertEquals(HarnessLog.MAX_KEPT_LINES + 1, log.lineCount());
        assertNull(log.firstLineContaining("line 0", 0));
        assertEquals("INFO exiledsector.Source - newest", log.firstLineContaining("newest", HarnessLog.MAX_KEPT_LINES));
        assertEquals("INFO exiledsector.Source - line " + (HarnessLog.MAX_KEPT_LINES - 1),
                log.lastLineContaining("line ", HarnessLog.MAX_KEPT_LINES - 1));
    }
}
