package exiledsector.ui.socket;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ItemStorageSearch {

    private static final Pattern MINIMUM_TOKEN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\+");
    private static final Pattern LINE_NUMBER = Pattern.compile("(?<![\\p{L}\\d.\\-])-?\\d[\\d,]*(?:\\.\\d+)?");

    private final List<String> words;
    private final Double minimumValue;

    private ItemStorageSearch(List<String> words, Double minimumValue) {
        this.words = words;
        this.minimumValue = minimumValue;
    }

    static ItemStorageSearch parse(String query) {
        List<String> words = new ArrayList<>();
        Double minimumValue = null;
        for (String token : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            Matcher minimumMatcher = MINIMUM_TOKEN.matcher(token);
            if (minimumMatcher.matches()) {
                minimumValue = Double.parseDouble(minimumMatcher.group(1));
            } else if (!token.isEmpty()) {
                words.add(token);
            }
        }
        return new ItemStorageSearch(words, minimumValue);
    }

    boolean isEmpty() {
        return words.isEmpty() && minimumValue == null;
    }

    boolean matches(List<String> searchLines) {
        return isEmpty() || searchLines.stream().anyMatch(this::matchesLine);
    }

    OptionalDouble matchedValue(List<String> searchLines) {
        if (isEmpty()) {
            return OptionalDouble.empty();
        }
        return searchLines.stream().filter(this::matchesLine).map(ItemStorageSearch::firstNumber)
                .filter(OptionalDouble::isPresent).mapToDouble(OptionalDouble::getAsDouble).max();
    }

    private boolean matchesLine(String searchLine) {
        if (!words.stream().allMatch(searchLine::contains)) {
            return false;
        }
        if (minimumValue == null) {
            return true;
        }
        OptionalDouble lineValue = firstNumber(searchLine);
        return lineValue.isPresent() && lineValue.getAsDouble() >= minimumValue;
    }

    static OptionalDouble firstNumber(String searchLine) {
        Matcher numberMatcher = LINE_NUMBER.matcher(searchLine);
        if (!numberMatcher.find()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Double.parseDouble(numberMatcher.group().replace(",", "")));
    }
}
