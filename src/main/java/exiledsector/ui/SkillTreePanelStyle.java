package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.DescriptionLine;
import exiledsector.ui.util.FallbackSupport;
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
import java.util.concurrent.ConcurrentHashMap;

public final class SkillTreePanelStyle {

    public static final float TOOLTIP_TITLE_FONT_SIZE = 24f;
    public static final float TOOLTIP_BODY_FONT_SIZE = 20f;
    public static final Color TOOLTIP_TITLE_COLOR = Color.WHITE;
    public static final Color TOOLTIP_BODY_COLOR = new Color(230, 230, 230);
    public static final float TOOLTIP_FLAVOUR_FONT_SIZE = 17f;
    public static final float TOOLTIP_MAX_TEXT_WIDTH = 480f;
    public static final float NODE_TOOLTIP_MAX_TEXT_WIDTH = TOOLTIP_MAX_TEXT_WIDTH * 1.2f;
    public static final float FONT_LINE_HEIGHT_FACTOR = 1f;
    public static final Color TOOLTIP_BACKGROUND_COLOR = Color.BLACK;
    public static final float TOOLTIP_BORDER_THICKNESS = 2f;
    public static final Color GLOW_COLOR = new Color(120, 200, 255);
    public static final Color POSITIVE_STAT_COLOR = new Color(0x98, 0xFB, 0x00);
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
    private static final float TOOLTIP_TITLE_BOLD_OFFSET = 1f;
    private static final float TOOLTIP_TABLE_GAP = 14f;
    private static final float TOOLTIP_SCREEN_MARGIN = 4f;
    private static final float TOOLTIP_FLAVOUR_GAP = 8f;

    static final String DEFAULT_FONT_PATH = "graphics/fonts/orbitron20aabold.fnt";
    private static LazyFont cachedFont;
    private static boolean fontLoadFailed;
    private static Boolean cachedFakeBold;

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

    private static boolean fakeBold() {
        if (cachedFakeBold == null) {
            cachedFakeBold = !Translation.has("meta.fakeBold") || !"false".equals(Translation.text("meta.fakeBold"));
        }
        return cachedFakeBold;
    }

    public static LazyFont font() {
        if (cachedFont == null && !fontLoadFailed) {
            cachedFont = loadFontOrDefault(Translation.has("meta.font") ? Translation.text("meta.font") : DEFAULT_FONT_PATH);
            fontLoadFailed = cachedFont == null;
        }
        return cachedFont;
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
        drawTitleBodyTooltip(title, null, body, List.of(), null, mouseX, mouseY, alphaMult);
    }

    public void drawTitleBodyTooltip(TextLabel title, TextLabel flavour, TextLabel body, List<SkillTreeTooltipTable> tables,
                                     TextLabel footer, float mouseX, float mouseY, float alphaMult) {
        float contentWidth = Math.max(title.width(), body.width());
        float flavourHeight = 0f;
        if (flavour != null) {
            contentWidth = Math.max(contentWidth, flavour.width());
            flavourHeight = flavour.height() + TOOLTIP_FLAVOUR_GAP;
        }
        float tablesHeight = 0f;
        for (SkillTreeTooltipTable table : tables) {
            contentWidth = Math.max(contentWidth, table.width());
            tablesHeight += TOOLTIP_TABLE_GAP + table.height();
        }
        float footerHeight = 0f;
        if (footer != null) {
            contentWidth = Math.max(contentWidth, footer.width());
            footerHeight = TOOLTIP_TABLE_GAP + footer.height();
        }
        float boxWidth = contentWidth + TOOLTIP_PADDING * 2f + TOOLTIP_WIDTH_SAFETY_MARGIN;
        float boxHeight = title.height() + TOOLTIP_TITLE_BODY_GAP + flavourHeight + body.height() + tablesHeight + footerHeight
                + TOOLTIP_PADDING * 2f;
        float boxX = tooltipLeft(mouseX, boxWidth, Global.getSettings().getScreenWidth());
        float boxY = Math.max(TOOLTIP_SCREEN_MARGIN, mouseY - boxHeight - TOOLTIP_CURSOR_OFFSET);

        Color accent = getAccentColor();
        drawTooltipBackground(boxX, boxY, boxWidth, boxHeight, alphaMult, accent);

        float titleY = boxY + boxHeight - TOOLTIP_PADDING;
        float flavourY = titleY - title.height() - TOOLTIP_TITLE_BODY_GAP;
        float bodyY = flavourY - flavourHeight;
        float titleX = boxX + (boxWidth - title.width()) / 2f;
        title.setAlpha(alphaMult).draw(titleX, titleY);
        if (fakeBold()) {
            title.draw(titleX + TOOLTIP_TITLE_BOLD_OFFSET, titleY);
        }
        if (flavour != null) {
            flavour.setAlpha(alphaMult).draw(boxX + TOOLTIP_PADDING, flavourY);
        }
        body.setAlpha(alphaMult).draw(boxX + TOOLTIP_PADDING, bodyY);

        float tableTop = bodyY - body.height();
        float tableWidth = boxWidth - TOOLTIP_PADDING * 2f;
        for (SkillTreeTooltipTable table : tables) {
            tableTop -= TOOLTIP_TABLE_GAP;
            table.draw(boxX + TOOLTIP_PADDING, tableTop, tableWidth, accent, alphaMult);
            tableTop -= table.height();
        }
        if (footer != null) {
            footer.setAlpha(alphaMult).draw(boxX + TOOLTIP_PADDING, tableTop - TOOLTIP_TABLE_GAP);
        }
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
