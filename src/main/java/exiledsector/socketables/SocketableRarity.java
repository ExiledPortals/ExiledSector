package exiledsector.socketables;

import java.awt.Color;

public enum SocketableRarity {

    COMMON(new Color(120, 150, 255)),
    RARE(new Color(255, 225, 90)),
    UNIQUE(new Color(255, 140, 40));

    static final int COMMON_MAX_EFFECTS = 2;

    private final Color color;

    SocketableRarity(Color color) {
        this.color = color;
    }

    public Color color() {
        return color;
    }

    static SocketableRarity of(SocketableDefinition definition, int effectCount) {
        if (definition != null && definition.unique()) {
            return UNIQUE;
        }
        return effectCount <= COMMON_MAX_EFFECTS ? COMMON : RARE;
    }
}
