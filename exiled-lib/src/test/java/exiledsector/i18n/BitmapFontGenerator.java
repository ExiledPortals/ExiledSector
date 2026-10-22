package exiledsector.i18n;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

public final class BitmapFontGenerator {

    static final String USAGE = "Usage: BitmapFontGenerator <font file> <output .fnt path> <glyph px> <line height px> "
            + "<atlas width> <atlas height> [text files whose characters must be included...]";

    private static final int SPACING = 2;
    private static final float IDEOGRAPH_ASCENT = 0.88f;

    record Glyph(int id, int x, int y, int width, int height, int xOffset, int yOffset, int xAdvance) {
    }

    record Atlas(String face, int glyphPx, int lineHeight, int base, BufferedImage image, List<Glyph> glyphs) {
    }

    private record Measured(int codePoint, GlyphVector vector, Rectangle bounds, int advance) {
    }

    private BitmapFontGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.out.println(USAGE);
            return;
        }
        Path fontFile = Path.of(args[0]);
        Path fntFile = Path.of(args[1]);
        int glyphPx = Integer.parseInt(args[2]);
        int lineHeight = Integer.parseInt(args[3]);
        int width = Integer.parseInt(args[4]);
        int height = Integer.parseInt(args[5]);
        SortedSet<Integer> codePoints = defaultCharacters();
        for (int i = 6; i < args.length; i++) {
            Files.readString(Path.of(args[i]), StandardCharsets.UTF_8).codePoints()
                    .filter(cp -> !Character.isISOControl(cp)).forEach(codePoints::add);
        }
        Font font = loadFont(fontFile).deriveFont(Font.PLAIN, glyphPx);
        Atlas atlas = generate(font, glyphPx, lineHeight, width, height, codePoints);
        String pngName = fntFile.getFileName().toString().replaceFirst("\\.fnt$", "") + "_0.png";
        Files.createDirectories(fntFile.toAbsolutePath().getParent());
        ImageIO.write(atlas.image(), "png", fntFile.resolveSibling(pngName).toFile());
        Files.writeString(fntFile, toFnt(atlas, pngName), StandardCharsets.UTF_8);
        System.out.println("Wrote " + atlas.glyphs().size() + " glyphs to " + fntFile + " and " + pngName);
    }

    private static Font loadFont(Path fontFile) throws IOException, FontFormatException {
        if (fontFile.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ttc")) {
            return Font.createFonts(fontFile.toFile())[0];
        }
        return Font.createFont(Font.TRUETYPE_FONT, fontFile.toFile());
    }

    static SortedSet<Integer> defaultCharacters() {
        SortedSet<Integer> codePoints = new TreeSet<>();
        addRange(codePoints, 0x20, 0x7E);
        addRange(codePoints, 0xA0, 0xFF);
        addRange(codePoints, 0x2010, 0x2027);
        addRange(codePoints, 0x2030, 0x203A);
        addRange(codePoints, 0x2264, 0x2265);
        addRange(codePoints, 0x3000, 0x303F);
        addRange(codePoints, 0xFF01, 0xFF5E);
        addRange(codePoints, 0xFFE0, 0xFFE6);
        codePoints.addAll(gb2312LevelOneHanzi());
        return codePoints;
    }

    static SortedSet<Integer> gb2312LevelOneHanzi() {
        SortedSet<Integer> hanzi = new TreeSet<>();
        CharsetDecoder decoder = Charset.forName("GB2312").newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        for (int row = 0xB0; row <= 0xD7; row++) {
            for (int cell = 0xA1; cell <= 0xFE; cell++) {
                try {
                    String decoded = decoder.decode(ByteBuffer.wrap(new byte[]{(byte) row, (byte) cell})).toString();
                    decoded.codePoints().filter(Character::isIdeographic).forEach(hanzi::add);
                } catch (CharacterCodingException e) {
                    continue;
                }
            }
        }
        return hanzi;
    }

    private static void addRange(SortedSet<Integer> codePoints, int first, int last) {
        for (int cp = first; cp <= last; cp++) {
            codePoints.add(cp);
        }
    }

    static Atlas generate(Font font, int glyphPx, int lineHeight, int width, int height, SortedSet<Integer> codePoints) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        graphics.setColor(Color.WHITE);
        FontRenderContext context = graphics.getFontRenderContext();
        int base = Math.round((lineHeight - glyphPx) / 2f + glyphPx * IDEOGRAPH_ASCENT);

        List<Measured> measured = new ArrayList<>();
        for (int codePoint : codePoints) {
            if (!font.canDisplay(codePoint)) {
                continue;
            }
            GlyphVector vector = font.createGlyphVector(context, new String(Character.toChars(codePoint)));
            Rectangle bounds = vector.getPixelBounds(context, 0f, 0f);
            int advance = Math.round((float) vector.getGlyphMetrics(0).getAdvanceX());
            measured.add(new Measured(codePoint, vector, bounds, advance));
        }
        measured.sort(Comparator.comparingInt((Measured m) -> -Math.max(1, m.bounds().height)).thenComparingInt(Measured::codePoint));

        List<Glyph> glyphs = new ArrayList<>();
        int x = SPACING;
        int y = SPACING;
        int shelfHeight = 0;
        for (Measured m : measured) {
            int glyphWidth = Math.max(1, m.bounds().width);
            int glyphHeight = Math.max(1, m.bounds().height);
            if (x + glyphWidth + SPACING > width) {
                x = SPACING;
                y += shelfHeight + SPACING;
                shelfHeight = 0;
            }
            if (y + glyphHeight + SPACING > height) {
                throw new IllegalStateException("A " + width + "x" + height + " atlas cannot fit " + measured.size() + " glyphs");
            }
            if (!m.bounds().isEmpty()) {
                graphics.drawGlyphVector(m.vector(), x - m.bounds().x, y - m.bounds().y);
            }
            int xOffset = m.bounds().isEmpty() ? 0 : m.bounds().x;
            int yOffset = m.bounds().isEmpty() ? base : base + m.bounds().y;
            glyphs.add(new Glyph(m.codePoint(), x, y, glyphWidth, glyphHeight, xOffset, yOffset, m.advance()));
            x += glyphWidth + SPACING;
            shelfHeight = Math.max(shelfHeight, glyphHeight);
        }
        graphics.dispose();
        glyphs.sort(Comparator.comparingInt(Glyph::id));
        return new Atlas(font.getFamily(), glyphPx, lineHeight, base, image, glyphs);
    }

    static String toFnt(Atlas atlas, String pngName) {
        StringBuilder out = new StringBuilder();
        out.append("info face=\"").append(atlas.face()).append("\" size=-").append(atlas.glyphPx())
                .append(" bold=0 italic=0 charset=\"\" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=")
                .append(SPACING).append(',').append(SPACING).append(" outline=0\n");
        out.append("common lineHeight=").append(atlas.lineHeight()).append(" base=").append(atlas.base())
                .append(" scaleW=").append(atlas.image().getWidth()).append(" scaleH=").append(atlas.image().getHeight())
                .append(" pages=1 packed=0 alphaChnl=0 redChnl=4 greenChnl=4 blueChnl=4\n");
        out.append("page id=0 file=\"").append(pngName).append("\"\n");
        out.append("chars count=").append(atlas.glyphs().size()).append('\n');
        for (Glyph glyph : atlas.glyphs()) {
            out.append("char id=").append(glyph.id()).append(" x=").append(glyph.x()).append(" y=").append(glyph.y())
                    .append(" width=").append(glyph.width()).append(" height=").append(glyph.height())
                    .append(" xoffset=").append(glyph.xOffset()).append(" yoffset=").append(glyph.yOffset())
                    .append(" xadvance=").append(glyph.xAdvance()).append(" page=0 chnl=15\n");
        }
        return out.toString();
    }
}
