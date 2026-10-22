package exiledsector.ui;

record TemplateBarState(boolean visible, boolean templateModeEnabled, boolean planning, boolean saveEnabled, boolean loadEnabled,
                        boolean autoEnabled, boolean autoOn, String saveLabelKey, String saveHintKey, String autoHintKey) {

    static final TemplateBarState HIDDEN = new TemplateBarState(false, false, false, false, false, false, false,
            "ui.template.button.save", "ui.template.hint.save", "ui.template.hint.autoOff");

    private static final TemplateBarState[] VISIBLE_STATES = new TemplateBarState[64];

    static {
        for (int i = 0; i < VISIBLE_STATES.length; i++) {
            VISIBLE_STATES[i] = visible((i & 1) != 0, (i & 2) != 0, (i & 4) != 0, (i & 8) != 0, (i & 16) != 0, (i & 32) != 0);
        }
    }

    static TemplateBarState of(boolean rootChosen, int allocatedCount, boolean hasTemplate, boolean running, boolean inert,
                               boolean planning, boolean autoOn) {
        if (!rootChosen) {
            return HIDDEN;
        }
        return VISIBLE_STATES[(allocatedCount > 1 ? 1 : 0) | (hasTemplate ? 2 : 0) | (running ? 4 : 0) | (inert ? 8 : 0)
                | (planning ? 16 : 0) | (autoOn ? 32 : 0)];
    }

    private static TemplateBarState visible(boolean hasNodes, boolean hasTemplate, boolean running, boolean inert, boolean planning,
                                            boolean autoOn) {
        boolean live = !running && !inert;
        String saveLabel;
        String saveHint;
        boolean saveEnabled;
        if (planning) {
            saveLabel = "ui.template.button.rename";
            saveHint = hasTemplate ? "ui.template.hint.rename" : "ui.template.hint.renameEmpty";
            saveEnabled = hasTemplate && live;
        } else {
            saveLabel = "ui.template.button.save";
            saveHint = hasNodes ? "ui.template.hint.save" : "ui.template.hint.saveEmpty";
            saveEnabled = hasNodes && live;
        }
        String autoHint;
        if (!hasTemplate) {
            autoHint = "ui.template.hint.autoNoTemplate";
        } else if (planning) {
            autoHint = "ui.template.hint.autoPlanning";
        } else {
            autoHint = autoOn ? "ui.template.hint.autoOn" : "ui.template.hint.autoOff";
        }
        boolean autoEnabled = live && !planning && (hasTemplate || autoOn);
        return new TemplateBarState(true, live, planning, saveEnabled, live, autoEnabled, autoOn,
                saveLabel, saveHint, autoHint);
    }
}
