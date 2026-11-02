package exiledsector.items;

import java.awt.Color;

public enum ItemRarity {

    COMMON(new Color(120, 150, 255), 3),
    RARE(new Color(255, 225, 90), 5),
    UNIQUE(new Color(255, 140, 40), 10);

    private final Color color;
    private final int disassemblyFeedstock;

    ItemRarity(Color color, int disassemblyFeedstock) {
        this.color = color;
        this.disassemblyFeedstock = disassemblyFeedstock;
    }

    public Color color() {
        return color;
    }

    public int disassemblyFeedstock() {
        return disassemblyFeedstock;
    }
}
