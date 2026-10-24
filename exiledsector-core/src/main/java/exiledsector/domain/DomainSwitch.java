package exiledsector.domain;

import exiledlib.ModSettings;
import exiledlib.i18n.Translation;
import exiledsector.ui.ExiledSectorSettings;
import lunalib.lunaSettings.LunaSettings.SettingsCreator;

import static exiledsector.ExiledSector.MOD_ID;

public final class DomainSwitch {

    private final String fieldId;
    private final boolean defaultOn;
    private final Runnable onSwitched;
    private volatile boolean switchedOn;

    public DomainSwitch(String fieldId, boolean defaultOn, Runnable onSwitched) {
        this.fieldId = fieldId;
        this.defaultOn = defaultOn;
        this.onSwitched = onSwitched;
        this.switchedOn = defaultOn;
    }

    public void addSettings(String headerId, String headerKey, String nameKey, String tooltipKey) {
        SettingsCreator.addHeader(MOD_ID, headerId, Translation.text(headerKey), ExiledSectorSettings.MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, fieldId, Translation.text(nameKey), Translation.text(tooltipKey), defaultOn, ExiledSectorSettings.MAIN_TAB);
    }

    public void reload() {
        boolean on = ModSettings.booleanOr(fieldId, defaultOn);
        boolean switched = on != switchedOn;
        switchedOn = on;
        if (switched) {
            onSwitched.run();
        }
    }

    public boolean on() {
        return switchedOn;
    }

    public void setForTests(boolean on) {
        switchedOn = on;
    }
}
