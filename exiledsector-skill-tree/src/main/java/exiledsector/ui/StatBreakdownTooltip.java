package exiledsector.ui;

import exiledlib.i18n.Translation;
import exiledsector.ui.stats.ShipStatSheet;
import exiledsector.ui.util.TextLabel;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class StatBreakdownTooltip {

    private static final float MAX_TITLE_HEIGHT = 200f;
    private static final float MAX_NOTES_HEIGHT = 200f;
    private static final float NOTES_FONT_SIZE = 17f;
    private static final Color NOTES_COLOR = new Color(0xA0, 0xA0, 0xA0);

    private final SkillTreePanelStyle panelStyle;
    private final TextLabel titleLabel = new TextLabel(SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel bodyLabel = new TextLabel(SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
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
        panelStyle.drawTitleBodyTooltip(titleLabel, null, bodyLabel, tables, notesLabel.isEmpty() ? null : notesLabel, mouseX, mouseY, alphaMult);
    }

    private void show(LazyFont font, ShipStatSheet.Breakdown breakdown) {
        shownBreakdown = breakdown;
        titleLabel.setWrapped(breakdown.title(), SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH, MAX_TITLE_HEIGHT);
        bodyLabel.set(breakdown.summary() == null ? "" : breakdown.summary());
        notesLabel.setWrapped(String.join("\n", breakdown.notes()), SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH, MAX_NOTES_HEIGHT);
        if (breakdown.sources().isEmpty()) {
            tables = List.of();
            return;
        }
        List<TooltipTable.Row> rows = new ArrayList<>();
        for (ShipStatSheet.SourceLine line : breakdown.sources()) {
            rows.add(TooltipTable.Row.of(line.fromTree(), line.source(), line.amount()));
        }
        TooltipTable table = new TooltipTable(Translation.text("ui.stats.breakdown.heading"),
                List.of(Translation.text("ui.stats.breakdown.source"), Translation.text("ui.stats.breakdown.amount")), rows);
        tables = List.of(SkillTreeTooltipTable.measure(font, table));
    }
}
