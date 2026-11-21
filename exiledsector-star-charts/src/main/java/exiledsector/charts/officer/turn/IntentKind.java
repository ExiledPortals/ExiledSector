package exiledsector.charts.officer.turn;

import java.util.Locale;

public enum IntentKind {
    EXPAND,
    PUSH,
    CHALLENGE,
    DEFEND,
    HOLD;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isConflict() {
        return this == PUSH || this == CHALLENGE;
    }

    public static IntentKind fromId(String id) {
        for (IntentKind kind : values()) {
            if (kind.id().equals(id)) {
                return kind;
            }
        }
        return null;
    }
}
