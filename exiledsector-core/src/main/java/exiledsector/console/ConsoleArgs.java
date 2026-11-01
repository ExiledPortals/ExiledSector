package exiledsector.console;

import org.lazywizard.console.Console;

import java.util.List;

final class ConsoleArgs {

    private ConsoleArgs() {
    }

    static List<String> tokens(String args) {
        return args == null || args.isBlank() ? List.of() : List.of(args.trim().split("\\s+"));
    }

    static Integer copiesOrComplain(String args, int defaultCopies, int maxCopies) {
        Integer copies = count(args, defaultCopies, 1, maxCopies);
        if (copies == null) {
            Console.showMessage("Give a number of copies from 1 to " + maxCopies + ".");
        }
        return copies;
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
