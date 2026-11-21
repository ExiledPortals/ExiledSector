package exiledsector.compat;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FirstFailureLog {

    private static final String ONLY_KEY = "";

    private final Logger log;
    private final Level firstLevel;
    private final Set<String> reportedKeys = ConcurrentHashMap.newKeySet();

    private FirstFailureLog(Logger log, Level firstLevel) {
        this.log = log;
        this.firstLevel = firstLevel;
    }

    public static FirstFailureLog errors(Logger log) {
        return new FirstFailureLog(log, Level.ERROR);
    }

    public static FirstFailureLog warnings(Logger log) {
        return new FirstFailureLog(log, Level.WARN);
    }

    public boolean report(String key, String message, Throwable cause) {
        if (!reportedKeys.add(key)) {
            return false;
        }
        log.log(firstLevel, message, cause);
        return true;
    }

    public boolean report(String message, Throwable cause) {
        return report(ONLY_KEY, message, cause);
    }

    public void reportThenDebug(String key, String message, String repeatMessage, Throwable cause) {
        if (!report(key, message, cause)) {
            log.debug(repeatMessage, cause);
        }
    }

    public void reportThenDebug(String message, String repeatMessage, Throwable cause) {
        reportThenDebug(ONLY_KEY, message, repeatMessage, cause);
    }

    public void reset() {
        reportedKeys.clear();
    }
}
