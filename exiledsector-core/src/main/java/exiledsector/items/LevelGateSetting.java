package exiledsector.items;

import exiledsector.ModSettings;
import exiledsector.ui.SettingsPage;

public final class LevelGateSetting {

    public static final int DEFAULT_LEVEL = 15;
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 15;

    private final String fieldId;
    private volatile int level = DEFAULT_LEVEL;

    public LevelGateSetting(String fieldId) {
        this.fieldId = fieldId;
    }

    public void addSetting(SettingsPage page, String keyBase) {
        page.intSetting(fieldId, keyBase, DEFAULT_LEVEL, MIN_LEVEL, MAX_LEVEL);
    }

    public void reload() {
        level = clamp(ModSettings.intOr(fieldId, DEFAULT_LEVEL));
    }

    public int level() {
        return level;
    }

    public void setForTests(int level) {
        this.level = level;
    }

    private static int clamp(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }
}
