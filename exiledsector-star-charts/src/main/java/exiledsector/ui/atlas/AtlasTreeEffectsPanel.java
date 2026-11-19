package exiledsector.ui.atlas;

import exiledsector.charts.anomaly.FlareLuck;
import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.atlas.AtlasNames;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.tree.AtlasTreeEffectGroups;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.modifier.Luck;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.TooltipText;
import exiledsector.ui.UiFonts;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloFrame;
import exiledsector.ui.util.Rects;
import exiledsector.ui.util.TextLabel;
import exiledsector.ui.util.UiClip;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

final class AtlasTreeEffectsPanel {

    static final float CONTENT_WIDTH = 400f;
    private static final float PADDING = 18f;
    private static final float TITLE_GAP = 10f;
    private static final float HEADER_GAP = 4f;
    private static final float EFFECT_GAP = 7f;
    private static final float GROUP_GAP = 14f;
    private static final float SCROLL_STEP = 60f;
    private static final float UNLIMITED_TEXT_HEIGHT = 100000f;

    private record Section(TextLabel header, List<TextLabel> effects) {

        float height() {
            float height = header.height() + HEADER_GAP;
            for (TextLabel effect : effects) {
                height += effect.height();
            }
            return height + EFFECT_GAP * Math.max(0, effects.size() - 1);
        }

        void draw(float left, float top, float alpha) {
            header.setAlpha(alpha).draw(left, top);
            float effectTop = top - header.height() - HEADER_GAP;
            for (TextLabel effect : effects) {
                effect.setAlpha(alpha).draw(left, effectTop);
                effectTop -= effect.height() + EFFECT_GAP;
            }
        }

        void dispose() {
            header.dispose();
            effects.forEach(TextLabel::dispose);
        }
    }

    private record Box(float left, float bottom, float width, float height) {

        float top() {
            return bottom + height;
        }

        boolean contains(float x, float y) {
            return Rects.contains(left, bottom, width, height, x, y);
        }
    }

    private final Supplier<List<AtlasTreeEffectGroups.Group>> groups;
    private final TooltipText fonts = SkillTreePanelStyle.tooltipText();
    private final HoloFrame frame = new HoloFrame(HoloFrame.Look.PANEL);
    private final TextLabel titleLabel = new TextLabel(UiFonts.TITLE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final List<Section> sections = new ArrayList<>();
    private boolean open;
    private boolean stale = true;
    private float sectionsHeight;
    private float scrollOffset;
    private Box drawnBox;

    AtlasTreeEffectsPanel(Supplier<List<AtlasTreeEffectGroups.Group>> groups) {
        this.groups = groups;
    }

    boolean isOpen() {
        return open;
    }

    void toggle() {
        open = !open;
        if (open) {
            frame.open();
            SkillTreeSounds.panelOpened();
        } else {
            frame.close();
        }
    }

    void invalidate() {
        stale = true;
    }

    void advance(float amount) {
        frame.advance(amount);
    }

    boolean contains(float x, float y) {
        return frame.isBlocking() && drawnBox != null && drawnBox.contains(x, y);
    }

    boolean scroll(float x, float y, int wheelValue) {
        if (!contains(x, y)) {
            return false;
        }
        float step = wheelValue > 0 ? -SCROLL_STEP : SCROLL_STEP;
        scrollOffset = Math.max(0f, Math.min(maxScroll(drawnBox), scrollOffset + step));
        return true;
    }

    void render(float right, float top, float lowest, float alpha) {
        drawnBox = null;
        if (!frame.isVisible() || alpha <= 0f) {
            return;
        }
        if (stale) {
            if (!fonts.fontsLoaded()) {
                return;
            }
            I18n.forGameText(this::rebuild);
            stale = false;
        }
        float width = CONTENT_WIDTH + PADDING * 2f;
        float height = Math.min(top - lowest, titleBlockHeight() + sectionsHeight + PADDING * 2f);
        if (height <= PADDING * 2f) {
            return;
        }
        Box box = new Box(right - width, top - height, width, height);
        drawnBox = box;
        scrollOffset = Math.min(scrollOffset, maxScroll(box));
        frame.render(box.left(), box.bottom(), box.width(), box.height(), SkillTreePanelStyle.GLOW_COLOR, alpha,
                (frameLeft, frameBottom, frameWidth, frameHeight, frameAlpha) -> renderContent(box, frameAlpha));
    }

    private void renderContent(Box box, float alpha) {
        float left = box.left() + PADDING;
        float titleTop = box.top() - PADDING;
        titleLabel.setAlpha(alpha).draw(left, titleTop);
        float clipTop = titleTop - titleBlockHeight() + TITLE_GAP / 2f;
        float revealedBottom = box.bottom() + box.height() * (1f - Math.min(1f, frame.contentAlpha()));
        float clipBottom = Math.max(box.bottom() + PADDING / 2f, revealedBottom);
        if (clipBottom >= clipTop) {
            return;
        }
        try (GlScope clip = UiClip.clipUi(box.left(), clipBottom, box.width(), clipTop - clipBottom)) {
            float sectionTop = titleTop - titleBlockHeight() + scrollOffset;
            for (Section section : sections) {
                float sectionBottom = sectionTop - section.height();
                if (sectionBottom < clipTop && sectionTop > clipBottom) {
                    section.draw(left, sectionTop, alpha);
                }
                sectionTop = sectionBottom - GROUP_GAP;
            }
        }
    }

    private float titleBlockHeight() {
        return titleLabel.height() + TITLE_GAP;
    }

    private float maxScroll(Box box) {
        return Math.max(0f, sectionsHeight - (box.height() - PADDING * 2f - titleBlockHeight()));
    }

    private void rebuild() {
        sections.forEach(Section::dispose);
        sections.clear();
        titleLabel.set(Translation.text("atlasTree.effects.title"));
        List<AtlasTreeEffectGroups.Group> granted = groups.get();
        if (granted.isEmpty()) {
            sections.add(section("", List.of(StyledText.styled(Translation.text("atlasTree.effects.none"), Style.MUTED))));
        }
        int luckyRolls = luckyRolls(granted);
        for (AtlasTreeEffectGroups.Group group : granted) {
            sections.add(section(groupName(group), lines(group, luckyRolls)));
        }
        float total = 0f;
        for (Section section : sections) {
            total += section.height();
        }
        sectionsHeight = total + GROUP_GAP * Math.max(0, sections.size() - 1);
    }

    private Section section(String headerText, List<StyledText> lines) {
        TextLabel header = new TextLabel(fonts.title(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR).set(headerText);
        List<TextLabel> effects = new ArrayList<>();
        for (StyledText line : lines) {
            effects.add(new TextLabel(fonts.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR).setParagraphs(List.of(line), CONTENT_WIDTH,
                    UNLIMITED_TEXT_HEIGHT, SkillTreePanelStyle::standardHighlightColor));
        }
        return new Section(header, effects);
    }

    static String groupName(AtlasTreeEffectGroups.Group group) {
        if (group.everyChart()) {
            return Translation.text("atlasTree.effects.everyChart");
        }
        String name = AtlasNames.anomaly(group.anomalyIds().get(0));
        for (String anomalyId : group.anomalyIds().subList(1, group.anomalyIds().size())) {
            name = Translation.msg("atlasTree.effects.hybrid").arg("first", name).arg("second", AtlasNames.anomaly(anomalyId)).text();
        }
        return name;
    }

    static int luckyRolls(List<AtlasTreeEffectGroups.Group> groups) {
        List<ChartGenContext.AppliedEffect> chartLines = new ArrayList<>();
        groups.forEach(group -> chartLines.addAll(group.chartLines()));
        return FlareLuck.treeRolls(chartLines);
    }

    static List<StyledText> lines(AtlasTreeEffectGroups.Group group) {
        return lines(group, Luck.BASE_ROLLS);
    }

    static List<StyledText> lines(AtlasTreeEffectGroups.Group group, int luckyRolls) {
        List<StyledText> lines = new ArrayList<>();
        for (ChartGenContext.AppliedEffect effect : group.chartLines()) {
            AtlasTreeNodeText.addChartLine(lines, effect.effect(), effect.value(), luckyRolls);
        }
        for (AtlasEffects.Line effect : group.atlasLines()) {
            addLine(lines, AtlasTreeNodeText.atlasLine(effect.effect(), effect.value()));
        }
        return lines;
    }

    private static void addLine(List<StyledText> lines, StyledText line) {
        if (line != null && !line.isEmpty()) {
            lines.add(line);
        }
    }
}
