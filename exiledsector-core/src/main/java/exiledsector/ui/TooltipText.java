package exiledsector.ui;

import exiledsector.ui.util.TextFont;

public record TooltipText(TextFont title, TextFont body, TextFont flavour, Table table, float maxTextWidth) {

    private static final float NODE_TEXT_WIDTH_FACTOR = 1.2f;

    public record Table(TextFont heading, TextFont cell, float headingHeight, float rowHeight) {
    }

    public float nodeMaxTextWidth() {
        return maxTextWidth * NODE_TEXT_WIDTH_FACTOR;
    }

    public boolean fontsLoaded() {
        return body.source().get() != null;
    }
}
