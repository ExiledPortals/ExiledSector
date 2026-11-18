package exiledsector.ui.infestation;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.ui.TreeLayers;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import exiledsector.ui.util.TexturedQuadBatch;
import exiledsector.ui.util.UnitCircle;

final class ShroudInfestation implements InfestationPainter {

    private static final UnitCircle CORE_CIRCLE = UnitCircle.of(96);
    private static final float SCENE_REACH_SHARE = 1.08f;
    private static final float CORE_DARKNESS_SHARE = 0.7f;
    private static final float CORE_DARKNESS_ALPHA = 0.35f;
    private static final float RIM_GLOW_INNER_SHARE = 0.96f;

    private final InfestedGeometry geometry;
    private final InfestationSprites sprites;
    private final ShroudMist mist;
    private final ShroudBlobShells shells;
    private final ShroudEyes eyes;
    private final ShroudTendrils tendrils;
    private final ShroudBlobRim rim;
    private final ScreenPuffSink sink = new ScreenPuffSink();
    private final ScreenEyeSink eyeSink = new ScreenEyeSink();
    private final TexturedQuadBatch quadBatch = new TexturedQuadBatch();

    ShroudInfestation(InfestedGeometry geometry, InfestationSprites sprites) {
        this.geometry = geometry;
        this.sprites = sprites;
        this.mist = new ShroudMist(geometry);
        this.shells = new ShroudBlobShells(geometry);
        this.eyes = new ShroudEyes(geometry);
        this.tendrils = new ShroudTendrils(geometry);
        this.rim = new ShroudBlobRim(geometry);
    }

    @Override
    public void renderUnderNodes(TreeLayers layers, float elapsedSeconds) {
        TreeViewport viewport = layers.viewportFor(geometry.socket());
        float sceneAlpha = layers.alphaFor(geometry.socket());
        if (!isShown(viewport, sceneAlpha)) {
            return;
        }
        float centreX = viewport.screenX(geometry.socketX());
        float centreY = viewport.screenY(geometry.socketY());
        float screenRadius = geometry.worldRadius() * viewport.zoom();
        try (GlScope scope = GlScope.flat()) {
            HoloShapes.centreGlow(centreX, centreY, screenRadius * CORE_DARKNESS_SHARE, CORE_CIRCLE, ShroudPuffs.DARKNESS,
                    CORE_DARKNESS_ALPHA * sceneAlpha);
        }
        SpriteAPI sheetTexture = sprites.shroudSheet();
        if (sheetTexture == null) {
            return;
        }
        try (TexturedQuadBatch sheet = quadBatch.beginNormal(sheetTexture)) {
            sink.draw(viewport, sheet, sceneAlpha);
            mist.emit(sink, elapsedSeconds);
            rim.emitPuffs(sink, elapsedSeconds);
            tendrils.emitPuffs(sink, elapsedSeconds);
            shells.emitPuffs(sink, elapsedSeconds, false);
        }
        try (GlScope scope = GlScope.flat().additive()) {
            HoloShapes.radialGlow(centreX, centreY, screenRadius, screenRadius * RIM_GLOW_INNER_SHARE, ShroudPuffs.SHROUD,
                    ShroudBlobRim.glowAlpha(elapsedSeconds) * sceneAlpha);
        }
        try (TexturedQuadBatch flashes = quadBatch.beginAdditive(sheetTexture)) {
            sink.draw(viewport, flashes, sceneAlpha);
            shells.emitFlashes(sink, elapsedSeconds, false);
            tendrils.emitFlashes(sink, elapsedSeconds);
            rim.emitFlashes(sink, elapsedSeconds);
        }
    }

    @Override
    public void renderOverNodes(TreeLayers layers, float elapsedSeconds) {
        TreeViewport viewport = layers.viewportFor(geometry.socket());
        float sceneAlpha = layers.alphaFor(geometry.socket());
        if (!isShown(viewport, sceneAlpha)) {
            return;
        }
        SpriteAPI sheetTexture = sprites.shroudSheet();
        if (sheetTexture == null) {
            return;
        }
        try (TexturedQuadBatch sheet = quadBatch.beginNormal(sheetTexture)) {
            sink.draw(viewport, sheet, sceneAlpha);
            shells.emitPuffs(sink, elapsedSeconds, true);
            eyes.emitSockets(sink, elapsedSeconds);
        }
        SpriteAPI spotsTexture = sprites.shroudSpots();
        if (spotsTexture != null) {
            try (TexturedQuadBatch spots = quadBatch.beginAdditive(spotsTexture)) {
                eyeSink.draw(viewport, spots, sceneAlpha);
                eyes.emitGlows(eyeSink, elapsedSeconds);
            }
        }
        try (TexturedQuadBatch flashes = quadBatch.beginAdditive(sheetTexture)) {
            sink.draw(viewport, flashes, sceneAlpha);
            shells.emitFlashes(sink, elapsedSeconds, true);
        }
    }

    private boolean isShown(TreeViewport viewport, float sceneAlpha) {
        if (sceneAlpha <= 0f || geometry.worldRadius() <= 0f) {
            return false;
        }
        return viewport.isVisible(viewport.screenX(geometry.socketX()), viewport.screenY(geometry.socketY()),
                geometry.worldRadius() * SCENE_REACH_SHARE * viewport.zoom());
    }

    private static final class ScreenEyeSink implements ShroudEyes.EyeSink {

        private TreeViewport viewport;
        private TexturedQuadBatch quads;
        private float sceneAlpha;

        void draw(TreeViewport frameViewport, TexturedQuadBatch spotQuads, float frameAlpha) {
            viewport = frameViewport;
            quads = spotQuads;
            sceneAlpha = frameAlpha;
        }

        @Override
        public void eye(float worldX, float worldY, float worldWidth, float worldHeight, float angleDeg, float alpha) {
            float drawnAlpha = sceneAlpha * alpha;
            if (drawnAlpha <= 0f || worldHeight <= 0f) {
                return;
            }
            float screenX = viewport.screenX(worldX);
            float screenY = viewport.screenY(worldY);
            float screenWidth = worldWidth * viewport.zoom();
            if (!viewport.isVisible(screenX, screenY, screenWidth)) {
                return;
            }
            quads.quadAtCenter(screenX, screenY, screenWidth, worldHeight * viewport.zoom(), angleDeg, ShroudPuffs.EYE_GLOW,
                    Math.min(1f, drawnAlpha));
        }
    }
}
