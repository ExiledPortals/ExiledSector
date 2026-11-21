package exiledsector.harness;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.util.ArrayList;
import java.util.List;

final class HarnessLog extends AppenderSkeleton {

    static final int MAX_KEPT_LINES = 50_000;
    static final int MAX_KEPT_ERRORS = 5_000;

    private final List<String> keptLines = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private int droppedLineCount;

    @Override
    protected void append(LoggingEvent event) {
        String line = line(event);
        if (keptLines.size() >= MAX_KEPT_LINES) {
            int dropped = MAX_KEPT_LINES / 2;
            keptLines.subList(0, dropped).clear();
            droppedLineCount += dropped;
        }
        keptLines.add(line);
        if (event.getLevel().isGreaterOrEqual(Level.ERROR) && errors.size() < MAX_KEPT_ERRORS) {
            errors.add(line);
        }
    }

    static String line(LoggingEvent event) {
        String line = event.getLevel() + " " + event.getLoggerName() + " - " + event.getRenderedMessage();
        ThrowableInformation thrown = event.getThrowableInformation();
        return thrown == null || thrown.getThrowable() == null ? line : line + " | " + thrown.getThrowable();
    }

    synchronized int lineCount() {
        return droppedLineCount + keptLines.size();
    }

    synchronized List<String> errorsFrom(int firstError) {
        return new ArrayList<>(errors.subList(Math.min(firstError, errors.size()), errors.size()));
    }

    synchronized String firstLineContaining(String text, int firstLine) {
        for (int index = keptIndex(firstLine); index < keptLines.size(); index++) {
            if (keptLines.get(index).contains(text)) {
                return keptLines.get(index);
            }
        }
        return null;
    }

    synchronized String lastLineContaining(String text, int firstLine) {
        for (int index = keptLines.size() - 1; index >= keptIndex(firstLine); index--) {
            if (keptLines.get(index).contains(text)) {
                return keptLines.get(index);
            }
        }
        return null;
    }

    private int keptIndex(int line) {
        return Math.min(keptLines.size(), Math.max(0, line - droppedLineCount));
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
