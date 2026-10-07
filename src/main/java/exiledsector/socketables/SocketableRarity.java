package exiledsector.socketables;

import java.awt.Color;

public enum SocketableRarity {

    COMMON(new Color(120, 150, 255), 1),
    RARE(new Color(255, 225, 90), 2),
    UNIQUE(new Color(255, 140, 40), 10);

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
}
