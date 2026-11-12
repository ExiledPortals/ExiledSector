package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.officer.OfficerText;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.TooltipText;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class AtlasSystemCard {

    static final float PADDING_X = 22f;
    static final float PADDING_Y = 14f;
    static final float MIN_CONTENT_WIDTH = 220f;
    static final float LINE_GAP = 3f;
    static final float SECTION_GAP = 12f;
    static final float PORTRAIT_SIZE = 40f;
    static final float PORTRAIT_GAP = 10f;
    static final float DIVIDER_ALPHA = 0.15f;
    static final float MAX_TEXT_HEIGHT = 800f;
    private static final StyledText LINE_BREAK = StyledText.of("\n");

    private final SpriteCache sprites;
    private final TooltipText tooltipText = SkillTreePanelStyle.tooltipText();
    private final TextLabel titleLabel = new TextLabel(tooltipText.title(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel layoutLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TextLabel holderLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TextLabel officerNameLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel officerRoleLabel = new TextLabel(tooltipText.hint(), AtlasScreen.HINT_COLOUR);
    private final TextLabel notesLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TextLabel statusLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private boolean hasOfficer;
    private String portraitPath;
    private float contentWidth;

    AtlasSystemCard(SpriteCache sprites) {
        this.sprites = sprites;
    }

    void write(AtlasNodeText.SystemCard card, StyledText verdict) {
        float maxWidth = tooltipText.maxTextWidth();
        titleLabel.setWrapped(card.title(), maxWidth, MAX_TEXT_HEIGHT);
        layoutLabel.setWrapped(card.layout(), maxWidth, MAX_TEXT_HEIGHT);
        writeHolder(card.factionId(), maxWidth);
        writeOfficer(card);
        setLines(notesLabel, styledLines(card.notes()), maxWidth);
        List<StyledText> status = new ArrayList<>();
        if (verdict != null) {
            status.add(verdict);
        }
        status.addAll(styledLines(card.status()));
        setLines(statusLabel, status, maxWidth);
        contentWidth = Math.max(MIN_CONTENT_WIDTH, widestLabel());
    }

    private void writeHolder(String factionId, float maxWidth) {
        if (factionId == null) {
            holderLabel.set("");
            return;
        }
        Color factionColour = primaryColour(faction(factionId));
        StyledText line = Translation.msg("chart.atlas.node.faction")
                .arg("faction", StyledText.styled(ChartFactions.displayName(factionId), Style.HIGHLIGHT)).styled();
        holderLabel.setParagraphs(List.of(line), maxWidth, MAX_TEXT_HEIGHT,
                style -> style == Style.HIGHLIGHT ? factionColour : SkillTreePanelStyle.standardHighlightColor(style));
    }

    private void writeOfficer(AtlasNodeText.SystemCard card) {
        hasOfficer = card.officer() != null;
        if (!hasOfficer) {
            return;
        }
        float textWidth = tooltipText.maxTextWidth() - PORTRAIT_SIZE - PORTRAIT_GAP;
        officerNameLabel.setWrapped(OfficerText.title(card.officer()), textWidth, MAX_TEXT_HEIGHT);
        officerNameLabel.setColor(primaryColour(OfficerText.faction(card.officer())));
        officerRoleLabel.setWrapped(Translation.text(card.commandCentre() ? "chart.atlas.card.commandCentre" : "chart.atlas.card.territory"),
                textWidth, MAX_TEXT_HEIGHT);
        portraitPath = OfficerText.portraitPath(card.officer());
    }

    private static List<StyledText> styledLines(List<AtlasLine> lines) {
        return lines.stream().map(AtlasLine::styled).toList();
    }

    private static void setLines(TextLabel label, List<StyledText> lines, float maxWidth) {
        if (lines.isEmpty()) {
            label.set("");
            return;
        }
        label.setParagraphs(List.of(StyledText.join(LINE_BREAK, lines)), maxWidth, MAX_TEXT_HEIGHT, SkillTreePanelStyle::standardHighlightColor);
    }

    private static FactionAPI faction(String factionId) {
        SectorAPI sector = Global.getSector();
        return sector == null ? null : sector.getFaction(factionId);
    }

    private static Color primaryColour(FactionAPI faction) {
        return faction == null ? Misc.getHighlightColor() : faction.getColor();
    }

    private float widestLabel() {
        float widest = Math.max(titleLabel.width(), Math.max(layoutLabel.width(), holderLabel.width()));
        if (hasOfficer) {
            widest = Math.max(widest, PORTRAIT_SIZE + PORTRAIT_GAP + Math.max(officerNameLabel.width(), officerRoleLabel.width()));
        }
        return Math.max(widest, Math.max(notesLabel.width(), statusLabel.width()));
    }

    private float headerHeight() {
        float height = titleLabel.height();
        for (TextLabel label : List.of(layoutLabel, holderLabel)) {
            if (!label.isEmpty()) {
                height += LINE_GAP + label.height();
            }
        }
        return height;
    }

    private float officerTextHeight() {
        return officerNameLabel.height() + LINE_GAP + officerRoleLabel.height();
    }

    private float officerHeight() {
        return hasOfficer ? SECTION_GAP + Math.max(PORTRAIT_SIZE, officerTextHeight()) : 0f;
    }

    private float notesHeight() {
        return notesLabel.isEmpty() ? 0f : SECTION_GAP + notesLabel.height();
    }

    private float statusHeight() {
        return statusLabel.isEmpty() ? 0f : SECTION_GAP * 2f + statusLabel.height();
    }

    private float boxHeight() {
        return (float) Math.ceil(headerHeight() + officerHeight() + notesHeight() + statusHeight() + PADDING_Y * 2f);
    }

    void draw(float mouseX, float mouseY, float alphaMult) {
        float boxWidth = (float) Math.ceil(contentWidth + PADDING_X * 2f);
        float boxHeight = boxHeight();
        float boxX = Math.round(OfficerBoardTooltip.boxLeft(mouseX, boxWidth, Global.getSettings().getScreenWidth()));
        float boxY = Math.round(OfficerBoardTooltip.boxBottom(mouseY, boxHeight, Global.getSettings().getScreenHeight()));
        BorderedPanel.draw(boxX, boxY, boxWidth, boxHeight, alphaMult);
        float left = boxX + PADDING_X;
        float top = boxY + boxHeight - PADDING_Y;
        top = drawHeader(left, top, alphaMult);
        if (hasOfficer) {
            top = drawOfficer(left, top - SECTION_GAP, alphaMult);
        }
        if (!notesLabel.isEmpty()) {
            top -= SECTION_GAP;
            notesLabel.setAlpha(alphaMult).draw(left, top);
            top -= notesLabel.height();
        }
        if (!statusLabel.isEmpty()) {
            top -= SECTION_GAP;
            GLDraw.fillQuad(left, top, contentWidth, 1f, Color.WHITE, DIVIDER_ALPHA * alphaMult);
            statusLabel.setAlpha(alphaMult).draw(left, top - SECTION_GAP);
        }
    }

    private float drawHeader(float left, float top, float alphaMult) {
        titleLabel.setColor(Misc.getTooltipTitleAndLightHighlightColor()).setAlpha(alphaMult).drawWithShadow(left, top, Color.BLACK);
        top -= titleLabel.height();
        for (TextLabel label : List.of(layoutLabel, holderLabel)) {
            if (!label.isEmpty()) {
                top -= LINE_GAP;
                label.setAlpha(alphaMult).draw(left, top);
                top -= label.height();
            }
        }
        return top;
    }

    private float drawOfficer(float left, float top, float alphaMult) {
        float rowHeight = Math.max(PORTRAIT_SIZE, officerTextHeight());
        SpriteAPI portrait = portraitPath == null ? null : sprites.texture(portraitPath);
        if (portrait != null) {
            SpriteDraw.drawDiscAtCenter(portrait, left + PORTRAIT_SIZE / 2f, top - rowHeight / 2f, PORTRAIT_SIZE / 2f, alphaMult);
        }
        float textLeft = left + PORTRAIT_SIZE + PORTRAIT_GAP;
        float textTop = top - (rowHeight - officerTextHeight()) / 2f;
        officerNameLabel.setAlpha(alphaMult).draw(textLeft, textTop);
        officerRoleLabel.setAlpha(alphaMult).draw(textLeft, textTop - officerNameLabel.height() - LINE_GAP);
        return top - rowHeight;
    }
}
