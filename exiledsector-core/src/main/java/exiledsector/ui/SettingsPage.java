package exiledsector.ui;

import exiledsector.i18n.Translation;
import lunalib.lunaSettings.LunaSettings.SettingsCreator;

import java.util.List;

import static exiledsector.ExiledSector.MOD_ID;

public final class SettingsPage {

    private final String tab;

    public SettingsPage(String tab) {
        this.tab = tab;
    }

    public record Label(String nameKey, String tooltipKey) {

        public static Label of(String keyBase) {
            return new Label(keyBase + ".name", keyBase + ".tooltip");
        }

        public static Label untipped(String nameKey) {
            return new Label(nameKey, null);
        }

        String name() {
            return Translation.text(nameKey);
        }

        String tooltip() {
            return tooltipKey == null ? "" : Translation.text(tooltipKey);
        }
    }

    public void header(String headerId, String titleKey) {
        SettingsCreator.addHeader(MOD_ID, headerId, Translation.text(titleKey), tab);
    }

    public void text(String textId, String textKey) {
        SettingsCreator.addText(MOD_ID, textId, Translation.text(textKey), tab);
    }

    public void intSetting(String fieldId, String keyBase, int defaultValue, int minValue, int maxValue) {
        intSetting(fieldId, Label.of(keyBase), defaultValue, minValue, maxValue);
    }

    public void intSetting(String fieldId, Label label, int defaultValue, int minValue, int maxValue) {
        SettingsCreator.addInt(MOD_ID, fieldId, label.name(), label.tooltip(), defaultValue, minValue, maxValue, tab);
    }

    public void doubleSetting(String fieldId, String keyBase, double defaultValue, double minValue, double maxValue) {
        doubleSetting(fieldId, Label.of(keyBase), defaultValue, minValue, maxValue);
    }

    public void doubleSetting(String fieldId, Label label, double defaultValue, double minValue, double maxValue) {
        SettingsCreator.addDouble(MOD_ID, fieldId, label.name(), label.tooltip(), defaultValue, minValue, maxValue, tab);
    }

    public void booleanSetting(String fieldId, String keyBase, boolean defaultValue) {
        booleanSetting(fieldId, Label.of(keyBase), defaultValue);
    }

    public void booleanSetting(String fieldId, Label label, boolean defaultValue) {
        SettingsCreator.addBoolean(MOD_ID, fieldId, label.name(), label.tooltip(), defaultValue, tab);
    }

    public void radioSetting(String fieldId, String keyBase, String defaultOption, List<String> options) {
        Label label = Label.of(keyBase);
        SettingsCreator.addRadio(MOD_ID, fieldId, label.name(), label.tooltip(), defaultOption, String.join(",", options), tab);
    }

    public void keybindSetting(String fieldId, String keyBase, int defaultKey) {
        Label label = Label.of(keyBase);
        SettingsCreator.addKeybind(MOD_ID, fieldId, label.name(), label.tooltip(), defaultKey, tab);
    }
}
