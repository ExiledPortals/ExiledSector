package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardText;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.ScarText;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.TooltipText;
import exiledsector.ui.decoration.RefitShipPortrait;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

final class OfficerBoardTooltip {

    static final float PADDING = 10f;
    static final float CURSOR_OFFSET = 18f;
    static final float SCREEN_MARGIN = 4f;
    static final float PORTRAIT_SHARE = 0.2f;
    static final float FLAG_HEIGHT_SHARE = 0.7f;
    static final float HEADER_GAP = 8f;
    static final float LINE_GAP = 4f;
    static final float SECTION_GAP = 10f;
    static final float HEADING_PAD = 2f;
    static final float HEADING_CONTENT_GAP = 6f;
    static final float ICON_SIZE = 40f;
    static final float ICON_GAP = 8f;
    static final float ROW_GAP = 6f;
    static final float FRAME_WIDTH = 1.5f;
    static final float SHIP_MAX_HEIGHT = 240f;
    static final float SHIP_MIN_HEIGHT = 80f;
    static final float SHIP_GAP = 8f;
    static final float MAX_TEXT_HEIGHT = 800f;
    static final Color SLOT_FILL = new Color(20, 24, 30);
    static final Color FALLBACK_HEADING_BAR = new Color(40, 44, 50);
    private static final StyledText LINE_BREAK = StyledText.of("\n");

    private record RewardRow(RewardFace face, TextLabel nameLabel, TextLabel descriptionLabel) {

        float height() {
            float textHeight = nameLabel.height() + (descriptionLabel.isEmpty() ? 0f : LINE_GAP + descriptionLabel.height());
            return Math.max(ICON_SIZE, textHeight);
        }
    }

    private final SpriteCache sprites;
    private final TooltipText tooltipText = SkillTreePanelStyle.tooltipText();
    private final TextLabel titleLabel = new TextLabel(tooltipText.title(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel detailsLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TextLabel rewardsHeading = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel scarsHeading = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel flagshipHeading = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel scarsLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TextLabel flagshipLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TextLabel codexHintLabel = new TextLabel(tooltipText.hint(), AtlasScreen.HINT_COLOUR);
    private final List<RewardRow> rewardRows = new ArrayList<>();
    private int shownRecordId = -1;
    private String shownRecord;
    private String portraitPath;
    private String flagPath;
    private float flagWidth;
    private float flagHeight;
    private Color factionColour = OfficerBoard.FALLBACK_FRAME_COLOUR;
    private Color headingBarColour = FALLBACK_HEADING_BAR;
    private boolean hasScars;
    private boolean hasFlagship;
    private FleetMemberAPI flagship;
    private RefitShipPortrait shipPortrait;
    private float shipZoom;

    OfficerBoardTooltip(SpriteCache sprites) {
        this.sprites = sprites;
    }

    void show(AtlasOfficer officer) {
        String record = officer.encode();
        if (officer.recordId() == shownRecordId && record.equals(shownRecord)) {
            return;
        }
        cleanup();
        shownRecordId = officer.recordId();
        shownRecord = record;
        I18n.forGameText(() -> write(officer));
    }

    FleetMemberAPI flagshipOf(int recordId) {
        return recordId == shownRecordId ? flagship : null;
    }

    void openCodex(int recordId) {
        FleetMemberAPI shown = flagshipOf(recordId);
        if (shown != null && !Global.getSettings().isShowingCodex()) {
            Global.getSettings().showCodex(shown);
        }
    }

    void cleanup() {
        if (shipPortrait != null) {
            shipPortrait.cleanup();
        }
        shipPortrait = null;
        flagship = null;
        shownRecordId = -1;
        shownRecord = null;
    }

    private float contentWidth() {
        return tooltipText.nodeMaxTextWidth();
    }

    private float portraitSize() {
        return Math.round(contentWidth() * PORTRAIT_SHARE);
    }

    private float headerTextWidth() {
        float flagRoom = flagPath == null ? 0f : flagWidth + HEADER_GAP;
        return contentWidth() - portraitSize() - HEADER_GAP - flagRoom;
    }

    private void write(AtlasOfficer officer) {
        FactionAPI faction = OfficerText.faction(officer);
        factionColour = faction == null ? OfficerBoard.FALLBACK_FRAME_COLOUR : faction.getBaseUIColor();
        headingBarColour = faction == null ? FALLBACK_HEADING_BAR : faction.getDarkUIColor();
        portraitPath = OfficerText.portraitPath(officer);
        measureFlag(faction == null ? null : faction.getLogo());
        titleLabel.setWrapped(OfficerText.title(officer), headerTextWidth(), MAX_TEXT_HEIGHT);
        detailsLabel.setParagraphs(List.of(StyledText.join(LINE_BREAK, detailLines(officer, faction))), headerTextWidth(), MAX_TEXT_HEIGHT,
                SkillTreePanelStyle::standardHighlightColor);
        rewardsHeading.set(Translation.text("chart.officer.board.rewards"));
        scarsHeading.set(Translation.text("chart.officer.board.scars"));
        flagshipHeading.set(Translation.text("chart.officer.board.flagshipHeading"));
        writeRewards(officer.carried());
        ShipVariantAPI variant = OfficerFleets.flagshipVariant(officer);
        List<StyledText> scarLines = ScarText.tooltipLines(new TreeMap<>(officer.scars()), HullFacts.of(variant));
        hasScars = !scarLines.isEmpty();
        scarsLabel.setParagraphs(List.of(StyledText.join(LINE_BREAK, scarLines)), contentWidth(), MAX_TEXT_HEIGHT,
                SkillTreePanelStyle::standardHighlightColor);
        writeFlagship(officer, faction, variant);
        fitToScreen(Global.getSettings().getScreenHeight());
    }

    private void fitToScreen(float screenHeight) {
        float overflow = boxHeight() - (screenHeight - SCREEN_MARGIN * 2f);
        float shipHeight = shipHeight();
        if (overflow > 0f && shipHeight > 0f) {
            float fitted = fittedShipHeight(shipHeight, overflow);
            shipZoom *= fitted / shipHeight;
            overflow -= shipHeight - fitted;
        }
        if (overflow > 0f) {
            rewardRows.forEach(row -> row.descriptionLabel().set(""));
        }
    }

    static float fittedShipHeight(float shipHeight, float overflow) {
        return Math.min(shipHeight, Math.max(SHIP_MIN_HEIGHT, shipHeight - overflow));
    }

    private float boxHeight() {
        return (float) Math.ceil(headerHeight() + rewardsHeight() + scarsHeight() + flagshipHeight() + PADDING * 2f);
    }

    private void measureFlag(String path) {
        flagPath = null;
        SpriteAPI natural = path == null || path.isEmpty() ? null : sprites.texture(path);
        if (natural == null || natural.getWidth() <= 0f || natural.getHeight() <= 0f) {
            return;
        }
        float maxWidth = portraitSize();
        float maxHeight = portraitSize() * FLAG_HEIGHT_SHARE;
        float scale = Math.min(maxWidth / natural.getWidth(), maxHeight / natural.getHeight());
        flagPath = path;
        flagWidth = natural.getWidth() * scale;
        flagHeight = natural.getHeight() * scale;
    }

    static List<StyledText> detailLines(AtlasOfficer officer, FactionAPI faction) {
        List<StyledText> lines = new ArrayList<>();
        if (faction != null) {
            lines.add(Translation.msg("chart.officer.board.faction").arg("faction", faction.getDisplayName()).styled());
        }
        lines.add(Translation.msg("chart.officer.board.personality").arg("personality", OfficerText.traitName(officer)).styled());
        if (!officer.isActive()) {
            lines.add(Translation.styled("chart.officer.board.waiting"));
        }
        return lines;
    }

    private void writeRewards(List<CarriedItem> carried) {
        rewardRows.clear();
        float textWidth = contentWidth() - ICON_SIZE - ICON_GAP;
        for (CarriedItem item : carried) {
            TextLabel nameLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
            String name = OfficerRewardText.name(item);
            nameLabel.setWrapped(item.baseCount() > 1
                    ? Translation.msg("chart.officer.board.rewardCount").arg("item", name).arg("count", item.baseCount()).text()
                    : name, textWidth, MAX_TEXT_HEIGHT);
            TextLabel descriptionLabel = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
            StyledText description = RewardFace.description(item);
            if (description != null) {
                descriptionLabel.setParagraphs(List.of(description), textWidth, MAX_TEXT_HEIGHT, SkillTreePanelStyle::standardHighlightColor);
            }
            rewardRows.add(new RewardRow(RewardFace.of(item), nameLabel, descriptionLabel));
        }
    }

    private void writeFlagship(AtlasOfficer officer, FactionAPI faction, ShipVariantAPI variant) {
        hasFlagship = variant != null;
        if (!hasFlagship) {
            return;
        }
        List<StyledText> lines = new ArrayList<>();
        String shipName = faction == null ? "" : OfficerFleets.flagshipName(officer, faction);
        lines.add(Translation.msg("chart.officer.board.flagship").arg("name", shipName == null ? "" : shipName)
                .arg("hull", variant.getHullSpec().getNameWithDesignationWithDashClass()).styled());
        List<StyledText> dmodNames = dmodNames(officer.dmodIds());
        if (!dmodNames.isEmpty()) {
            lines.add(Translation.msg("chart.officer.board.dmods").arg("dmods", Translation.list(dmodNames)).styled());
        }
        flagshipLabel.setParagraphs(List.of(StyledText.join(LINE_BREAK, lines)), contentWidth(), MAX_TEXT_HEIGHT,
                SkillTreePanelStyle::standardHighlightColor);
        flagship = OfficerFleets.previewFlagship(officer);
        shipPortrait = flagship == null ? null : RefitShipPortrait.of(flagship, flagship.getVariant());
        shipZoom = shipPortrait == null ? 0f : shipZoom(shipPortrait.halfWidthPixels(1f), shipPortrait.halfLengthPixels(1f), contentWidth());
        codexHintLabel.set(flagship == null ? "" : Translation.text("chart.officer.board.codexHint"));
    }

    static float shipZoom(float halfWidth, float halfLength, float contentWidth) {
        if (halfWidth <= 0f || halfLength <= 0f) {
            return 0f;
        }
        return Math.min(1f, Math.min(contentWidth / (halfWidth * 2f), SHIP_MAX_HEIGHT / (halfLength * 2f)));
    }

    private static List<StyledText> dmodNames(List<String> dmodIds) {
        List<StyledText> names = new ArrayList<>();
        for (String dmodId : dmodIds) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(dmodId);
            names.add(StyledText.of(spec == null ? dmodId : spec.getDisplayName()));
        }
        return names;
    }

    private float headingHeight() {
        return rewardsHeading.height() + HEADING_PAD * 2f;
    }

    private float headerHeight() {
        float textHeight = titleLabel.height() + LINE_GAP + detailsLabel.height();
        return Math.max(Math.max(portraitSize(), flagPath == null ? 0f : flagHeight), textHeight);
    }

    private float rewardsHeight() {
        if (rewardRows.isEmpty()) {
            return 0f;
        }
        float height = SECTION_GAP + headingHeight() + HEADING_CONTENT_GAP;
        for (int index = 0; index < rewardRows.size(); index++) {
            height += rewardRows.get(index).height() + (index > 0 ? ROW_GAP : 0f);
        }
        return height;
    }

    private float scarsHeight() {
        return hasScars ? SECTION_GAP + headingHeight() + HEADING_CONTENT_GAP + scarsLabel.height() : 0f;
    }

    private float shipHeight() {
        return shipPortrait == null ? 0f : shipPortrait.halfLengthPixels(shipZoom) * 2f;
    }

    private float flagshipHeight() {
        if (!hasFlagship) {
            return 0f;
        }
        float height = SECTION_GAP + headingHeight() + HEADING_CONTENT_GAP + flagshipLabel.height();
        if (shipPortrait != null) {
            height += SHIP_GAP + shipHeight();
        }
        if (!codexHintLabel.isEmpty()) {
            height += SHIP_GAP + codexHintLabel.height();
        }
        return height;
    }

    void draw(Color accent, float mouseX, float mouseY, float alphaMult) {
        float contentWidth = contentWidth();
        float boxWidth = (float) Math.ceil(contentWidth + PADDING * 2f);
        float boxHeight = boxHeight();
        float boxX = Math.round(boxLeft(mouseX, boxWidth, Global.getSettings().getScreenWidth()));
        float boxY = Math.round(boxBottom(mouseY, boxHeight, Global.getSettings().getScreenHeight()));
        GLDraw.fillQuad(boxX, boxY, boxWidth, boxHeight, SkillTreePanelStyle.TOOLTIP_BACKGROUND_COLOR, alphaMult);
        GLDraw.strokeQuad(boxX, boxY, boxWidth, boxHeight, accent, SkillTreePanelStyle.TOOLTIP_BORDER_THICKNESS, alphaMult);
        float left = boxX + PADDING;
        float top = boxY + boxHeight - PADDING;
        drawHeader(left, top, alphaMult);
        top -= headerHeight();
        if (!rewardRows.isEmpty()) {
            top = drawHeading(rewardsHeading, left, top, alphaMult);
            for (int index = 0; index < rewardRows.size(); index++) {
                if (index > 0) {
                    top -= ROW_GAP;
                }
                top = drawReward(rewardRows.get(index), left, top, alphaMult);
            }
        }
        if (hasScars) {
            top = drawHeading(scarsHeading, left, top, alphaMult);
            scarsLabel.setAlpha(alphaMult).draw(left, top);
            top -= scarsLabel.height();
        }
        if (hasFlagship) {
            drawFlagship(left, drawHeading(flagshipHeading, left, top, alphaMult), alphaMult);
        }
    }

    private void drawHeader(float left, float top, float alphaMult) {
        float size = portraitSize();
        GLDraw.fillQuad(left, top - size, size, size, SLOT_FILL, alphaMult);
        if (portraitPath != null) {
            SpriteDraw.drawAtCenter(sprites, portraitPath, left + size / 2f, top - size / 2f, size, size, Color.WHITE, alphaMult);
        }
        GLDraw.strokeQuad(left, top - size, size, size, factionColour, FRAME_WIDTH, alphaMult);
        if (flagPath != null) {
            float flagCentreX = left + contentWidth() - flagWidth / 2f;
            SpriteDraw.drawAtCenter(sprites, flagPath, flagCentreX, top - flagHeight / 2f, flagWidth, flagHeight, Color.WHITE, alphaMult);
        }
        float textLeft = left + size + HEADER_GAP;
        titleLabel.setColor(Misc.getTooltipTitleAndLightHighlightColor()).setAlpha(alphaMult).drawWithShadow(textLeft, top, Color.BLACK);
        detailsLabel.setAlpha(alphaMult).draw(textLeft, top - titleLabel.height() - LINE_GAP);
    }

    private float drawHeading(TextLabel heading, float left, float top, float alphaMult) {
        float barTop = top - SECTION_GAP;
        float barHeight = headingHeight();
        GLDraw.fillQuad(left, barTop - barHeight, contentWidth(), barHeight, headingBarColour, alphaMult);
        heading.setColor(factionColour).setAlpha(alphaMult).draw(left + (contentWidth() - heading.width()) / 2f, barTop - HEADING_PAD);
        return barTop - barHeight - HEADING_CONTENT_GAP;
    }

    private float drawReward(RewardRow row, float left, float top, float alphaMult) {
        RewardFace face = row.face();
        GLDraw.fillQuad(left, top - ICON_SIZE, ICON_SIZE, ICON_SIZE, SLOT_FILL, alphaMult);
        if (face.iconPath() != null) {
            SpriteDraw.drawAtCenter(sprites, face.iconPath(), left + ICON_SIZE / 2f, top - ICON_SIZE / 2f, ICON_SIZE, ICON_SIZE, Color.WHITE, alphaMult);
        }
        GLDraw.strokeQuad(left, top - ICON_SIZE, ICON_SIZE, ICON_SIZE, face.frameColour(), FRAME_WIDTH, alphaMult);
        float textLeft = left + ICON_SIZE + ICON_GAP;
        row.nameLabel().setAlpha(alphaMult).draw(textLeft, top);
        if (!row.descriptionLabel().isEmpty()) {
            row.descriptionLabel().setAlpha(alphaMult).draw(textLeft, top - row.nameLabel().height() - LINE_GAP);
        }
        return top - row.height();
    }

    private void drawFlagship(float left, float top, float alphaMult) {
        flagshipLabel.setAlpha(alphaMult).draw(left, top);
        top -= flagshipLabel.height();
        if (shipPortrait != null) {
            top -= SHIP_GAP;
            float shipHeight = shipHeight();
            shipPortrait.draw(left + contentWidth() / 2f, top - shipHeight / 2f, shipZoom, alphaMult);
            top -= shipHeight;
        }
        if (!codexHintLabel.isEmpty()) {
            top -= SHIP_GAP;
            codexHintLabel.setAlpha(alphaMult).draw(left + (contentWidth() - codexHintLabel.width()) / 2f, top);
        }
    }

    static float boxLeft(float mouseX, float boxWidth, float screenWidth) {
        float rightOfCursor = mouseX + CURSOR_OFFSET;
        if (rightOfCursor + boxWidth <= screenWidth - SCREEN_MARGIN) {
            return rightOfCursor;
        }
        return Math.max(SCREEN_MARGIN, mouseX - CURSOR_OFFSET - boxWidth);
    }

    static float boxBottom(float mouseY, float boxHeight, float screenHeight) {
        float belowCursor = Math.max(SCREEN_MARGIN, mouseY - boxHeight - CURSOR_OFFSET);
        return Math.max(SCREEN_MARGIN, Math.min(belowCursor, screenHeight - SCREEN_MARGIN - boxHeight));
    }
}
