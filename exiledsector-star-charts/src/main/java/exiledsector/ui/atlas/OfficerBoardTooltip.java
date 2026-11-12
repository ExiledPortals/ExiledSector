package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.officer.reward.OfficerRewardText;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.ScarText;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.TooltipText;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class OfficerBoardTooltip {

    static final float PADDING = 10f;
    static final float WIDTH_SAFETY_MARGIN = 8f;
    static final float TITLE_BODY_GAP = 6f;
    static final float CURSOR_OFFSET = 18f;
    static final float SECTION_GAP = 14f;
    static final float SEPARATOR_THICKNESS = 1f;
    static final float SCREEN_MARGIN = 4f;
    static final float FLAG_GAP = 8f;
    static final float MAX_FLAG_HEIGHT = 160f;
    static final float MAX_TEXT_HEIGHT = 800f;
    private static final StyledText LINE_BREAK = StyledText.of("\n");

    private final SpriteCache sprites;
    private final TooltipText tooltipText = SkillTreePanelStyle.tooltipText();
    private final TextLabel titleLabel = new TextLabel(tooltipText.title(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel bodyLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TextLabel flagshipLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private int shownRecordId = -1;
    private String shownRecord;
    private String flagPath;
    private float flagWidth;
    private float flagHeight;
    private boolean hasFlagship;

    OfficerBoardTooltip(SpriteCache sprites) {
        this.sprites = sprites;
    }

    void show(AtlasOfficer officer, AtlasGenerator generator) {
        String record = officer.encode();
        if (officer.recordId() == shownRecordId && record.equals(shownRecord)) {
            return;
        }
        shownRecordId = officer.recordId();
        shownRecord = record;
        I18n.forGameText(() -> write(officer, generator));
    }

    private void write(AtlasOfficer officer, AtlasGenerator generator) {
        float maxTextWidth = tooltipText.maxTextWidth();
        FactionAPI faction = OfficerText.faction(officer);
        measureFlag(faction == null ? null : faction.getLogo(), maxTextWidth);
        titleLabel.setWrapped(OfficerText.title(officer), maxTextWidth, MAX_TEXT_HEIGHT);
        bodyLabel.setParagraphs(List.of(StyledText.join(LINE_BREAK, officerLines(officer, faction, generator))), maxTextWidth, MAX_TEXT_HEIGHT,
                SkillTreePanelStyle::standardHighlightColor);
        List<StyledText> flagshipLines = flagshipLines(officer, faction);
        hasFlagship = !flagshipLines.isEmpty();
        flagshipLabel.setParagraphs(List.of(StyledText.join(LINE_BREAK, flagshipLines)), maxTextWidth, MAX_TEXT_HEIGHT,
                SkillTreePanelStyle::standardHighlightColor);
    }

    private void measureFlag(String path, float maxTextWidth) {
        flagPath = null;
        SpriteAPI natural = path == null || path.isEmpty() ? null : sprites.texture(path);
        if (natural == null || natural.getWidth() <= 0f || natural.getHeight() <= 0f) {
            return;
        }
        float scale = Math.min(1f, Math.min(maxTextWidth / natural.getWidth(), MAX_FLAG_HEIGHT / natural.getHeight()));
        flagPath = path;
        flagWidth = natural.getWidth() * scale;
        flagHeight = natural.getHeight() * scale;
    }

    private static List<StyledText> officerLines(AtlasOfficer officer, FactionAPI faction, AtlasGenerator generator) {
        List<StyledText> lines = new ArrayList<>();
        if (faction != null) {
            lines.add(Translation.msg("chart.officer.board.faction").arg("faction", faction.getDisplayName()).styled());
        }
        lines.add(Translation.msg("chart.officer.board.personality").arg("personality", OfficerText.traitName(officer)).styled());
        if (!officer.isActive()) {
            lines.add(Translation.styled("chart.officer.board.waiting"));
        }
        List<StyledText> scarNames = scarNames(officer.scars());
        if (!scarNames.isEmpty()) {
            lines.add(Translation.msg("chart.officer.board.scars").arg("scars", Translation.list(scarNames)).styled());
        }
        AtlasLine carried = OfficerRewardText.carriedLine(officer.carried());
        if (carried != null) {
            lines.add(carried.styled());
        }
        String history = generator == null ? "" : OfficerText.latestHistory(generator, officer);
        if (!history.isEmpty()) {
            lines.add(StyledText.of(history));
        }
        return lines;
    }

    private static List<StyledText> scarNames(Map<String, Integer> scars) {
        List<StyledText> names = new ArrayList<>();
        new TreeMap<>(scars).forEach((scarId, stacks) -> {
            if (stacks != null && stacks > 1) {
                names.add(Translation.msg("chart.officer.scar.stacked").arg("scar", ScarText.name(scarId)).arg("stacks", stacks).styled());
            } else if (stacks != null && stacks == 1) {
                names.add(StyledText.of(ScarText.name(scarId)));
            }
        });
        return names;
    }

    private static List<StyledText> flagshipLines(AtlasOfficer officer, FactionAPI faction) {
        ShipVariantAPI variant = OfficerFleets.flagshipVariant(officer);
        if (variant == null) {
            return List.of();
        }
        List<StyledText> lines = new ArrayList<>();
        String shipName = faction == null ? "" : OfficerFleets.flagshipName(officer, faction);
        lines.add(Translation.msg("chart.officer.board.flagship").arg("name", shipName == null ? "" : shipName)
                .arg("hull", variant.getHullSpec().getNameWithDesignationWithDashClass()).styled());
        List<StyledText> dmodNames = dmodNames(officer.dmodIds());
        if (!dmodNames.isEmpty()) {
            lines.add(Translation.msg("chart.officer.board.dmods").arg("dmods", Translation.list(dmodNames)).styled());
        }
        lines.addAll(ScarText.tooltipLines(new TreeMap<>(officer.scars()), HullFacts.of(variant)));
        return lines;
    }

    private static List<StyledText> dmodNames(List<String> dmodIds) {
        List<StyledText> names = new ArrayList<>();
        for (String dmodId : dmodIds) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(dmodId);
            names.add(StyledText.of(spec == null ? dmodId : spec.getDisplayName()));
        }
        return names;
    }

    void draw(Color accent, float mouseX, float mouseY, float alphaMult) {
        float flagRoom = flagPath == null ? 0f : flagHeight + FLAG_GAP;
        float flagshipRoom = hasFlagship ? SECTION_GAP * 2f + SEPARATOR_THICKNESS + flagshipLabel.height() : 0f;
        float contentWidth = Math.max(Math.max(titleLabel.width(), bodyLabel.width()), flagPath == null ? 0f : flagWidth);
        if (hasFlagship) {
            contentWidth = Math.max(contentWidth, flagshipLabel.width());
        }
        float boxWidth = (float) Math.ceil(contentWidth + PADDING * 2f + WIDTH_SAFETY_MARGIN);
        float boxHeight = (float) Math.ceil(flagRoom + titleLabel.height() + TITLE_BODY_GAP + bodyLabel.height() + flagshipRoom + PADDING * 2f);
        float boxX = Math.round(boxLeft(mouseX, boxWidth, Global.getSettings().getScreenWidth()));
        float boxY = Math.round(Math.max(SCREEN_MARGIN, mouseY - boxHeight - CURSOR_OFFSET));
        GLDraw.fillQuad(boxX, boxY, boxWidth, boxHeight, SkillTreePanelStyle.TOOLTIP_BACKGROUND_COLOR, alphaMult);
        GLDraw.strokeQuad(boxX, boxY, boxWidth, boxHeight, accent, SkillTreePanelStyle.TOOLTIP_BORDER_THICKNESS, alphaMult);
        float top = boxY + boxHeight - PADDING;
        if (flagPath != null) {
            SpriteDraw.drawAtCenter(sprites, flagPath, boxX + boxWidth / 2f, top - flagHeight / 2f, flagWidth, flagHeight, Color.WHITE, alphaMult);
            top -= flagRoom;
        }
        titleLabel.setColor(Misc.getTooltipTitleAndLightHighlightColor()).setAlpha(alphaMult)
                .drawWithShadow(boxX + (boxWidth - titleLabel.width()) / 2f, top, Color.BLACK);
        top -= titleLabel.height() + TITLE_BODY_GAP;
        bodyLabel.setAlpha(alphaMult).draw(boxX + PADDING, top);
        top -= bodyLabel.height();
        if (hasFlagship) {
            top -= SECTION_GAP + SEPARATOR_THICKNESS;
            GLDraw.fillQuad(boxX + PADDING, top, boxWidth - PADDING * 2f, SEPARATOR_THICKNESS, accent, alphaMult);
            top -= SECTION_GAP;
            flagshipLabel.setAlpha(alphaMult).draw(boxX + PADDING, top);
        }
    }

    static float boxLeft(float mouseX, float boxWidth, float screenWidth) {
        float rightOfCursor = mouseX + CURSOR_OFFSET;
        if (rightOfCursor + boxWidth <= screenWidth - SCREEN_MARGIN) {
            return rightOfCursor;
        }
        return Math.max(SCREEN_MARGIN, mouseX - CURSOR_OFFSET - boxWidth);
    }
}
