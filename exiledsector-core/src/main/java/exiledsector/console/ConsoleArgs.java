package exiledsector.console;

final class ConsoleArgs {

    private ConsoleArgs() {
    }

    static Integer count(String text, int defaultCount, int min, int max) {
        return text == null || text.isBlank() ? Integer.valueOf(defaultCount) : wholeNumber(text, min, max);
    }

    static Integer wholeNumber(String text, int min, int max) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            int number = Integer.parseInt(text.trim());
            return number >= min && number <= max ? number : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
