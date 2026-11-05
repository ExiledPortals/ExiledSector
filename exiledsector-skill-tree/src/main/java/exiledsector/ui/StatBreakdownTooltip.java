package exiledsector.ui;

import exiledsector.i18n.Translation;
import exiledsector.ui.stats.ShipStatSheet;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class StatBreakdownTooltip {

    private static final float MAX_TITLE_HEIGHT = 200f;
    private static final float MAX_NOTES_HEIGHT = 200f;
    private static final Color NOTES_COLOR = new Color(0xA0, 0xA0, 0xA0);
    private static final List<ShipStatSheet.Combine> VALUE_COLUMNS = List.of(ShipStatSheet.Combine.FLAT, ShipStatSheet.Combine.PERCENT,
            ShipStatSheet.Combine.MULTI);

    private final SkillTreePanelStyle panelStyle;
    private final TooltipText tooltipText = SkillTreePanelStyle.tooltipText();
    private final TextLabel titleLabel = new TextLabel(tooltipText.title(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel bodyLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TextLabel resultLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel notesLabel = new TextLabel(tooltipText.flavour(), NOTES_COLOR);
    private ShipStatSheet.Breakdown shownBreakdown;
    private List<SkillTreeTooltipTable> tables = List.of();

    StatBreakdownTooltip(SkillTreePanelStyle panelStyle) {
        this.panelStyle = panelStyle;
    }

    void render(ShipStatSheet.Breakdown breakdown, float mouseX, float mouseY, float alphaMult) {
        if (!tooltipText.fontsLoaded() || breakdown == null) {
            return;
        }
        if (breakdown != shownBreakdown) {
            show(breakdown);
        }
        panelStyle.drawTitleBodyTooltip(titleLabel, null, bodyLabel, tables, resultLabel.isEmpty() ? null : resultLabel,
                notesLabel.isEmpty() ? null : notesLabel, mouseX, mouseY, alphaMult);
    }

    private void show(ShipStatSheet.Breakdown breakdown) {
        shownBreakdown = breakdown;
        float maxTextWidth = tooltipText.maxTextWidth();
        titleLabel.setWrapped(breakdown.title(), maxTextWidth, MAX_TITLE_HEIGHT);
        bodyLabel.setWrapped(breakdown.summary() == null ? "" : breakdown.summary(), maxTextWidth, MAX_NOTES_HEIGHT);
        resultLabel.setWrapped(breakdown.result() == null ? "" : breakdown.result(), maxTextWidth, MAX_NOTES_HEIGHT);
        notesLabel.setWrapped(String.join("\n", breakdown.notes()), maxTextWidth, MAX_NOTES_HEIGHT);
        if (breakdown.sources().isEmpty()) {
            tables = List.of();
            return;
        }
        boolean combined = breakdown.sources().stream().anyMatch(line -> line.combine() != ShipStatSheet.Combine.OTHER);
        TooltipTable table = new TooltipTable(Translation.text("ui.stats.breakdown.heading"), combined ? combinedHeaders() : amountHeaders(),
                combined ? combinedRows(breakdown.sources()) : amountRows(breakdown.sources()));
        tables = List.of(SkillTreeTooltipTable.measure(tooltipText, table));
    }

    private static List<String> combinedHeaders() {
        return List.of(Translation.text("ui.stats.breakdown.source"), Translation.text("ui.stats.breakdown.flat"),
                Translation.text("ui.stats.breakdown.percent"), Translation.text("ui.stats.breakdown.multi"));
    }

    private static List<String> amountHeaders() {
        return List.of(Translation.text("ui.stats.breakdown.source"), Translation.text("ui.stats.breakdown.amount"));
    }

    private static List<TooltipTable.Row> combinedRows(List<ShipStatSheet.SourceLine> lines) {
        List<TooltipTable.Row> rows = new ArrayList<>();
        for (ShipStatSheet.SourceLine line : lines) {
            List<String> cells = new ArrayList<>();
            cells.add(line.source());
            VALUE_COLUMNS.forEach(column -> cells.add(column == line.combine() ? line.amount() : ""));
            rows.add(new TooltipTable.Row(List.copyOf(cells), line.fromTree()));
        }
        return rows;
    }

    private static List<TooltipTable.Row> amountRows(List<ShipStatSheet.SourceLine> lines) {
        List<TooltipTable.Row> rows = new ArrayList<>();
        lines.forEach(line -> rows.add(TooltipTable.Row.of(line.fromTree(), line.source(), line.amount())));
        return rows;
    }
}
