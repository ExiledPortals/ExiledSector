package exiledsector.ui.util;

import java.awt.Color;

public final class HoloSocketFrame {

    static final float SQUARE_SHARE = 0.42f;
    private static final float GLOW_INNER_SHARE = 0.8f;
    private static final int SCANNER_ARCS = 3;
    private static final float SCANNER_WIDTH = 2f;
    private static final float TICK_WIDTH = 1f;
    private static final float SQUARE_WIDTH = 1.5f;
    private static final float INNER_GLOW_SHARE = 0.5f;
    private static final float BRACKET_ARM_SHARE = 0.35f;
    private static final float BRACKET_WIDTH = 2.5f;

    public record OuterArcs(float inset, int count, float fill, float speed, float width) {
    }

    public record Ticks(float radiusShare, int count, int majorEvery, float majorLength, float minorLength, boolean lengthsScaleWithRadius,
                        float speed) {

        float majorLengthAt(float radius) {
            return lengthsScaleWithRadius ? radius * majorLength : majorLength;
        }

        float minorLengthAt(float radius) {
            return lengthsScaleWithRadius ? radius * minorLength : minorLength;
        }
    }

    public record Scanner(float radiusShare, float fill, float speed) {
    }

    public record ScanLines(float speed, int lineCount, float headAlpha, float tailAlpha) {
    }

    public record Spec(float glowReachShare, float ringSpeed, OuterArcs outerArcs, Ticks ticks, Scanner scanner, Color socketFill,
                       ScanLines scanLines) {
    }

    public record RingShading(Color accent, Color glowColor, float glowAlpha, float ringAlpha, float outerArcAlpha, float tickAlpha,
                              Color scannerColor, float scannerAlpha) {
    }

    private HoloSocketFrame() {
    }

    public static float socketHalf(float radius) {
        return radius * SQUARE_SHARE;
    }

    public static void drawRings(Spec spec, float cx, float cy, float radius, float elapsedSeconds, RingShading shading) {
        Color accent = shading.accent();
        HoloShapes.radialGlow(cx, cy, socketHalf(radius) * GLOW_INNER_SHARE, radius * spec.glowReachShare(), shading.glowColor(), shading.glowAlpha());
        HoloBeams.ring(cx, cy, radius, -elapsedSeconds * spec.ringSpeed(), accent, shading.ringAlpha());
        OuterArcs outerArcs = spec.outerArcs();
        HoloBeams.arcSegments(cx, cy, radius - outerArcs.inset(), outerArcs.count(), outerArcs.fill(), elapsedSeconds * outerArcs.speed(),
                outerArcs.width(), accent, shading.outerArcAlpha());
        Ticks ticks = spec.ticks();
        HoloBeams.ticks(cx, cy, radius * ticks.radiusShare(), ticks.count(), ticks.majorEvery(), ticks.majorLengthAt(radius),
                ticks.minorLengthAt(radius), elapsedSeconds * ticks.speed(), TICK_WIDTH, accent, shading.tickAlpha());
        Scanner scanner = spec.scanner();
        HoloBeams.arcSegments(cx, cy, radius * scanner.radiusShare(), SCANNER_ARCS, scanner.fill(), elapsedSeconds * scanner.speed(), SCANNER_WIDTH,
                shading.scannerColor(), shading.scannerAlpha());
    }

    public static void drawSocket(Spec spec, float cx, float cy, float half, Color socketColor, float alpha, float innerGlowAlpha,
                                  float squareAlpha) {
        GLDraw.fillQuad(cx - half, cy - half, half * 2f, half * 2f, spec.socketFill(), alpha);
        GLDraw.innerGlow(cx - half, cy - half, half * 2f, half * 2f, half * INNER_GLOW_SHARE, socketColor, innerGlowAlpha);
        HoloBeams.square(cx, cy, half, SQUARE_WIDTH, socketColor, squareAlpha);
    }

    public static void drawBrackets(float cx, float cy, float bracketHalf, float socketHalf, Color accent, float alpha) {
        HoloBeams.brackets(cx, cy, bracketHalf, socketHalf * BRACKET_ARM_SHARE, BRACKET_WIDTH, accent, alpha);
    }

    public static void drawScanLines(Spec spec, float cx, float cy, float half, Color lineColor, float elapsedSeconds, float alpha) {
        ScanLines scanLines = spec.scanLines();
        float travel = (elapsedSeconds * scanLines.speed()) % 1f;
        float headY = cy + half - travel * half * 2f;
        for (int i = 0; i < scanLines.lineCount(); i++) {
            float lineY = headY + i * 2f;
            if (lineY > cy + half) {
                break;
            }
            HoloBeams.line(cx - half, lineY, cx + half, lineY, i == 0 ? 2f : 1f, lineColor,
                    (i == 0 ? scanLines.headAlpha() : scanLines.tailAlpha() / i) * alpha);
        }
    }
}
