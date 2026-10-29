package exiledsector.i18n;

final class LazyFontWrapOracle {

    private LazyFontWrapOracle() {
    }

    static String wrapString(String toWrap, TextWrapper.Metrics metrics, float fontSize, float maxWidth, float maxHeight) {
        int maxLines = (int) (maxHeight / fontSize);
        StringBuilder wrapped = new StringBuilder();
        int numLines = 0;
        if (maxWidth <= 0f) {
            return "";
        }
        for (String rawLine : toWrap.split("\n", -1)) {
            String line = rawLine;
            if (line.isBlank()) {
                wrapped.append('\n');
                numLines++;
                continue;
            }
            inner:
            while (!line.isBlank()) {
                if (numLines >= maxLines) {
                    break;
                }
                String tmp = buildUntilLimit(line, metrics, maxWidth);
                if (tmp.length() == line.length()) {
                    wrapped.append(tmp).append('\n');
                    numLines++;
                    break;
                }
                int lastSpace = tmp.lastIndexOf(' ');
                if (lastSpace != -1) {
                    wrapped.append(line, 0, lastSpace).append('\n');
                    line = line.length() > lastSpace ? line.substring(lastSpace + 1) : "";
                    numLines++;
                } else {
                    while (true) {
                        int splitIndex = Math.max(0, buildUntilLimit("-" + line, metrics, maxWidth).length() - 1);
                        if (splitIndex < line.length()) {
                            wrapped.append(line, 0, splitIndex).append("-\n");
                            line = line.substring(splitIndex);
                            numLines++;
                            continue inner;
                        }
                        wrapped.append(line).append('\n');
                        numLines++;
                        break inner;
                    }
                }
            }
        }
        return wrapped.substring(0, wrapped.length() - 1);
    }

    private static String buildUntilLimit(String rawLine, TextWrapper.Metrics metrics, float maxWidth) {
        if (rawLine.isBlank() || maxWidth <= 0f) {
            return "";
        }
        for (int length = 1; length <= rawLine.length(); length++) {
            if (metrics.width(rawLine.substring(0, length)) > maxWidth) {
                return rawLine.substring(0, length - 1);
            }
        }
        return rawLine;
    }
}
