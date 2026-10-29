package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.List;

final class ChipFlow {

    static final float CHIP_HEIGHT = 24f;
    private static final float CHIP_TEXT_PADDING = 28f;
    private static final float CHIP_DARK_SCALE = 0.3f;

    record Colors(Color base, Color dark, Color bright) {

        static Colors of(Color base) {
            return of(base, base.brighter());
        }

        static Colors of(Color base, Color bright) {
            return new Colors(base, Misc.scaleColorOnly(base, CHIP_DARK_SCALE), bright);
        }

        static Colors player() {
            return new Colors(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor());
        }
    }

    private final TooltipMakerAPI element;
    private final float rowWidth;
    private float chipLeft;
    private int chipRow;

    ChipFlow(TooltipMakerAPI element, float rowWidth) {
        this.element = element;
        this.rowWidth = rowWidth;
    }

    static float width(String label) {
        return Global.getSettings().computeStringWidth(label, Fonts.ORBITRON_12) + CHIP_TEXT_PADDING;
    }

    static int rowsFor(List<String> labels, float rowWidth) {
        if (labels.isEmpty()) {
            return 0;
        }
        int rows = 1;
        float left = 0f;
        for (String label : labels) {
            float chipWidth = width(label);
            if (left > 0f && left + chipWidth > rowWidth) {
                left = 0f;
                rows++;
            }
            left += chipWidth + HoloPanel.GAP;
        }
        return rows;
    }

    static float uniformWidth(List<String> labels, float rowWidth) {
        float widest = 0f;
        for (String label : labels) {
            widest = Math.max(widest, width(label));
        }
        int columns = Math.max(1, (int) ((rowWidth + HoloPanel.GAP) / (widest + HoloPanel.GAP)));
        return (rowWidth - (columns - 1) * HoloPanel.GAP) / columns;
    }

    static int uniformRows(int chipCount, float chipWidth, float rowWidth) {
        int columns = Math.max(1, (int) ((rowWidth + HoloPanel.GAP) / (chipWidth + HoloPanel.GAP)));
        return Math.max(1, (chipCount + columns - 1) / columns);
    }

    static float height(int rows) {
        return rows * CHIP_HEIGHT + (rows - 1) * HoloPanel.GAP;
    }

    static float rowsHeight(int rows) {
        return rows * (CHIP_HEIGHT + HoloPanel.GAP);
    }

    void place(String label, Object chipId, Colors colors, boolean checked) {
        place(label, chipId, colors, checked, width(label));
    }

    void place(String label, Object chipId, Colors colors, boolean checked, float chipWidth) {
        if (chipLeft > 0f && chipLeft + chipWidth > rowWidth + 0.5f) {
            nextRow();
        }
        placeInRow(label, chipId, colors, checked, chipWidth);
    }

    void placeInRow(String label, Object chipId, Colors colors, boolean checked) {
        placeInRow(label, chipId, colors, checked, width(label));
    }

    private void placeInRow(String label, Object chipId, Colors colors, boolean checked, float chipWidth) {
        ButtonAPI chip = element.addAreaCheckbox(label, chipId, colors.base(), colors.dark(), colors.bright(), chipWidth, CHIP_HEIGHT, 0f);
        chip.setChecked(checked);
        chip.getPosition().inTL(chipLeft, chipRow * (CHIP_HEIGHT + HoloPanel.GAP));
        chipLeft += chipWidth + HoloPanel.GAP;
    }

    void nextRow() {
        chipLeft = 0f;
        chipRow++;
    }

    void startRow(int row) {
        chipLeft = 0f;
        chipRow = row;
    }
}
