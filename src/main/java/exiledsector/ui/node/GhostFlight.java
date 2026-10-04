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

    private static Color color;

    private final Vector2f from;
    private final Vector2f to;
    private final float duration;
    private final float arc;
    private final float phase;
    private float elapsed;

    private GhostFlight(Vector2f from, Vector2f to, float duration, float arc, float phase) {
        this.from = from;
        this.to = to;
        this.duration = duration;
        this.arc = arc;
        this.phase = phase;
    }

    public static Color color() {
        if (color == null) {
            color = FallbackSupport.getOrFallback(
                    () -> Global.getSector().getFaction(Factions.NEUTRAL).getBaseUIColor(), FALLBACK_COLOR,
                    Logger.getLogger(GhostFlight.class), "Failed to read neutral faction colour");
        }
        return color;
    }

    public static GhostFlight between(float fromX, float fromY, float toX, float toY, float speed, Random random) {
        Vector2f from = new Vector2f(fromX, fromY);
        Vector2f to = new Vector2f(toX, toY);
        float distance = Vector2f.sub(to, from, null).length();
        return new GhostFlight(from, to, Math.max(MIN_FLIGHT_SECONDS, distance / speed),
                (random.nextFloat() * 2f - 1f) * MAX_ARC_RATIO * distance, random.nextFloat() * 10f);
    }

    public boolean advance(float amount) {
        elapsed += amount;
        return elapsed >= duration;
    }

    public void draw(SpriteAPI sprite, Color color, float size, TreeViewport viewport, float alphaMult) {
        float t = elapsed / duration;
        Vector2f position = pointAt(t);
        Vector2f heading = headingAt(t);
        float fade = Math.min(1f, Math.min(elapsed, duration - elapsed) / FADE_SECONDS);
        float screenAngle = (float) Math.toDegrees(Math.atan2(-heading.y, heading.x));

        for (int i = 0; i < GHOSTS_PER_FLIGHT; i++) {
            Vector2f offset = formationOffset(i, heading, size / viewport.zoom());
            float flicker = (float) (0.5 + 0.5 * Math.sin(FLICKER_FREQUENCY * elapsed + phase + i * 2.1));
            sprite.setSize(size, size);
            sprite.setAngle(screenAngle - 90f);
            sprite.setColor(color);
            sprite.setAlphaMult((MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * flicker) * fade * alphaMult);
            sprite.renderAtCenter(viewport.screenX(position.x + offset.x), viewport.screenY(position.y + offset.y));
        }
    }

    private Vector2f formationOffset(int index, Vector2f heading, float spacing) {
        float back = index == 0 ? 0f : -WING_BACK_SPACING * spacing;
        float side = 0f;
        if (index > 0) {
            side = (index == 1 ? 1f : -1f) * WING_SIDE_SPACING * spacing;
        }
        side += (float) Math.sin(WOBBLE_FREQUENCY * elapsed + phase + index) * WOBBLE_RATIO * spacing;
        return new Vector2f(heading.x * back - heading.y * side, heading.y * back + heading.x * side);
    }

    private Vector2f control() {
        Vector2f delta = Vector2f.sub(to, from, null);
        float length = delta.length();
        Vector2f mid = new Vector2f((from.x + to.x) / 2f, (from.y + to.y) / 2f);
        if (length <= 0f) {
            return mid;
        }
        return new Vector2f(mid.x - delta.y / length * arc, mid.y + delta.x / length * arc);
    }

    private Vector2f pointAt(float t) {
        Vector2f control = control();
        float u = 1f - t;
        return new Vector2f(u * u * from.x + 2f * u * t * control.x + t * t * to.x,
                u * u * from.y + 2f * u * t * control.y + t * t * to.y);
    }

    private Vector2f headingAt(float t) {
        Vector2f control = control();
        float u = 1f - t;
        Vector2f heading = new Vector2f(2f * u * (control.x - from.x) + 2f * t * (to.x - control.x),
                2f * u * (control.y - from.y) + 2f * t * (to.y - control.y));
        if (heading.lengthSquared() <= 0f) {
            return new Vector2f(1f, 0f);
        }
        heading.normalise();
        return heading;
    }
}
