package exiledsector.ui;

record TemplateBarState(boolean visible, boolean saveEnabled, boolean loadEnabled, boolean autoEnabled,
                        String saveHintKey, String autoHintKey) {

    static final TemplateBarState HIDDEN = new TemplateBarState(false, false, false, false,
            "ui.template.hint.save", "ui.template.hint.auto");

    private static final TemplateBarState[] VISIBLE_STATES = new TemplateBarState[32];

    static {
        for (int i = 0; i < VISIBLE_STATES.length; i++) {
            VISIBLE_STATES[i] = visible((i & 1) != 0, (i & 2) != 0, (i & 4) != 0, (i & 8) != 0, (i & 16) != 0);
        }
    }

    static TemplateBarState of(boolean rootChosen, int allocatedCount, boolean hasTemplate, boolean running, boolean pointsLeft) {
        return of(rootChosen, allocatedCount, hasTemplate, running, pointsLeft, false);
    }

    static TemplateBarState of(boolean rootChosen, int allocatedCount, boolean hasTemplate, boolean running, boolean pointsLeft,
                               boolean inert) {
        if (!rootChosen) {
            return HIDDEN;
        }
        return VISIBLE_STATES[(allocatedCount > 1 ? 1 : 0) | (hasTemplate ? 2 : 0) | (running ? 4 : 0) | (pointsLeft ? 8 : 0)
                | (inert ? 16 : 0)];
    }

    private static TemplateBarState visible(boolean hasNodes, boolean hasTemplate, boolean running, boolean pointsLeft, boolean inert) {
        String saveHint = hasNodes ? "ui.template.hint.save" : "ui.template.hint.saveEmpty";
        String autoHint;
        if (!hasTemplate) {
            autoHint = "ui.template.hint.autoNoTemplate";
        } else if (!pointsLeft) {
            autoHint = "ui.template.hint.autoNoPoints";
        } else {
            autoHint = "ui.template.hint.auto";
        }
        boolean live = !running && !inert;
        return new TemplateBarState(true, hasNodes && live, live, hasTemplate && pointsLeft && live,
                saveHint, autoHint);
    }
}
