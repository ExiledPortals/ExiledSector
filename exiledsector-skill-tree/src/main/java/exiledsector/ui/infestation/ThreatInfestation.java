package exiledsector.ui.infestation;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.ui.TreeLayers;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import exiledsector.ui.util.TexturedQuadBatch;
import exiledsector.ui.util.UnitCircle;

import java.awt.Color;

final class ThreatInfestation implements InfestationPainter {

    static final Color THREAT_GLOW = new Color(130, 155, 145);
    private static final UnitCircle RIM_CIRCLE = UnitCircle.of(160);
    private static final float SCENE_REACH_SHARE = 1.08f;
    private static final float RIM_LINE_ALPHA = 0.25f;
    private static final float RIM_LINE_THICKNESS = 1.5f;
    private static final float RIM_HAZE_SHARE = 0.06f;
    private static final float RIM_HAZE_ALPHA = 0.08f;
    private static final float WAVE_LEAD_SHARE = 0.012f;
    private static final float WAVE_TRAIL_SHARE = 0.08f;
    private static final float WAVE_ALPHA = 0.22f;

    private final InfestedGeometry geometry;
    private final InfestationSprites sprites;
    private final ThreatSwarm swarm;
    private final ThreatNodeKnots knots;
    private final ThreatFabricationPulse pulse;
    private final ThreatFragmentStreams streams;
    private final ThreatFragmentRim rim;
    private final ScreenFragmentSink sink = new ScreenFragmentSink(THREAT_GLOW);
    private final TexturedQuadBatch quadBatch = new TexturedQuadBatch();

    ThreatInfestation(InfestedGeometry geometry, InfestationSprites sprites) {
        this.geometry = geometry;
        this.sprites = sprites;
        this.swarm = new ThreatSwarm(geometry);
        this.knots = new ThreatNodeKnots(geometry);
        this.pulse = new ThreatFabricationPulse(geometry);
        this.streams = new ThreatFragmentStreams(geometry);
        this.rim = new ThreatFragmentRim(geometry);
    }

    @Override
    public void renderUnderNodes(TreeLayers layers, float elapsedSeconds) {
        TreeViewport viewport = layers.viewportFor(geometry.socket());
        float sceneAlpha = layers.alphaFor(geometry.socket());
        if (!isShown(viewport, sceneAlpha)) {
            return;
        }
        renderWaveAndRimLine(viewport, sceneAlpha, elapsedSeconds);
        SpriteAPI glowTexture = sprites.fragmentGlow();
        SpriteAPI fragmentTexture = sprites.fragment();
        if (glowTexture == null || fragmentTexture == null) {
            return;
        }
        try (TexturedQuadBatch glows = quadBatch.beginAdditive(glowTexture)) {
            sink.drawGlows(viewport, glows, sceneAlpha);
            emitUnderNodes(elapsedSeconds);
        }
        try (TexturedQuadBatch fragments = quadBatch.beginNormal(fragmentTexture)) {
            sink.drawFragments(viewport, fragments, sceneAlpha);
            emitUnderNodes(elapsedSeconds);
        }
    }

    @Override
    public void renderOverNodes(TreeLayers layers, float elapsedSeconds) {
        TreeViewport viewport = layers.viewportFor(geometry.socket());
        float sceneAlpha = layers.alphaFor(geometry.socket());
        if (!isShown(viewport, sceneAlpha)) {
            return;
        }
        SpriteAPI glowTexture = sprites.fragmentGlow();
        SpriteAPI fragmentTexture = sprites.fragment();
        if (glowTexture == null || fragmentTexture == null) {
            return;
        }
        try (TexturedQuadBatch glows = quadBatch.beginAdditive(glowTexture)) {
            sink.drawGlows(viewport, glows, sceneAlpha);
            pulse.emitNodeFlares(sink, elapsedSeconds);
            emitOverNodes(elapsedSeconds);
        }
        try (TexturedQuadBatch fragments = quadBatch.beginNormal(fragmentTexture)) {
            sink.drawFragments(viewport, fragments, sceneAlpha);
            emitOverNodes(elapsedSeconds);
        }
    }

    private void emitUnderNodes(float elapsedSeconds) {
        rim.emit(sink, elapsedSeconds);
        swarm.emit(sink, elapsedSeconds, false);
        streams.emit(sink, elapsedSeconds);
        knots.emit(sink, elapsedSeconds, false);
        pulse.emitSparks(sink, elapsedSeconds);
    }

    private void emitOverNodes(float elapsedSeconds) {
        knots.emit(sink, elapsedSeconds, true);
        swarm.emit(sink, elapsedSeconds, true);
    }

    private boolean isShown(TreeViewport viewport, float sceneAlpha) {
        if (sceneAlpha <= 0f || geometry.worldRadius() <= 0f) {
            return false;
        }
        return viewport.isVisible(viewport.screenX(geometry.socketX()), viewport.screenY(geometry.socketY()),
                geometry.worldRadius() * SCENE_REACH_SHARE * viewport.zoom());
    }

    private void renderWaveAndRimLine(TreeViewport viewport, float sceneAlpha, float elapsedSeconds) {
        float centreX = viewport.screenX(geometry.socketX());
        float centreY = viewport.screenY(geometry.socketY());
        float screenRadius = geometry.worldRadius() * viewport.zoom();
        float pulseSeconds = ThreatFabricationPulse.pulseSeconds(elapsedSeconds);
        float waveAlpha = WAVE_ALPHA * ThreatFabricationPulse.waveAlpha(pulseSeconds) * sceneAlpha;
        try (GlScope scope = GlScope.flat().additive()) {
            HoloShapes.radialGlow(centreX, centreY, screenRadius, screenRadius * (1f - RIM_HAZE_SHARE), THREAT_GLOW, RIM_HAZE_ALPHA * sceneAlpha);
            HoloShapes.circle(centreX, centreY, screenRadius, RIM_CIRCLE, THREAT_GLOW, RIM_LINE_THICKNESS, RIM_LINE_ALPHA * sceneAlpha);
            if (waveAlpha > 0f) {
                float frontRadius = pulse.frontWorldRadius(elapsedSeconds) * viewport.zoom();
                HoloShapes.radialGlow(centreX, centreY, frontRadius, frontRadius + screenRadius * WAVE_LEAD_SHARE, THREAT_GLOW, waveAlpha);
                HoloShapes.radialGlow(centreX, centreY, frontRadius, Math.max(0f, frontRadius - screenRadius * WAVE_TRAIL_SHARE), THREAT_GLOW,
                        waveAlpha);
            }
        }
    }
}
