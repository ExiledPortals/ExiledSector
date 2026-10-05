package exiledsector.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record StyledText(String plain, List<Span> spans) {

    public record Span(int start, int end, Style style) {
    }

    public static final StyledText EMPTY = new StyledText("", List.of());

    public StyledText {
        spans = List.copyOf(spans);
        int previousEnd = 0;
        for (Span span : spans) {
            if (span.start() < previousEnd || span.end() <= span.start() || span.end() > plain.length()) {
                throw new IllegalArgumentException("Invalid span " + span + " in \"" + plain + "\"");
            }
            previousEnd = span.end();
        }
    }

    public static StyledText of(String plain) {
        return new StyledText(plain, List.of());
    }

    public static StyledText parse(String markup) {
        return Template.parse(markup).render(Map.of());
    }

    public static StyledText styled(String plain, Style style) {
        return plain.isEmpty() ? EMPTY : new StyledText(plain, List.of(new Span(0, plain.length(), style)));
    }

    public boolean isEmpty() {
        return plain.isEmpty();
    }

    public StyledText inverted() {
        List<Span> inverted = new ArrayList<>(spans.size());
        for (Span span : spans) {
            inverted.add(new Span(span.start(), span.end(), span.style().inverted()));
        }
        return new StyledText(plain, inverted);
    }

    public StyledText append(String literal) {
        return new StyledText(plain + literal, spans);
    }

    public StyledText append(StyledText other) {
        List<Span> combined = new ArrayList<>(spans);
        int offset = plain.length();
        for (Span span : other.spans) {
            combined.add(new Span(span.start() + offset, span.end() + offset, span.style()));
        }
        return new StyledText(plain + other.plain, combined);
    }

    public StyledText insert(int index, String literal) {
        List<Span> shifted = new ArrayList<>(spans.size());
        int length = literal.length();
        for (Span span : spans) {
            int start = span.start() >= index ? span.start() + length : span.start();
            int end = span.end() > index ? span.end() + length : span.end();
            shifted.add(new Span(start, end, span.style()));
        }
        return new StyledText(plain.substring(0, index) + literal + plain.substring(index), shifted);
    }

    public static StyledText join(StyledText separator, List<StyledText> parts) {
        StyledText joined = EMPTY;
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                joined = joined.append(separator);
            }
            joined = joined.append(parts.get(i));
        }
        return joined;
    }

    public String toMarkup() {
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        for (Span span : spans) {
            out.append(plain, cursor, span.start())
                    .append('<').append(span.style().tag()).append('>')
                    .append(plain, span.start(), span.end())
                    .append("</").append(span.style().tag()).append('>');
            cursor = span.end();
        }
        return out.append(plain.substring(cursor)).toString();
    }

    @Override
    public String toString() {
        return toMarkup();
    }
}
