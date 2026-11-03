package exiledsector.ui;

import exiledsector.i18n.Translation;
import exiledsector.ui.stats.ShipStatSheet;
import exiledsector.ui.util.TextLabel;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class StatBreakdownTooltip {

    private static final float MAX_TITLE_HEIGHT = 200f;
    private static final float MAX_NOTES_HEIGHT = 200f;
    private static final float NOTES_FONT_SIZE = 17f;
    private static final Color NOTES_COLOR = new Color(0xA0, 0xA0, 0xA0);
    private static final Map<ShipStatSheet.Combine, String> HEADING_KEYS = Map.of(
            ShipStatSheet.Combine.INCREASED, "ui.stats.breakdown.increased",
            ShipStatSheet.Combine.ADDED, "ui.stats.breakdown.added",
            ShipStatSheet.Combine.MORE, "ui.stats.breakdown.more",
            ShipStatSheet.Combine.OTHER, "ui.stats.breakdown.heading");

    private final SkillTreePanelStyle panelStyle;
    private final TextLabel titleLabel = new TextLabel(SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel bodyLabel = new TextLabel(SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TextLabel resultLabel = new TextLabel(SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel notesLabel = new TextLabel(NOTES_FONT_SIZE, NOTES_COLOR);
    private ShipStatSheet.Breakdown shownBreakdown;
    private List<SkillTreeTooltipTable> tables = List.of();

    StatBreakdownTooltip(SkillTreePanelStyle panelStyle) {
        this.panelStyle = panelStyle;
    }

    void render(ShipStatSheet.Breakdown breakdown, float mouseX, float mouseY, float alphaMult) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null || breakdown == null) {
            return;
        }
        if (breakdown != shownBreakdown) {
            show(font, breakdown);
        }
        panelStyle.drawTitleBodyTooltip(titleLabel, null, bodyLabel, tables, resultLabel.isEmpty() ? null : resultLabel,
                notesLabel.isEmpty() ? null : notesLabel, mouseX, mouseY, alphaMult);
    }

    private void show(LazyFont font, ShipStatSheet.Breakdown breakdown) {
        shownBreakdown = breakdown;
        titleLabel.setWrapped(breakdown.title(), SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH, MAX_TITLE_HEIGHT);
        bodyLabel.setWrapped(breakdown.summary() == null ? "" : breakdown.summary(), SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH, MAX_NOTES_HEIGHT);
        resultLabel.setWrapped(breakdown.result() == null ? "" : breakdown.result(), SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH, MAX_NOTES_HEIGHT);
        notesLabel.setWrapped(String.join("\n", breakdown.notes()), SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH, MAX_NOTES_HEIGHT);
        List<TooltipTable> groupTables = new ArrayList<>();
        for (ShipStatSheet.SourceGroup group : breakdown.groups()) {
            groupTables.add(new TooltipTable(Translation.text(HEADING_KEYS.get(group.combine())), List.of(), rows(group)));
        }
        tables = SkillTreeTooltipTable.measureAligned(font, groupTables);
    }

    private static List<TooltipTable.Row> rows(ShipStatSheet.SourceGroup group) {
        List<TooltipTable.Row> rows = new ArrayList<>();
        for (ShipStatSheet.SourceLine line : group.lines()) {
            rows.add(TooltipTable.Row.of(line.fromTree(), line.source(), line.amount()));
            for (ShipStatSheet.SourceLine part : line.parts()) {
                rows.add(TooltipTable.Row.of(part.fromTree() ? TooltipTable.Tone.DETAIL : TooltipTable.Tone.PLAIN, part.source(), part.amount()));
            }
        }
        if (group.total() != null) {
            rows.add(TooltipTable.Row.of(TooltipTable.Tone.TOTAL, Translation.text("ui.stats.breakdown.total"), group.total()));
        }
        return rows;
    }
}
