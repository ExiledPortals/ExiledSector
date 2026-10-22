package exiledsector.ui.socket;

public final class ItemCodexLink {

    public interface Codex {

        String existingEntryId(String definitionId);

        boolean open(String definitionId);
    }

    private static final Codex NO_CODEX = new Codex() {
        @Override
        public String existingEntryId(String definitionId) {
            return null;
        }

        @Override
        public boolean open(String definitionId) {
            return false;
        }
    };

    private static volatile Codex codex = NO_CODEX;

    private ItemCodexLink() {
    }

    public static void register(Codex itemCodex) {
        codex = itemCodex == null ? NO_CODEX : itemCodex;
    }

    public static String existingEntryId(String definitionId) {
        return codex.existingEntryId(definitionId);
    }

    public static boolean open(String definitionId) {
        return codex.open(definitionId);
    }
}
