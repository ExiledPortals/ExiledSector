package exiledsector.skills.unlock;

import exiledsector.ModSettings;

public final class HiddenNodeDisplayConfig {

    public static final String SHOW_HIDDEN_NODES_FIELD_ID = "exiledSector_showHiddenNodesByDefault";

    public static final boolean DEFAULT_SHOW_HIDDEN_NODES = false;

    private HiddenNodeDisplayConfig() {
    }

    public static boolean showHiddenNodesByDefault() {
        return ModSettings.booleanOr(SHOW_HIDDEN_NODES_FIELD_ID, DEFAULT_SHOW_HIDDEN_NODES);
    }
}
