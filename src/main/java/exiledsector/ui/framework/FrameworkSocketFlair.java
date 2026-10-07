package exiledsector.ui.framework;

import com.fs.starfarer.api.util.Misc;
import exiledsector.socketables.SocketType;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

public final class FrameworkSocketFlair {

    public record SocketLook(float hover, boolean selected, boolean active, String iconPath) {
    }

    private static final float OUTER_RING_SPEED = 14f;
    private static final float TICK_RING_SPEED = -8f;
    private static final float SCANNER_SPEED = -50f;
    private static final float ORBIT_SPEED = 0.35f;
    private static final float SCAN_SPEED = 0.5f;
    private static final float SQUARE_SHARE = 0.42f;
    private static final float ICON_INSET = 0.1f;
    private static final float BRACKET_HOVER_TRAVEL = 6f;
    private static final Color SOCKET_FILL = new Color(0, 0, 0, 190);
    private static final Color INACTIVE_COLOR = new Color(200, 70, 60);
    private static final SpriteCache ICONS = new SpriteCache(FrameworkSocketFlair.class);
    private static final Map<SocketType, Color> TYPE_COLORS = new EnumMap<>(SocketType.class);

    static {
        TYPE_COLORS.put(SocketType.BRIDGE, new Color(255, 205, 90));
        TYPE_COLORS.put(SocketType.CREW_QUARTERS, new Color(120, 220, 170));
        TYPE_COLORS.put(SocketType.ENGINE_ROOM, new Color(255, 150, 60));
        TYPE_COLORS.put(SocketType.REACTOR, new Color(190, 255, 110));
        TYPE_COLORS.put(SocketType.WEAPON_MOUNT, new Color(255, 95, 85));
        TYPE_COLORS.put(SocketType.SHIELD_GENERATOR, new Color(90, 170, 255));
        TYPE_COLORS.put(SocketType.PHASE_COIL, new Color(200, 120, 255));
        TYPE_COLORS.put(SocketType.FLIGHT_DECK, new Color(90, 230, 255));
    }

    private FrameworkSocketFlair() {
    }

    public static Color colorOf(SocketType socketType) {
        return TYPE_COLORS.getOrDefault(socketType, Misc.getBasePlayerColor());
    }

    public static Color inactiveColor() {
        return INACTIVE_COLOR;
    }

    public static void render(float cx, float cy, float radius, Color typeColor, SocketLook look, float elapsedSeconds, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        try (GlScope scope = GlScope.save(GlScope.DRAW_ATTRIBS)) {
            renderLayers(cx, cy, radius, look.active() ? typeColor : INACTIVE_COLOR, look, elapsedSeconds,
                    look.active() ? alpha : alpha * 0.65f);
        }
    }

    private static void renderLayers(float cx, float cy, float radius, Color accent, SocketLook look, float elapsedSeconds, float alpha) {
        float hover = look.hover();
        float pulse = 0.5f + 0.5f * (float) Math.sin(elapsedSeconds * 2.4f);
        float half = radius * SQUARE_SHARE;
        boolean filled = look.iconPath() != null;

        HoloShapes.additive();
        HoloShapes.radialGlow(cx, cy, half * 0.8f, radius * 1.45f, accent, (0.1f + 0.08f * pulse + 0.18f * hover) * alpha);
        HoloShapes.circle(cx, cy, radius, accent, 1f, (0.3f + 0.3f * hover) * alpha);
        HoloShapes.arcSegments(cx, cy, radius - 4f, 8, 0.55f, elapsedSeconds * OUTER_RING_SPEED, 2.5f, accent, (0.5f + 0.3f * hover) * alpha);
        HoloShapes.ticks(cx, cy, radius * 0.84f, 60, 5, radius * 0.12f, radius * 0.05f, elapsedSeconds * TICK_RING_SPEED, accent,
                0.4f * alpha);
        HoloShapes.arcSegments(cx, cy, radius * 0.68f, 3, 0.18f, elapsedSeconds * SCANNER_SPEED, 2f, accent,
                (filled ? 0.7f : 0.35f) * alpha);
        float orbitAngle = (elapsedSeconds * ORBIT_SPEED % 1f) * HoloShapes.TWO_PI;
        HoloShapes.diamond(cx + (float) Math.cos(orbitAngle) * radius, cy + (float) Math.sin(orbitAngle) * radius, 3f, accent, 0.9f * alpha);
        HoloShapes.diamond(cx - (float) Math.cos(orbitAngle) * radius, cy - (float) Math.sin(orbitAngle) * radius, 2f, accent, 0.5f * alpha);
        if (look.selected()) {
            HoloShapes.circle(cx, cy, radius + 6f + 2f * pulse, accent, 2f, (0.5f + 0.4f * pulse) * alpha);
            HoloShapes.arcSegments(cx, cy, radius + 12f, 4, 0.12f, -elapsedSeconds * OUTER_RING_SPEED * 2f, 3f, accent, 0.8f * alpha);
        }
        HoloShapes.normalBlend();

        GLDraw.fillQuad(cx - half, cy - half, half * 2f, half * 2f, SOCKET_FILL, alpha);
        GLDraw.innerGlow(cx - half, cy - half, half * 2f, half * 2f, half * 0.5f, accent, (0.15f + 0.25f * pulse + 0.2f * hover) * alpha);
        GLDraw.strokeQuad(cx - half, cy - half, half * 2f, half * 2f, accent, 1.5f, (0.55f + 0.35f * hover) * alpha);
        if (filled) {
            float size = half * 2f * (1f - ICON_INSET * 2f);
            SpriteDraw.drawAtCenter(ICONS, look.iconPath(), cx, cy, size, size, Color.WHITE, alpha);
        }
        HoloShapes.additive();
        if (!filled) {
            float arm = half * 0.35f * (0.8f + 0.2f * pulse);
            HoloShapes.line(cx - arm, cy, cx + arm, cy, 2f, accent, (0.35f + 0.4f * hover) * alpha);
            HoloShapes.line(cx, cy - arm, cx, cy + arm, 2f, accent, (0.35f + 0.4f * hover) * alpha);
        }
        if (!look.active()) {
            HoloShapes.line(cx - half, cy - half, cx + half, cy + half, 2f, INACTIVE_COLOR, 0.8f * alpha);
            HoloShapes.line(cx - half, cy + half, cx + half, cy - half, 2f, INACTIVE_COLOR, 0.8f * alpha);
        }
        HoloShapes.brackets(cx, cy, half + 5f + hover * BRACKET_HOVER_TRAVEL, half * 0.35f, accent, (0.65f + 0.35f * hover) * alpha);
        if (filled && look.active()) {
            scanLine(cx, cy, half, accent, elapsedSeconds, alpha);
        }
        HoloShapes.normalBlend();
    }

    private static void scanLine(float cx, float cy, float half, Color accent, float elapsedSeconds, float alpha) {
        float travel = (elapsedSeconds * SCAN_SPEED) % 1f;
        float y = cy + half - travel * half * 2f;
        for (int i = 0; i < 3; i++) {
            float lineY = y + i * 2f;
            if (lineY > cy + half) {
                break;
            }
            HoloShapes.line(cx - half, lineY, cx + half, lineY, i == 0 ? 2f : 1f, accent, (i == 0 ? 0.7f : 0.25f / i) * alpha);
        }
    }

    public static void leader(float socketEdgeX, float socketY, float elbowX, float anchorX, float anchorY, Color accent, float elapsedSeconds,
                              float alpha) {
        if (alpha <= 0f) {
            return;
        }
        try (GlScope scope = GlScope.save(GlScope.DRAW_ATTRIBS)) {
            HoloShapes.additive();
            HoloShapes.line(socketEdgeX, socketY, elbowX, socketY, 4f, accent, 0.12f * alpha);
            HoloShapes.line(elbowX, socketY, anchorX, anchorY, 4f, accent, 0.12f * alpha);
            HoloShapes.line(socketEdgeX, socketY, elbowX, socketY, 1.5f, accent, 0.75f * alpha);
            HoloShapes.line(elbowX, socketY, anchorX, anchorY, 1.5f, accent, 0.75f * alpha);
            HoloShapes.diamond(elbowX, socketY, 3f, accent, 0.9f * alpha);
            float firstLength = Math.abs(elbowX - socketEdgeX);
            float secondLength = (float) Math.hypot(anchorX - elbowX, anchorY - socketY);
            float travel = (elapsedSeconds * 0.6f) % 1f * (firstLength + secondLength);
            float pulseX;
            float pulseY;
            if (travel < firstLength) {
                pulseX = socketEdgeX + Math.signum(elbowX - socketEdgeX) * travel;
                pulseY = socketY;
            } else {
                float share = secondLength <= 0f ? 1f : (travel - firstLength) / secondLength;
                pulseX = elbowX + (anchorX - elbowX) * share;
                pulseY = socketY + (anchorY - socketY) * share;
            }
            HoloShapes.diamond(pulseX, pulseY, 2.5f, Color.WHITE, 0.8f * alpha);
            float markerPulse = 0.5f + 0.5f * (float) Math.sin(elapsedSeconds * 3f);
            HoloShapes.circle(anchorX, anchorY, 6f + 2f * markerPulse, accent, 1.5f, (0.5f + 0.4f * markerPulse) * alpha);
            HoloShapes.diamond(anchorX, anchorY, 2.5f, accent, alpha);
            HoloShapes.normalBlend();
            GL11.glLineWidth(1f);
        }
    }
}
