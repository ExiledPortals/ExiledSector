package exiledsector.ui.slipstream;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.decoration.GhostFlight;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.LineBatch;
import exiledsector.ui.util.TexturedQuadBatch;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.List;

final class SlipstreamPainter {

    static final String DARK_LAYER_PATH = "graphics/fx/slipstream_dark.png";
    static final String MIDDLE_LAYER_PATH = "graphics/fx/slipstream_layer1.png";
    static final String TOP_LAYER_PATH = "graphics/fx/slipstream_layer2.png";
    static final String EDGE_PATH = "graphics/fx/slipstream_edge3.png";
    static final float EDGE_WOBBLE_SHARE = 0.5f;
    static final float PARTICLE_LINE_PIXELS = 1.5f;
    private static final int LEFT_OUTER = 0;
    private static final int RIGHT_OUTER = 1;
    private static final int LEFT_EDGE_OUTER = 2;
    private static final int LEFT_EDGE_INNER = 3;
    private static final int RIGHT_EDGE_OUTER = 4;
    private static final int RIGHT_EDGE_INNER = 5;
    private static final int CORNERS_PER_SEGMENT = 6;

    private final SpriteCache sprites = new SpriteCache(SlipstreamPainter.class);
    private final LineBatch particleLines = new LineBatch(PARTICLE_LINE_PIXELS);
    private final TexturedQuadBatch ghostQuads = new TexturedQuadBatch();
    private final float[] placed = new float[SlipstreamProjection.PLACED_LENGTH];
    private float[] corners = new float[0];

    void paintStream(Slipstream stream, SlipstreamProjection projection, float alphaMult) {
        List<StreamSegment> segments = stream.segments();
        if (segments.size() < 3 || alphaMult <= 0f) {
            return;
        }
        SpriteAPI dark = sprites.texture(DARK_LAYER_PATH);
        SpriteAPI middle = sprites.texture(MIDDLE_LAYER_PATH);
        SpriteAPI top = sprites.texture(TOP_LAYER_PATH);
        SpriteAPI edge = sprites.texture(EDGE_PATH);
        if (dark == null || middle == null || top == null || edge == null) {
            return;
        }
        placeCorners(stream, projection);
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)) {
            float repeat = stream.textureRepeatLength();
            strip(dark, stream, alphaMult, LEFT_OUTER, RIGHT_OUTER, repeat, 0f, false);
            strip(middle, stream, alphaMult, LEFT_OUTER, RIGHT_OUTER, repeat, stream.middleLayerScroll(), false);
            strip(top, stream, alphaMult, LEFT_OUTER, RIGHT_OUTER, repeat, stream.topLayerScroll(), false);
            float edgeRepeat = stream.edgeWidth() * 2f;
            strip(edge, stream, alphaMult, LEFT_EDGE_OUTER, LEFT_EDGE_INNER, edgeRepeat, 0f, true);
            strip(edge, stream, alphaMult, RIGHT_EDGE_OUTER, RIGHT_EDGE_INNER, edgeRepeat, 0f, true);
        }
    }

    void paintParticles(Slipstream stream, StreamParticles particles, SlipstreamProjection projection, float alphaMult) {
        if (particles.count() == 0 || alphaMult <= 0f) {
            return;
        }
        particleLines.clear();
        particles.draw(particleLines, stream, projection, alphaMult);
        try (GlScope scope = GlScope.save(GlScope.DRAW_ATTRIBS).untextured().additive().enable(GL11.GL_LINE_SMOOTH)) {
            particleLines.flush();
        }
    }

    void paintGhosts(SensorGhostPack ghosts, SlipstreamProjection projection, float alphaMult) {
        SpriteAPI blip = sprites.sprite(GhostFlight.TEXTURE_PATH);
        if (blip == null || alphaMult <= 0f) {
            return;
        }
        try (TexturedQuadBatch quads = ghostQuads.beginNormal(blip)) {
            ghosts.draw(quads, projection, GhostFlight.color(), alphaMult);
        }
    }

    private void placeCorners(Slipstream stream, SlipstreamProjection projection) {
        List<StreamSegment> segments = stream.segments();
        int needed = segments.size() * CORNERS_PER_SEGMENT * 2;
        if (corners.length < needed) {
            corners = Arrays.copyOf(corners, needed * 2);
        }
        float edgeWidth = stream.edgeWidth();
        for (int i = 0; i < segments.size(); i++) {
            StreamSegment segment = segments.get(i);
            float half = segment.width / 2f;
            Wobble left = segment.leftWobble;
            Wobble right = segment.rightWobble;
            corner(projection, i, LEFT_OUTER, segment, half, left.offsetX(), left.offsetY());
            corner(projection, i, RIGHT_OUTER, segment, -half, right.offsetX(), right.offsetY());
            float leftWobbleX = left.offsetX() * EDGE_WOBBLE_SHARE;
            float leftWobbleY = left.offsetY() * EDGE_WOBBLE_SHARE;
            corner(projection, i, LEFT_EDGE_OUTER, segment, half, leftWobbleX, leftWobbleY);
            corner(projection, i, LEFT_EDGE_INNER, segment, half - edgeWidth, leftWobbleX, leftWobbleY);
            float rightWobbleX = right.offsetX() * EDGE_WOBBLE_SHARE;
            float rightWobbleY = right.offsetY() * EDGE_WOBBLE_SHARE;
            corner(projection, i, RIGHT_EDGE_OUTER, segment, -half, rightWobbleX, rightWobbleY);
            corner(projection, i, RIGHT_EDGE_INNER, segment, edgeWidth - half, rightWobbleX, rightWobbleY);
        }
    }

    private void corner(SlipstreamProjection projection, int segmentIndex, int corner, StreamSegment segment, float across, float wobbleX,
                        float wobbleY) {
        projection.place(segment.centerX + segment.normalX * across + wobbleX, segment.centerY + segment.normalY * across + wobbleY, placed);
        int slot = (segmentIndex * CORNERS_PER_SEGMENT + corner) * 2;
        corners[slot] = placed[SlipstreamProjection.X];
        corners[slot + 1] = placed[SlipstreamProjection.Y];
    }

    private void strip(SpriteAPI texture, Slipstream stream, float alphaMult, int firstCorner, int secondCorner, float repeat, float scroll,
                       boolean outerOnTop) {
        texture.bindTexture();
        List<StreamSegment> segments = stream.segments();
        float firstV = outerOnTop ? 1f : 0f;
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i < segments.size(); i++) {
            float textureX = segments.get(i).distance / repeat + scroll;
            GL11.glColor4f(1f, 1f, 1f, Math.max(0f, Math.min(1f, stream.alphaOf(i) * alphaMult)));
            int first = (i * CORNERS_PER_SEGMENT + firstCorner) * 2;
            int second = (i * CORNERS_PER_SEGMENT + secondCorner) * 2;
            GL11.glTexCoord2f(textureX, firstV);
            GL11.glVertex2f(corners[first], corners[first + 1]);
            GL11.glTexCoord2f(textureX, 1f - firstV);
            GL11.glVertex2f(corners[second], corners[second + 1]);
        }
        GL11.glEnd();
    }
}
