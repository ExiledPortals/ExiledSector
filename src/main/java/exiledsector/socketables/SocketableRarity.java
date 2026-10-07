package exiledsector.socketables;

import java.awt.Color;

public enum SocketableRarity {

    COMMON(new Color(120, 150, 255), 1),
    RARE(new Color(255, 225, 90), 2),
    UNIQUE(new Color(255, 140, 40), 10);

    static final int COMMON_MAX_EFFECTS = 2;

    private final Color color;
    private final int disassemblyParts;

    SocketableRarity(Color color, int disassemblyParts) {
        this.color = color;
        this.disassemblyParts = disassemblyParts;
    }

    public Color color() {
        return color;
    }

    public int disassemblyParts() {
        return disassemblyParts;
    }

    static SocketableRarity of(SocketableDefinition definition, int effectCount) {
        if (definition != null && definition.unique()) {
            return UNIQUE;
        }
        return effectCount <= COMMON_MAX_EFFECTS ? COMMON : RARE;
    }
}
