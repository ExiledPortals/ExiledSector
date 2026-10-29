package exiledsector.i18n;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Template {

    private sealed interface Part permits Literal, Placeholder, Open, Close {
    }

    private record Literal(String text) implements Part {
    }

    private record Placeholder(String name) implements Part {
    }

    private record Open(Style style) implements Part {
    }

    private record Close(Style style) implements Part {
    }

    private final List<Part> parts;

    private Template(List<Part> parts) {
        this.parts = List.copyOf(parts);
    }

    public static Template parse(String raw) {
        List<Part> parts = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            int end = c == '{' || c == '<' ? raw.indexOf(c == '{' ? '}' : '>', i + 1) : -1;
            Part token = end < 0 ? null : token(c, raw.substring(i + 1, end));
            if (token == null) {
                literal.append(c);
                i++;
                continue;
            }
            if (!literal.isEmpty()) {
                parts.add(new Literal(literal.toString()));
                literal.setLength(0);
            }
            parts.add(token);
            i = end + 1;
        }
        if (!literal.isEmpty()) {
            parts.add(new Literal(literal.toString()));
        }
        return new Template(parts);
    }

    private static Part token(char opener, String inner) {
        if (opener == '{') {
            return isIdentifier(inner) ? new Placeholder(inner) : null;
        }
        boolean closing = inner.startsWith("/");
        Style style = Style.byTag(closing ? inner.substring(1) : inner);
        if (style == null) {
            return null;
        }
        return closing ? new Close(style) : new Open(style);
    }

    private static boolean isIdentifier(String text) {
        if (text.isEmpty() || !Character.isLetter(text.charAt(0))) {
            return false;
        }
        for (int i = 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        return true;
    }

    public Set<String> placeholders() {
        Set<String> names = new LinkedHashSet<>();
        for (Part part : parts) {
            if (part instanceof Placeholder placeholder) {
                names.add(placeholder.name());
            }
        }
        return names;
    }

    public List<String> tagSequence() {
        List<String> tags = new ArrayList<>();
        for (Part part : parts) {
            if (part instanceof Open open) {
                tags.add(open.style().tag());
            } else if (part instanceof Close close) {
                tags.add("/" + close.style().tag());
            }
        }
        return tags;
    }

    public boolean hasBalancedTags() {
        Style open = null;
        for (Part part : parts) {
            if (part instanceof Open tag) {
                if (open != null) {
                    return false;
                }
                open = tag.style();
            } else if (part instanceof Close tag) {
                if (open != tag.style()) {
                    return false;
                }
                open = null;
            }
        }
        return open == null;
    }

    public StyledText render(Map<String, StyledText> args) {
        StringBuilder plainText = new StringBuilder();
        List<StyledText.Span> spans = new ArrayList<>();
        Style openStyle = null;
        int openStart = 0;
        int depth = 0;
        for (Part part : parts) {
            if (part instanceof Literal literal) {
                plainText.append(literal.text());
            } else if (part instanceof Placeholder placeholder) {
                StyledText argValue = args.get(placeholder.name());
                if (argValue == null) {
                    plainText.append('{').append(placeholder.name()).append('}');
                    continue;
                }
                if (depth == 0) {
                    int offset = plainText.length();
                    for (StyledText.Span span : argValue.spans()) {
                        spans.add(new StyledText.Span(span.start() + offset, span.end() + offset, span.style()));
                    }
                }
                plainText.append(argValue.plain());
            } else if (part instanceof Open open) {
                if (depth == 0) {
                    openStyle = open.style();
                    openStart = plainText.length();
                }
                depth++;
            } else if (depth > 0) {
                depth--;
                if (depth == 0) {
                    addSpan(spans, openStart, plainText.length(), openStyle);
                }
            }
        }
        if (depth > 0) {
            addSpan(spans, openStart, plainText.length(), openStyle);
        }
        return new StyledText(plainText.toString(), spans);
    }

    private static void addSpan(List<StyledText.Span> spans, int start, int end, Style style) {
        if (end > start) {
            spans.add(new StyledText.Span(start, end, style));
        }
    }
}
