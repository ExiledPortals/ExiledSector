package exiledsector.skills;

public record NodeEntry(String nodeId, String optionId) {

    private static final String OPTION_SEPARATOR = "=";

    public static NodeEntry parse(String entry) {
        int separator = entry.indexOf(OPTION_SEPARATOR);
        if (separator < 0) {
            return new NodeEntry(NodeReplacements.current(entry), null);
        }
        if (separator == 0) {
            return null;
        }
        String optionId = entry.substring(separator + 1);
        return new NodeEntry(NodeReplacements.current(entry.substring(0, separator)),
                optionId.isEmpty() ? null : AffinityOptions.currentOptionId(optionId));
    }

    public String format() {
        return optionId == null ? nodeId : nodeId + OPTION_SEPARATOR + optionId;
    }
}
