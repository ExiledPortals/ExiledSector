package exiledsector.items;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.function.Consumer;

public final class SlottedRows {

    public record Layout(float rowWidth, float slotTagWidth, float slotTagGap, Consumer<TooltipMakerAPI> cellFont) {
    }

    private SlottedRows() {
    }

    public static void add(TooltipMakerAPI tooltip, List<SlottedLine> rows, Layout layout, float firstPad, float linePad) {
        add(tooltip, rows, layout, firstPad, linePad, row -> {
        });
    }

    public static void add(TooltipMakerAPI tooltip, List<SlottedLine> rows, Layout layout, float firstPad, float linePad,
                           Consumer<SlottedLine> afterRowText) {
        float textWidth = layout.rowWidth() - layout.slotTagWidth() - layout.slotTagGap();
        float rowPad = firstPad;
        for (SlottedLine row : rows) {
            TooltipMakerAPI textCell = tooltip.beginSubTooltip(textWidth);
            layout.cellFont().accept(textCell);
            VanillaText.addPara(textCell, row.text(), 0f, Misc.getTextColor());
            tooltip.endSubTooltip();
            tooltip.addCustom(textCell, rowPad);
            afterRowText.accept(row);
            rowPad = linePad;
            if (row.slot() == null) {
                continue;
            }
            TooltipMakerAPI slotCell = tooltip.beginSubTooltip(layout.slotTagWidth());
            layout.cellFont().accept(slotCell);
            VanillaText.plainPara(slotCell, Translation.text(slotTagKey(row.slot())), 0f, Misc.getGrayColor()).setAlignment(Alignment.RMID);
            tooltip.endSubTooltip();
            tooltip.addCustomDoNotSetPosition(slotCell).getPosition().rightOfTop(textCell, layout.slotTagGap());
        }
    }

    public static String slotTagKey(ModifierSlot slot) {
        return slot == ModifierSlot.PREFIX ? "item.tooltip.slotTag.prefix" : "item.tooltip.slotTag.suffix";
    }
}
