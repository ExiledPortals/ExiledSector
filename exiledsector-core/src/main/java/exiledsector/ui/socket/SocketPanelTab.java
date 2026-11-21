package exiledsector.ui.socket;

import exiledsector.i18n.Translation;

public enum SocketPanelTab {
    HULL_ITEMS("ui.socketPanel.tab.hullItems"),
    HULL_UPGRADES("ui.socketPanel.tab.hullUpgrades"),
    HULL_PATTERNS("ui.socketPanel.tab.hullPatterns");

    private final String labelKey;

    SocketPanelTab(String labelKey) {
        this.labelKey = labelKey;
    }

    String labelKey() {
        return labelKey;
    }

    String shortLabelKey() {
        return labelKey + ".short";
    }

    String label() {
        return Translation.text(labelKey);
    }

    String shortLabel() {
        return Translation.text(shortLabelKey());
    }
}
