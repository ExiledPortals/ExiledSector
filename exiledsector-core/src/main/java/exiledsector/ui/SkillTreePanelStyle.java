package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.skilleffect.render.FallbackSupport;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.TextLabel;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.ui.LazyFont;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillTreePanelStyle {

    public static final Color TOOLTIP_TITLE_COLOR = Color.WHITE;
    public static final Color TOOLTIP_BODY_COLOR = new Color(230, 230, 230);
    public static final float FONT_LINE_HEIGHT_FACTOR = 1f;
    public static final Color TOOLTIP_BACKGROUND_COLOR = Color.BLACK;
    public static final Color OP_TEXT_COLOR = new Color(0xFD, 0xD0, 0x00);
    public static final float TOOLTIP_BORDER_THICKNESS = 2f;
    public static final Color GLOW_COLOR = new Color(120, 200, 255);
    public static final Color POSITIVE_STAT_COLOR = new Color(0x98, 0xFB, 0x00);
    public static final Color IMMUTABLE_TEXT_COLOR = new Color(230, 80, 175);
    public static final Color OVERCHARGED_TEXT_COLOR = new Color(80, 230, 255);
    public static final Color NEGATIVE_STAT_COLOR = new Color(0xFC, 0x63, 0x00);

    private static final Color FALLBACK_LOW_TECH_COLOR = new Color(255, 160, 60);
    private static final Color FALLBACK_HIGH_TECH_COLOR = new Color(90, 190, 255);
    private static final String LOW_TECH_DESIGN_TYPE = "Low Tech";
    private static final String HIGH_TECH_DESIGN_TYPE = "High Tech";

    private static final Color DEFAULT_ACCENT_COLOR = GLOW_COLOR;
    private static final int COLOR_QUANTIZE_STEP = 24;
    private static final int MIN_ALPHA_TO_SAMPLE = 128;

    private static final float TOOLTIP_PADDING = 10f;
    private static final float TOOLTIP_WIDTH_SAFETY_MARGIN = 8f;
    private static final float TOOLTIP_TITLE_BODY_GAP = 6f;
    private static final float TOOLTIP_CURSOR_OFFSET = 18f;
    private static final float TOOLTIP_TABLE_GAP = 14f;
    private static final float TOOLTIP_SEPARATOR_THICKNESS = 1f;
    private static final float TOOLTIP_SCREEN_MARGIN = 4f;
    private static final float TOOLTIP_FLAVOUR_GAP = 8f;
    private static final Color TOOLTIP_TITLE_SHADOW_COLOR = Color.BLACK;

    static final String DEFAULT_FONT_PATH = UiFonts.FONT_FOLDER + UiFonts.ORBITRON_20_BOLD_FILE;
    private static final Map<String, Optional<LazyFont>> FONTS_BY_DEFAULT_PATH = new HashMap<>();
    private static final Map<String, Optional<LazyFont>> FONTS_BY_FILE = new HashMap<>();

    private static final TooltipText VANILLA_TOOLTIP_TEXT = new TooltipText(UiFonts.TITLE, UiFonts.BODY, UiFonts.BODY, UiFonts.HINT,
            new TooltipText.Table(UiFonts.TITLE, UiFonts.BODY, 24f, 20f), 360f);
    private static final TooltipText LARGE_TOOLTIP_TEXT = new TooltipText(UiFonts.LARGE_TITLE, UiFonts.TITLE, UiFonts.LARGE_DETAIL, UiFonts.TITLE,
            new TooltipText.Table(UiFonts.LARGE_TITLE, UiFonts.LARGE_DETAIL, 30f, 22f), 480f);

    private static final Map<String, Color> ACCENT_COLORS_BY_ICON = new ConcurrentHashMap<>();

    private String accentIconPath;
    private Color accentColor;
    private Color lowTechColor;
    private Color highTechColor;

    public void setAccentIconPath(String accentIconPath) {
        if (!Objects.equals(this.accentIconPath, accentIconPath)) {
            this.accentIconPath = accentIconPath;
            this.accentColor = null;
        }
    }

    public static TooltipText tooltipText() {
        return TooltipTextSize.large() ? LARGE_TOOLTIP_TEXT : VANILLA_TOOLTIP_TEXT;
    }

    public static LazyFont font() {
        return localeFontOr(DEFAULT_FONT_PATH);
    }

    static LazyFont localeFontOr(String path) {
        return FONTS_BY_DEFAULT_PATH.computeIfAbsent(path,
                key -> loadedFont(Translation.has("meta.font") ? Translation.text("meta.font") : key)).orElse(null);
    }

    private static Optional<LazyFont> loadedFont(String file) {
        return FONTS_BY_FILE.computeIfAbsent(file, key -> Optional.ofNullable(loadFontOrDefault(key)));
    }

    static LazyFont loadFontOrDefault(String path) {
        LazyFont loaded = loadFontOrNull(path);
        if (loaded == null && !DEFAULT_FONT_PATH.equals(path)) {
            loaded = loadFontOrNull(DEFAULT_FONT_PATH);
        }
        return loaded;
    }

    private static LazyFont loadFontOrNull(String path) {
        return FallbackSupport.getOrFallback(() -> LazyFont.loadFont(path), null,
                Logger.getLogger(SkillTreePanelStyle.class), "Failed to load font " + path);
    }

    public void drawTooltipBackground(float x, float y, float width, float height, float alphaMult, Color borderColor) {
        GLDraw.fillQuad(x, y, width, height, TOOLTIP_BACKGROUND_COLOR, alphaMult);
        GLDraw.strokeQuad(x, y, width, height, borderColor, TOOLTIP_BORDER_THICKNESS, alphaMult);
    }

    public void drawTitleBodyTooltip(TextLabel title, TextLabel body, float mouseX, float mouseY, float alphaMult) {
        drawTitleBodyTooltip(title, null, body, List.of(), List.of(), null, null, mouseX, mouseY, alphaMult);
    }

    public void drawTitleBodyTooltip(TextLabel title, TextLabel flavour, TextLabel body, List<TextLabel> bodySections, List<SkillTreeTooltipTable> tables,
                                     TextLabel separatedSection, TextLabel footer, float mouseX, float mouseY, float alphaMult) {
        float contentWidth = Math.max(title.width(), body.width());
        float flavourHeight = 0f;
        if (flavour != null) {
            contentWidth = Math.max(contentWidth, flavour.width());
            flavourHeight = flavour.height() + TOOLTIP_FLAVOUR_GAP;
        }
        float bodySectionsHeight = 0f;
        for (TextLabel bodySection : bodySections) {
            contentWidth = Math.max(contentWidth, bodySection.width());
            bodySectionsHeight += separatedHeight(bodySection);
        }
        float tablesHeight = 0f;
        for (SkillTreeTooltipTable table : tables) {
            contentWidth = Math.max(contentWidth, table.width());
            tablesHeight += TOOLTIP_TABLE_GAP + table.height();
        }
        float separatedHeight = 0f;
        if (separatedSection != null) {
            contentWidth = Math.max(contentWidth, separatedSection.width());
            separatedHeight = separatedHeight(separatedSection);
        }
        float footerHeight = 0f;
        if (footer != null) {
            contentWidth = Math.max(contentWidth, footer.width());
            footerHeight = TOOLTIP_TABLE_GAP + footer.height();
        }
        float boxWidth = (float) Math.ceil(contentWidth + TOOLTIP_PADDING * 2f + TOOLTIP_WIDTH_SAFETY_MARGIN);
        float boxHeight = (float) Math.ceil(title.height() + TOOLTIP_TITLE_BODY_GAP + flavourHeight + body.height() + bodySectionsHeight + tablesHeight
                + separatedHeight + footerHeight + TOOLTIP_PADDING * 2f);
        float boxX = Math.round(tooltipLeft(mouseX, boxWidth, Global.getSettings().getScreenWidth()));
        float boxY = Math.round(Math.max(TOOLTIP_SCREEN_MARGIN, mouseY - boxHeight - TOOLTIP_CURSOR_OFFSET));

        Color accent = getAccentColor();
        drawTooltipBackground(boxX, boxY, boxWidth, boxHeight, alphaMult, accent);

        float titleY = boxY + boxHeight - TOOLTIP_PADDING;
        float flavourY = titleY - title.height() - TOOLTIP_TITLE_BODY_GAP;
        float bodyY = flavourY - flavourHeight;
        float titleX = boxX + (boxWidth - title.width()) / 2f;
        title.setColor(Misc.getTooltipTitleAndLightHighlightColor()).setAlpha(alphaMult).drawWithShadow(titleX, titleY, TOOLTIP_TITLE_SHADOW_COLOR);
        if (flavour != null) {
            flavour.setAlpha(alphaMult).draw(boxX + TOOLTIP_PADDING, flavourY);
        }
        body.setAlpha(alphaMult).draw(boxX + TOOLTIP_PADDING, bodyY);

        float tableTop = bodyY - body.height();
        float tableWidth = boxWidth - TOOLTIP_PADDING * 2f;
        for (TextLabel bodySection : bodySections) {
            tableTop = drawSeparated(bodySection, boxX, tableTop, tableWidth, accent, alphaMult);
        }
        for (SkillTreeTooltipTable table : tables) {
            tableTop -= TOOLTIP_TABLE_GAP;
            table.draw(boxX + TOOLTIP_PADDING, tableTop, tableWidth, accent, alphaMult);
            tableTop -= table.height();
        }
        if (separatedSection != null) {
            tableTop = drawSeparated(separatedSection, boxX, tableTop, tableWidth, accent, alphaMult);
        }
        if (footer != null) {
            footer.setAlpha(alphaMult).draw(boxX + TOOLTIP_PADDING, tableTop - TOOLTIP_TABLE_GAP);
        }
    }

    private static float separatedHeight(TextLabel section) {
        return TOOLTIP_TABLE_GAP * 2f + TOOLTIP_SEPARATOR_THICKNESS + section.height();
    }

    private static float drawSeparated(TextLabel section, float boxX, float top, float width, Color accent, float alphaMult) {
        float sectionTop = top - TOOLTIP_TABLE_GAP - TOOLTIP_SEPARATOR_THICKNESS;
        GLDraw.fillQuad(boxX + TOOLTIP_PADDING, sectionTop, width, TOOLTIP_SEPARATOR_THICKNESS, accent, alphaMult);
        sectionTop -= TOOLTIP_TABLE_GAP;
        section.setAlpha(alphaMult).draw(boxX + TOOLTIP_PADDING, sectionTop);
        return sectionTop - section.height();
    }

    static float tooltipLeft(float mouseX, float boxWidth, float screenWidth) {
        float rightOfCursor = mouseX + TOOLTIP_CURSOR_OFFSET;
        if (rightOfCursor + boxWidth <= screenWidth - TOOLTIP_SCREEN_MARGIN) {
            return rightOfCursor;
        }
        return Math.max(TOOLTIP_SCREEN_MARGIN, mouseX - TOOLTIP_CURSOR_OFFSET - boxWidth);
    }

    public Color getAccentColor() {
        if (accentColor == null) {
            accentColor = computeDominantColor(accentIconPath);
        }
        return accentColor;
    }

    private static Color computeDominantColor(String path) {
        if (path == null || path.isEmpty()) return DEFAULT_ACCENT_COLOR;
        return ACCENT_COLORS_BY_ICON.computeIfAbsent(path, SkillTreePanelStyle::readDominantColor);
    }

    private static Color readDominantColor(String path) {
        return FallbackSupport.getOrFallback(() -> computeDominantColorOrThrow(path), DEFAULT_ACCENT_COLOR,
                Logger.getLogger(SkillTreePanelStyle.class), "Failed to read " + path + " for accent colour");
    }

    private static Color computeDominantColorOrThrow(String path) throws IOException {
        try (InputStream imageStream = Global.getSettings().openStream(path)) {
            BufferedImage image = ImageIO.read(imageStream);
            if (image == null) return DEFAULT_ACCENT_COLOR;

            Map<Integer, Integer> bucketCounts = new HashMap<>();
            Map<Integer, int[]> bucketSums = new HashMap<>();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xFF;
                    if (alpha < MIN_ALPHA_TO_SAMPLE) continue;

                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int bucket = (quantize(r) << 16) | (quantize(g) << 8) | quantize(b);

                    bucketCounts.merge(bucket, 1, Integer::sum);
                    int[] sum = bucketSums.computeIfAbsent(bucket, key -> new int[3]);
                    sum[0] += r;
                    sum[1] += g;
                    sum[2] += b;
                }
            }

            if (bucketCounts.isEmpty()) return DEFAULT_ACCENT_COLOR;

            Map.Entry<Integer, Integer> mostCommon = null;
            for (Map.Entry<Integer, Integer> entry : bucketCounts.entrySet()) {
                if (mostCommon == null || entry.getValue() > mostCommon.getValue()) {
                    mostCommon = entry;
                }
            }
            if (mostCommon == null) return DEFAULT_ACCENT_COLOR;

            int[] sum = bucketSums.get(mostCommon.getKey());
            int pixelCount = mostCommon.getValue();
            return new Color(sum[0] / pixelCount, sum[1] / pixelCount, sum[2] / pixelCount);
        }
    }

    private static int quantize(int channel) {
        return (channel / COLOR_QUANTIZE_STEP) * COLOR_QUANTIZE_STEP;
    }

    public static Color standardHighlightColor(Style style) {
        return switch (style) {
            case GOOD -> POSITIVE_STAT_COLOR;
            case BAD -> NEGATIVE_STAT_COLOR;
            case IMMUTABLE -> IMMUTABLE_TEXT_COLOR;
            case OVERCHARGED -> OVERCHARGED_TEXT_COLOR;
            case MUTED -> Misc.getGrayColor();
            case BUILT_IN -> Misc.getStoryOptionColor();
            default -> Misc.getHighlightColor();
        };
    }

    public Color highlightColor(Style style) {
        return switch (style) {
            case HULLMOD -> lowTechColor();
            case NODE -> highTechColor();
            default -> standardHighlightColor(style);
        };
    }

    private Color lowTechColor() {
        if (lowTechColor == null) {
            lowTechColor = designTypeColor(LOW_TECH_DESIGN_TYPE, FALLBACK_LOW_TECH_COLOR);
        }
        return lowTechColor;
    }

    private Color highTechColor() {
        if (highTechColor == null) {
            highTechColor = designTypeColor(HIGH_TECH_DESIGN_TYPE, FALLBACK_HIGH_TECH_COLOR);
        }
        return highTechColor;
    }

    private static Color designTypeColor(String designType, Color fallback) {
        return FallbackSupport.getOrFallback(() -> Global.getSettings().getDesignTypeColor(designType), fallback,
                Logger.getLogger(SkillTreePanelStyle.class), "Failed to read the " + designType + " design type colour");
    }

    public TextLabel writeDescription(TextLabel label, List<DescriptionLine> paragraphs, float maxWidth, float maxHeight) {
        List<StyledText> displayed = new ArrayList<>(paragraphs.size());
        for (DescriptionLine paragraph : paragraphs) {
            displayed.add(paragraph.display());
        }
        return label.setParagraphs(displayed, maxWidth, maxHeight, this::highlightColor);
    }
}
