package exiledsector.ui.socket;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ItemStorageSearch {

    private static final Pattern TERM_SEPARATOR = Pattern.compile("(?<!\\d),|,(?!\\d)|[，；;]");
    private static final Pattern TOKEN = Pattern.compile("[\"“”]([^\"“”]*)[\"“”]?|(\\S+)");
    private static final Pattern MINIMUM_TOKEN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\+");
    private static final Pattern LINE_NUMBER = Pattern.compile("(?<![\\p{L}\\d.\\-])-?\\d[\\d,]*(?:\\.\\d+)?");

    private record Term(List<String> words, Double minimumValue) {

        boolean matchesLine(String searchLine) {
            if (!words.stream().allMatch(searchLine::contains)) {
                return false;
            }
            if (minimumValue == null) {
                return true;
            }
            OptionalDouble lineValue = firstNumber(searchLine);
            return lineValue.isPresent() && lineValue.getAsDouble() >= minimumValue;
        }

        boolean matches(List<String> searchLines) {
            return searchLines.stream().anyMatch(this::matchesLine);
        }
    }

    private final List<Term> terms;

    private ItemStorageSearch(List<Term> terms) {
        this.terms = terms;
    }

    static ItemStorageSearch parse(String query) {
        List<Term> terms = new ArrayList<>();
        for (String termText : TERM_SEPARATOR.split(query.toLowerCase(Locale.ROOT))) {
            Term term = parseTerm(termText);
            if (term != null) {
                terms.add(term);
            }
        }
        return new ItemStorageSearch(terms);
    }

    private static Term parseTerm(String termText) {
        List<String> words = new ArrayList<>();
        Double minimumValue = null;
        Matcher tokenMatcher = TOKEN.matcher(termText);
        while (tokenMatcher.find()) {
            String phrase = tokenMatcher.group(1);
            if (phrase != null) {
                String trimmedPhrase = phrase.trim().replaceAll("\\s+", " ");
                if (!trimmedPhrase.isEmpty()) {
                    words.add(trimmedPhrase);
                }
                continue;
            }
            String token = tokenMatcher.group(2);
            Matcher minimumMatcher = MINIMUM_TOKEN.matcher(token);
            if (minimumMatcher.matches()) {
                minimumValue = Double.parseDouble(minimumMatcher.group(1));
            } else {
                words.add(token);
            }
        }
        return words.isEmpty() && minimumValue == null ? null : new Term(words, minimumValue);
    }

    boolean matches(List<String> searchLines) {
        return terms.stream().allMatch(term -> term.matches(searchLines));
    }

    static OptionalDouble firstNumber(String searchLine) {
        Matcher numberMatcher = LINE_NUMBER.matcher(searchLine);
        if (!numberMatcher.find()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Double.parseDouble(numberMatcher.group().replace(",", "")));
    }
}
