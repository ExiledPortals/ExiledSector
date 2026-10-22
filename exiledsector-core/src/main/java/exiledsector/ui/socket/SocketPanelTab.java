package exiledsector.ui.socket;

import exiledsector.i18n.Translation;

public enum SocketPanelTab {
    HULL_ITEMS("ui.socketPanel.tab.socketables"),
    HULL_UPGRADES("ui.socketPanel.tab.hullUpgrades");

    private final String labelKey;

    SocketPanelTab(String labelKey) {
        this.labelKey = labelKey;
    }

    String labelKey() {
        return labelKey;
    }

    String label() {
        return Translation.text(labelKey);
    }
}
