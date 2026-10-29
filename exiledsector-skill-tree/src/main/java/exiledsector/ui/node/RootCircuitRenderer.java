package exiledsector.ui.node;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.TexturedQuadBatch;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class RootCircuitRenderer {

    static final String HALO = "graphics/fx/star_halo.png";
    static final String CORE_GLOW = "graphics/fx/owner_glow.png";
    static final String RIM = "graphics/fx/shields256ringc.png";
    static final String TRACE_FRINGE = "graphics/fx/beamfringeb.png";
    static final String TRACE_CORE = "graphics/fx/beamcorec.png";
    static final String DOT_GLOW = "graphics/fx/hit_glow_small.png";
    static final String DOT = "graphics/fx/particlealpha32sq.png";
    static final String SPARK = "graphics/fx/particlealpha64sq.png";
    private static final List<String> TEXTURES = List.of(HALO, CORE_GLOW, RIM, TRACE_FRINGE, TRACE_CORE, DOT_GLOW, DOT, SPARK);

    private static final float HALO_SIZE_RADII = 8f;
    private static final float HALO_ALPHA = 0.3f;
    private static final float HALO_REACH_RADII = 3f;
    private static final float CORE_GLOW_SIZE_RADII = 2.1f;
    private static final float CORE_GLOW_ALPHA = 0.55f;
    private static final float RIM_SIZE_RADII = 2.1f;
    private static final float RIM_OUTER_SIZE_RADII = 2.16f;
    private static final float RIM_OUTER_ALPHA = 0.8f;
    private static final float FRINGE_THICKNESS_RADII = 0.3f;
    private static final float FRINGE_OVERLAP_RADII = 0.06f;
    private static final float FRINGE_ALPHA = 0.3f;
    private static final float CORE_THICKNESS_RADII = 0.14f;
    private static final float CORE_OVERLAP_RADII = 0.03f;
    private static final float CORE_ALPHA = 0.95f;
    private static final float DOT_GLOW_SIZE_RADII = 0.42f;
    private static final float DOT_GLOW_ALPHA = 0.75f;
    private static final float DOT_SIZE_RADII = 0.16f;
    private static final float DOT_FADE_IN_RADII = 0.2f;
    private static final float MIN_SPARK_ROOT_RADIUS_PX = 12f;
    private static final int MAX_SPARKS_PER_TRACE = 4;
    private static final int TAIL_COPIES = 4;
    private static final float TAIL_SPACING_RADII = 0.06f;
    private static final float SPARK_SIZE_RADII = 0.34f;
    private static final float TAIL_SHRINK_PER_COPY = 0.18f;
    private static final float TAIL_FADE_PER_COPY = 0.22f;
    private static final float END_FADE_RADII = 0.15f;
    private static final float CLOCK_WRAP_SECONDS = RootCircuit.SPARK_INTERVAL_SECONDS * 1000f;

    private final SpriteCache sprites = new SpriteCache(RootCircuitRenderer.class);
    private final TexturedQuadBatch batch = new TexturedQuadBatch();
    private final Map<Color, Color> whitenedColours = new HashMap<>();
    private final float[] sparkDistances = new float[MAX_SPARKS_PER_TRACE];
    private final float[] point = new float[2];
    private float clockSeconds;

    void advance(float amount) {
        clockSeconds = (clockSeconds + amount) % CLOCK_WRAP_SECONDS;
    }

    float reachRadius(float rootRadius) {
        return Math.max(RootCircuit.get().extentRadii(), HALO_REACH_RADII) * rootRadius;
    }

    void draw(float centerX, float centerY, float rootRadius, float alphaMult, Color rootColour, float grownRadii) {
        RootCircuit circuit = RootCircuit.get();
        if (circuit.traces().isEmpty() || !texturesLoaded()) {
            return;
        }
        Color whitened = whitenedColours.computeIfAbsent(rootColour, NodeBeams::whitened);
        float growth = Math.min(1f, grownRadii / circuit.longestTraceRadii());
        if (growth > 0f) {
            centredQuad(HALO, centerX, centerY, HALO_SIZE_RADII * rootRadius, rootColour, alphaMult * HALO_ALPHA * growth);
        }
        centredQuad(CORE_GLOW, centerX, centerY, CORE_GLOW_SIZE_RADII * rootRadius, rootColour, alphaMult * CORE_GLOW_ALPHA);
        if (growth > 0f) {
            drawTraces(circuit, TRACE_FRINGE, centerX, centerY, rootRadius, grownRadii, FRINGE_THICKNESS_RADII, FRINGE_OVERLAP_RADII, rootColour,
                    alphaMult * FRINGE_ALPHA);
            drawTraces(circuit, TRACE_CORE, centerX, centerY, rootRadius, grownRadii, CORE_THICKNESS_RADII, CORE_OVERLAP_RADII, whitened,
                    alphaMult * CORE_ALPHA);
            drawDots(circuit, DOT_GLOW, centerX, centerY, DOT_GLOW_SIZE_RADII * rootRadius, rootRadius, grownRadii, rootColour,
                    alphaMult * DOT_GLOW_ALPHA);
            drawDots(circuit, DOT, centerX, centerY, DOT_SIZE_RADII * rootRadius, rootRadius, grownRadii, Color.WHITE, alphaMult);
        }
        centredQuad(RIM, centerX, centerY, RIM_SIZE_RADII * rootRadius, whitened, alphaMult);
        centredQuad(RIM, centerX, centerY, RIM_OUTER_SIZE_RADII * rootRadius, rootColour, alphaMult * RIM_OUTER_ALPHA);
        if (growth > 0f && rootRadius >= MIN_SPARK_ROOT_RADIUS_PX) {
            if (growth < 1f) {
                drawGrowingTips(circuit, centerX, centerY, rootRadius, grownRadii, alphaMult, rootColour, whitened);
            } else {
                drawSparks(circuit, centerX, centerY, rootRadius, alphaMult, rootColour, whitened);
            }
        }
    }

    private boolean texturesLoaded() {
        for (String texture : TEXTURES) {
            if (!sprites.ensureLoaded(texture)) {
                return false;
            }
        }
        return true;
    }

    private SpriteAPI texture(String path) {
        return sprites.sprite(path);
    }

    private void centredQuad(String texturePath, float centerX, float centerY, float size, Color tint, float alphaMult) {
        try (TexturedQuadBatch quads = batch.beginAdditive(texture(texturePath))) {
            quads.quadAtCenter(centerX, centerY, size, size, 0f, tint, alphaMult);
        }
    }

    private void drawTraces(RootCircuit circuit, String texturePath, float centerX, float centerY, float rootRadius, float grownRadii,
                            float thicknessRadii, float overlapRadii, Color tint, float alphaMult) {
        float thickness = thicknessRadii * rootRadius;
        try (TexturedQuadBatch quads = batch.beginAdditive(texture(texturePath))) {
            for (RootCircuit.Trace trace : circuit.traces()) {
                float[] distances = trace.distances();
                for (int segment = 0; segment < trace.segmentAngles().length && distances[segment] < grownRadii; segment++) {
                    float shownLength = Math.min(distances[segment + 1], grownRadii) - distances[segment];
                    trace.pointAt(distances[segment] + shownLength / 2f, point);
                    quads.quadAtCenter(centerX + point[0] * rootRadius, centerY + point[1] * rootRadius, (shownLength + overlapRadii) * rootRadius,
                            thickness, trace.segmentAngles()[segment], tint, alphaMult);
                }
            }
        }
    }

    private void drawDots(RootCircuit circuit, String texturePath, float centerX, float centerY, float size, float rootRadius, float grownRadii,
                          Color tint, float alphaMult) {
        try (TexturedQuadBatch quads = batch.beginAdditive(texture(texturePath))) {
            for (RootCircuit.Trace trace : circuit.traces()) {
                float dotFadeIn = Math.min(1f, (grownRadii - trace.length()) / DOT_FADE_IN_RADII);
                if (dotFadeIn <= 0f) {
                    continue;
                }
                int last = trace.xs().length - 1;
                quads.quadAtCenter(centerX + trace.xs()[last] * rootRadius, centerY + trace.ys()[last] * rootRadius, size, size, 0f, tint,
                        alphaMult * dotFadeIn);
            }
        }
    }

    private void drawGrowingTips(RootCircuit circuit, float centerX, float centerY, float rootRadius, float grownRadii, float alphaMult,
                                 Color rootColour, Color headColour) {
        try (TexturedQuadBatch quads = batch.beginAdditive(texture(SPARK))) {
            for (RootCircuit.Trace trace : circuit.traces()) {
                if (grownRadii < trace.length()) {
                    drawSpark(quads, trace, grownRadii, centerX, centerY, rootRadius, alphaMult, rootColour, headColour);
                }
            }
        }
    }

    private void drawSparks(RootCircuit circuit, float centerX, float centerY, float rootRadius, float alphaMult, Color rootColour,
                            Color headColour) {
        try (TexturedQuadBatch quads = batch.beginAdditive(texture(SPARK))) {
            for (RootCircuit.Trace trace : circuit.traces()) {
                int sparkCount = trace.sparkDistances(clockSeconds, sparkDistances);
                for (int sparkIndex = 0; sparkIndex < sparkCount; sparkIndex++) {
                    drawSpark(quads, trace, sparkDistances[sparkIndex], centerX, centerY, rootRadius, alphaMult, rootColour, headColour);
                }
            }
        }
    }

    private void drawSpark(TexturedQuadBatch quads, RootCircuit.Trace trace, float headDistance, float centerX, float centerY, float rootRadius,
                           float alphaMult, Color rootColour, Color headColour) {
        float endFade = Math.max(0f, Math.min(1f, (trace.length() - headDistance) / END_FADE_RADII));
        for (int copy = TAIL_COPIES; copy >= 0; copy--) {
            float copyDistance = headDistance - copy * TAIL_SPACING_RADII;
            if (copyDistance < 0f) {
                continue;
            }
            trace.pointAt(copyDistance, point);
            float copySize = SPARK_SIZE_RADII * rootRadius * (1f - copy * TAIL_SHRINK_PER_COPY);
            quads.quadAtCenter(centerX + point[0] * rootRadius, centerY + point[1] * rootRadius, copySize, copySize, 0f,
                    copy == 0 ? headColour : rootColour, alphaMult * endFade * (1f - copy * TAIL_FADE_PER_COPY));
        }
    }
}
