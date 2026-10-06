package exiledsector.i18n;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TextWrapper {

    @FunctionalInterface
    public interface Metrics {
        float width(String text);
    }

    private static final String NO_BREAK_BEFORE = "，。、；：？！）》」』】〉〕］｝…‥·・～—％%,.;:?!)]}'\"”’";
    private static final String NO_BREAK_AFTER = "（《「『【〈〔［｛“‘(";

    private TextWrapper() {
    }

    public static StyledText wrap(StyledText text, Metrics metrics, float fontSize, float maxWidth, float maxHeight) {
        return wrap(text, metrics, maxWidth, (int) (maxHeight / fontSize));
    }

    public static StyledText wrap(StyledText text, Metrics metrics, float maxWidth, int maxLines) {
        if (maxWidth <= 0f) {
            return StyledText.EMPTY;
        }
        return new Wrapping(text, metrics, maxWidth, maxLines).run();
    }

    private static final class Wrapping {
        private final String source;
        private final List<StyledText.Span> spans;
        private final Metrics metrics;
        private final float maxWidth;
        private final int maxLines;
        private final Output out;
        private int numLines;

        Wrapping(StyledText text, Metrics metrics, float maxWidth, int maxLines) {
            this.source = text.plain();
            this.spans = text.spans();
            this.metrics = metrics;
            this.maxWidth = maxWidth;
            this.maxLines = maxLines;
            this.out = new Output(source.length());
        }

        StyledText run() {
            int lineStart = 0;
            while (lineStart <= source.length()) {
                int lineEnd = source.indexOf('\n', lineStart);
                if (lineEnd < 0) {
                    lineEnd = source.length();
                }
                if (isBlank(source, lineStart, lineEnd)) {
                    out.append('\n', -1);
                    numLines++;
                } else {
                    wrapParagraph(lineStart, lineEnd);
                }
                lineStart = lineEnd + 1;
            }
            return out.result(spans);
        }

        private void wrapParagraph(int start, int end) {
            int cursor = start;
            while (numLines < maxLines && !isBlank(source, cursor, end)) {
                cursor = writeLine(cursor, end);
                numLines++;
            }
        }

        private int writeLine(int cursor, int end) {
            int fits = fittingLength(source, cursor, end, metrics, maxWidth);
            if (cursor + fits == end) {
                out.copy(source, cursor, end);
                out.append('\n', -1);
                return end;
            }
            int lastSpace = source.lastIndexOf(' ', cursor + fits - 1);
            lastSpace = lastSpace >= cursor ? lastSpace : -1;
            int cjkBreak = cjkBreak(source, cursor, cursor + fits, spans);
            if (cjkBreak > lastSpace && cjkBreak > cursor) {
                out.copy(source, cursor, cjkBreak);
                out.append('\n', -1);
                return cjkBreak;
            }
            if (lastSpace >= 0) {
                out.copy(source, cursor, lastSpace);
                out.append('\n', lastSpace);
                return lastSpace + 1;
            }
            return hyphenate(cursor, end);
        }

        private int hyphenate(int cursor, int end) {
            int split = Math.max(1, fittingLength("-" + source.substring(cursor, end), 0, end - cursor + 1, metrics, maxWidth) - 1);
            if (split < end - cursor) {
                out.copy(source, cursor, cursor + split);
                out.append('-', -1);
                out.append('\n', -1);
                return cursor + split;
            }
            out.copy(source, cursor, end);
            out.append('\n', -1);
            return end;
        }
    }

    private static boolean isBlank(String text, int start, int end) {
        for (int i = start; i < end; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static int fittingLength(String text, int start, int end, Metrics metrics, float maxWidth) {
        for (int length = 1; start + length <= end; length++) {
            if (metrics.width(text.substring(start, start + length)) > maxWidth) {
                return length - 1;
            }
        }
        return end - start;
    }

    private static int cjkBreak(String text, int start, int end, List<StyledText.Span> spans) {
        int insideSpan = -1;
        for (int position = end; position > start; position--) {
            if (canBreakBefore(text, position)) {
                if (!insideSpan(spans, position)) {
                    return position;
                }
                if (insideSpan < 0) {
                    insideSpan = position;
                }
            }
        }
        return insideSpan;
    }

    private static boolean insideSpan(List<StyledText.Span> spans, int position) {
        for (StyledText.Span span : spans) {
            if (span.start() < position && position < span.end()) {
                return true;
            }
        }
        return false;
    }

    static boolean canBreakBefore(String text, int position) {
        if (position <= 0 || position >= text.length()) {
            return false;
        }
        char before = text.charAt(position - 1);
        char after = text.charAt(position);
        if (Character.isHighSurrogate(before) || Character.isLowSurrogate(after)
                || Character.isWhitespace(before) || Character.isWhitespace(after)) {
            return false;
        }
        if (!isCjk(text.codePointBefore(position)) && !isCjk(text.codePointAt(position))) {
            return false;
        }
        return NO_BREAK_BEFORE.indexOf(after) < 0 && NO_BREAK_AFTER.indexOf(before) < 0;
    }

    static boolean isCjk(int codePoint) {
        if (Character.isIdeographic(codePoint)) {
            return true;
        }
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.GENERAL_PUNCTUATION && codePoint >= 0x2018 && codePoint <= 0x2026;
    }

    private static final class Output {
        private final StringBuilder text = new StringBuilder();
        private final int[] positions;

        Output(int sourceLength) {
            positions = new int[sourceLength];
            Arrays.fill(positions, -1);
        }

        void copy(String source, int start, int end) {
            for (int i = start; i < end; i++) {
                positions[i] = text.length();
                text.append(source.charAt(i));
            }
        }

        void append(char c, int sourceIndex) {
            if (sourceIndex >= 0) {
                positions[sourceIndex] = text.length();
            }
            text.append(c);
        }

        StyledText result(List<StyledText.Span> sourceSpans) {
            if (!text.isEmpty()) {
                text.setLength(text.length() - 1);
            }
            List<StyledText.Span> spans = new ArrayList<>();
            for (StyledText.Span span : sourceSpans) {
                int start = firstKept(span.start(), span.end());
                int end = Math.min(lastKept(span.start(), span.end()) + 1, text.length());
                if (start >= 0 && end > start) {
                    spans.add(new StyledText.Span(start, end, span.style()));
                }
            }
            return new StyledText(text.toString(), spans);
        }

        private int firstKept(int start, int end) {
            for (int i = start; i < end; i++) {
                if (positions[i] >= 0) {
                    return positions[i];
                }
            }
            return -1;
        }

        private int lastKept(int start, int end) {
            for (int i = end - 1; i >= start; i--) {
                if (positions[i] >= 0) {
                    return positions[i];
                }
            }
            return -1;
        }
    }
}
