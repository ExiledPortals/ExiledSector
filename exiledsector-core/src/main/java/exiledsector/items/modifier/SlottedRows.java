package exiledsector.items.modifier;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.function.Consumer;

public final class SlottedRows {

    public record Layout(float rowWidth, float captionTagGap, float captionPad, Consumer<TooltipMakerAPI> cellFont) {
    }

    private SlottedRows() {
    }

    public static void add(TooltipMakerAPI tooltip, List<SlottedLine> rows, Layout layout, float firstPad, float linePad) {
        add(tooltip, rows, layout, firstPad, linePad, row -> {
        });
    }

    public static void add(TooltipMakerAPI tooltip, List<SlottedLine> rows, Layout layout, float firstPad, float linePad,
                           Consumer<SlottedLine> afterRowText) {
        float rowPad = firstPad;
        boolean first = true;
        for (SlottedLine row : rows) {
            if (row.caption() && !first) {
                rowPad = Math.max(rowPad, layout.captionPad());
            }
            TooltipMakerAPI textCell = tooltip.beginSubTooltip(layout.rowWidth());
            layout.cellFont().accept(textCell);
            boolean tagsBeside = row.captionTags() == null || fitsBeside(textCell, row, layout);
            VanillaText.addPara(textCell, row.text(), 0f, row.caption() ? Misc.getGrayColor() : Misc.getTextColor());
            tooltip.endSubTooltip();
            tooltip.addCustom(textCell, rowPad);
            afterRowText.accept(row);
            rowPad = linePad;
            first = false;
            if (row.captionTags() == null) {
                continue;
            }
            TooltipMakerAPI tagCell = tooltip.beginSubTooltip(layout.rowWidth());
            layout.cellFont().accept(tagCell);
            VanillaText.addPara(tagCell, row.captionTags(), 0f, Misc.getGrayColor()).setAlignment(Alignment.RMID);
            tooltip.endSubTooltip();
            if (tagsBeside) {
                tooltip.addCustomDoNotSetPosition(tagCell).getPosition().rightOfTop(textCell, -layout.rowWidth());
            } else {
                tooltip.addCustom(tagCell, 0f);
            }
        }
    }

    private static boolean fitsBeside(TooltipMakerAPI cell, SlottedLine row, Layout layout) {
        float labelsWidth = row.text().isEmpty() ? 0f : cell.computeStringWidth(row.text().plain()) + layout.captionTagGap();
        return labelsWidth + cell.computeStringWidth(row.captionTags().plain()) <= layout.rowWidth();
    }
}
