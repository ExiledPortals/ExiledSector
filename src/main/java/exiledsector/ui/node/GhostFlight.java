package exiledsector.ui.node;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.FallbackSupport;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

public final class GhostFlight {

    public static final String TEXTURE_PATH = SkillTreeNodeGhostRenderer.GHOST_TEXTURE_PATH;
    private static final int GHOSTS_PER_FLIGHT = 3;
    private static final float MIN_FLIGHT_SECONDS = 2.5f;
    private static final float FADE_SECONDS = 0.6f;
    private static final float MAX_ARC_RATIO = 0.2f;
    private static final float WING_BACK_SPACING = 0.9f;
    private static final float WING_SIDE_SPACING = 0.75f;
    private static final float WOBBLE_RATIO = 0.15f;
    private static final float WOBBLE_FREQUENCY = 3f;
    private static final float FLICKER_FREQUENCY = 5f;
    private static final float MIN_ALPHA = 0.45f;
    private static final float MAX_ALPHA = 0.9f;
    private static final Color FALLBACK_COLOR = new Color(155, 155, 155);

    private static Color neutralFactionColor;

    private final Vector2f startPoint;
    private final Vector2f endPoint;
    private final float flightDuration;
    private final float arcOffset;
    private final float animationPhase;
    private float flightElapsed;

    private GhostFlight(Vector2f startPoint, Vector2f endPoint, float flightDuration, float arcOffset, float animationPhase) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.flightDuration = flightDuration;
        this.arcOffset = arcOffset;
        this.animationPhase = animationPhase;
    }

    public static Color color() {
        if (neutralFactionColor == null) {
            neutralFactionColor = FallbackSupport.getOrFallback(
                    () -> Global.getSector().getFaction(Factions.NEUTRAL).getBaseUIColor(), FALLBACK_COLOR,
                    Logger.getLogger(GhostFlight.class), "Failed to read neutral faction colour");
        }
        return neutralFactionColor;
    }

    public static GhostFlight between(float fromX, float fromY, float toX, float toY, float speed, Random random) {
        Vector2f startPoint = new Vector2f(fromX, fromY);
        Vector2f endPoint = new Vector2f(toX, toY);
        float distance = Vector2f.sub(endPoint, startPoint, null).length();
        return new GhostFlight(startPoint, endPoint, Math.max(MIN_FLIGHT_SECONDS, distance / speed),
                (random.nextFloat() * 2f - 1f) * MAX_ARC_RATIO * distance, random.nextFloat() * 10f);
    }

    public boolean advance(float amount) {
        flightElapsed += amount;
        return flightElapsed >= flightDuration;
    }

    public void draw(SpriteAPI sprite, Color ghostColor, float ghostSize, TreeViewport viewport, float alphaMult) {
        float t = flightElapsed / flightDuration;
        Vector2f leaderPosition = pointAt(t);
        Vector2f heading = headingAt(t);
        float fade = Math.min(1f, Math.min(flightElapsed, flightDuration - flightElapsed) / FADE_SECONDS);
        float screenAngle = (float) Math.toDegrees(Math.atan2(-heading.y, heading.x));

        for (int i = 0; i < GHOSTS_PER_FLIGHT; i++) {
            Vector2f wingOffset = formationOffset(i, heading, ghostSize / viewport.zoom());
            float flicker = (float) (0.5 + 0.5 * Math.sin(FLICKER_FREQUENCY * flightElapsed + animationPhase + i * 2.1));
            sprite.setSize(ghostSize, ghostSize);
            sprite.setAngle(screenAngle - 90f);
            sprite.setColor(ghostColor);
            sprite.setAlphaMult((MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * flicker) * fade * alphaMult);
            sprite.renderAtCenter(viewport.screenX(leaderPosition.x + wingOffset.x), viewport.screenY(leaderPosition.y + wingOffset.y));
        }
    }

    private Vector2f formationOffset(int index, Vector2f heading, float spacing) {
        float back = index == 0 ? 0f : -WING_BACK_SPACING * spacing;
        float side = 0f;
        if (index > 0) {
            side = (index == 1 ? 1f : -1f) * WING_SIDE_SPACING * spacing;
        }
        side += (float) Math.sin(WOBBLE_FREQUENCY * flightElapsed + animationPhase + index) * WOBBLE_RATIO * spacing;
        return new Vector2f(heading.x * back - heading.y * side, heading.y * back + heading.x * side);
    }

    private Vector2f control() {
        Vector2f delta = Vector2f.sub(endPoint, startPoint, null);
        float length = delta.length();
        Vector2f mid = new Vector2f((startPoint.x + endPoint.x) / 2f, (startPoint.y + endPoint.y) / 2f);
        if (length <= 0f) {
            return mid;
        }
        return new Vector2f(mid.x - delta.y / length * arcOffset, mid.y + delta.x / length * arcOffset);
    }

    private Vector2f pointAt(float t) {
        Vector2f controlPoint = control();
        float u = 1f - t;
        return new Vector2f(u * u * startPoint.x + 2f * u * t * controlPoint.x + t * t * endPoint.x,
                u * u * startPoint.y + 2f * u * t * controlPoint.y + t * t * endPoint.y);
    }

    private Vector2f headingAt(float t) {
        Vector2f controlPoint = control();
        float u = 1f - t;
        Vector2f heading = new Vector2f(2f * u * (controlPoint.x - startPoint.x) + 2f * t * (endPoint.x - controlPoint.x),
                2f * u * (controlPoint.y - startPoint.y) + 2f * t * (endPoint.y - controlPoint.y));
        if (heading.lengthSquared() <= 0f) {
            return new Vector2f(1f, 0f);
        }
        heading.normalise();
        return heading;
    }
}
