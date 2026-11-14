package exiledsector.compat;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class FirstFailureLogTest {

    @Test
    void eachKeyIsLoggedAtItsLevelOnceUntilReset() {
        Logger log = mock(Logger.class);
        FirstFailureLog failures = FirstFailureLog.warnings(log);
        RuntimeException cause = new RuntimeException("boom");

        assertTrue(failures.report("a", "first a", cause));
        assertFalse(failures.report("a", "second a", cause));
        assertTrue(failures.report("b", "first b", cause));
        failures.reset();
        assertTrue(failures.report("a", "first a", cause));

        verify(log, times(2)).log(Level.WARN, "first a", cause);
        verify(log, never()).log(Level.WARN, "second a", cause);
        verify(log).log(Level.WARN, "first b", cause);
    }

    @Test
    void repeatsDropToDebug() {
        Logger log = mock(Logger.class);
        FirstFailureLog failures = FirstFailureLog.errors(log);
        RuntimeException cause = new RuntimeException("boom");

        failures.reportThenDebug("failed", "failed again", cause);
        failures.reportThenDebug("failed", "failed again", cause);

        verify(log).log(Level.ERROR, "failed", cause);
        verify(log).debug("failed again", cause);
    }
}
