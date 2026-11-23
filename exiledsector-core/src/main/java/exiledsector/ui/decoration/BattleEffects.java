package exiledsector.ui.decoration;

import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.TexturedQuadBatch;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class BattleEffects {

    static final float SHOT_SECONDS = 0.35f;
    static final float SHOT_SIZE = 5f;
    static final float SHOT_STREAK_MULT = 4f;
    static final float IMPACT_SECONDS = 0.25f;
    static final float IMPACT_SIZE = 9f;
    static final float EXPLOSION_SECONDS = 1.2f;
    static final float EXPLOSION_SIZE_MULT = 2.5f;
    static final float MIN_SCREEN_SIZE = 2f;
    private static final Color IMPACT_COLOR = new Color(255, 220, 170);
    private static final Color EXPLOSION_COLOR = new Color(255, 140, 50);
    private static final Color EXPLOSION_CORE_COLOR = new Color(255, 240, 210);

    private enum Kind { SHOT, IMPACT, EXPLOSION }

    record ShipSpot(float shipX, float shipY, float fleetX, float fleetY) {

        static ShipSpot of(FactionFleet fleet, RoamingShip ship) {
            return new ShipSpot(fleet.shipX(ship), fleet.shipY(ship), fleet.flight().positionX(), fleet.flight().positionY());
        }
    }

    private static final class Effect {
        private final Kind kind;
        private final ShipSpot from;
        private final ShipSpot to;
        private final float size;
        private final float lifeSeconds;
        private final Color color;
        private float ageSeconds;

        private Effect(Kind kind, ShipSpot from, ShipSpot to, float size, float lifeSeconds, Color color) {
            this.kind = kind;
            this.from = from;
            this.to = to;
            this.size = size;
            this.lifeSeconds = lifeSeconds;
            this.color = color;
        }

        private float progress() {
            return Math.min(1f, ageSeconds / lifeSeconds);
        }
    }

    private final List<Effect> effects = new ArrayList<>();
    private final List<Effect> spawned = new ArrayList<>();
    private final float[] placed = new float[3];

    void shot(ShipSpot from, ShipSpot to, Color color) {
        effects.add(new Effect(Kind.SHOT, from, to, SHOT_SIZE, SHOT_SECONDS, color));
    }

    void explosion(ShipSpot at, float shipLength) {
        effects.add(new Effect(Kind.EXPLOSION, at, at, shipLength * EXPLOSION_SIZE_MULT, EXPLOSION_SECONDS, EXPLOSION_COLOR));
    }

    boolean isEmpty() {
        return effects.isEmpty();
    }

    int size() {
        return effects.size();
    }

    void advance(float amount) {
        Iterator<Effect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            Effect effect = iterator.next();
            effect.ageSeconds += amount;
            if (effect.ageSeconds >= effect.lifeSeconds) {
                iterator.remove();
                if (effect.kind == Kind.SHOT) {
                    spawned.add(new Effect(Kind.IMPACT, effect.to, effect.to, IMPACT_SIZE, IMPACT_SECONDS, IMPACT_COLOR));
                }
            }
        }
        effects.addAll(spawned);
        spawned.clear();
    }

    void render(TexturedQuadBatch glows, DepthLens lens, float alphaMult) {
        TreeViewport viewport = lens.view();
        float zoom = viewport.zoom();
        for (Effect effect : effects) {
            float progress = effect.progress();
            ShipSpot from = effect.from;
            ShipSpot to = effect.to;
            float worldX = lerp(from.shipX(), to.shipX(), progress);
            float worldY = lerp(from.shipY(), to.shipY(), progress);
            float fleetX = lerp(from.fleetX(), to.fleetX(), progress);
            float fleetY = lerp(from.fleetY(), to.fleetY(), progress);
            lens.place(fleetX, fleetY, placed);
            float scale = placed[DepthLens.SCALE];
            float screenX = placed[DepthLens.X] + (viewport.screenX(worldX) - viewport.screenX(fleetX)) * scale;
            float screenY = placed[DepthLens.Y] + (viewport.screenY(worldY) - viewport.screenY(fleetY)) * scale;
            float screenSize = Math.max(MIN_SCREEN_SIZE, effect.size * zoom * scale);
            if (!viewport.isVisible(screenX, screenY, screenSize * SHOT_STREAK_MULT)) {
                continue;
            }
            switch (effect.kind) {
                case SHOT -> {
                    float angleDeg = (float) Math.toDegrees(Math.atan2(-(to.shipY() - from.shipY()), to.shipX() - from.shipX()));
                    glows.quadAtCenter(screenX, screenY, screenSize * SHOT_STREAK_MULT, screenSize, angleDeg, effect.color, alphaMult);
                    glows.quadAtCenter(screenX, screenY, screenSize, screenSize, 0f, Color.WHITE, alphaMult * 0.6f);
                }
                case IMPACT -> glows.quadAtCenter(screenX, screenY, screenSize * (1f + progress), screenSize * (1f + progress), 0f, effect.color,
                        alphaMult * (1f - progress));
                case EXPLOSION -> {
                    float fade = (1f - progress) * (1f - progress);
                    float outer = screenSize * (0.5f + progress);
                    glows.quadAtCenter(screenX, screenY, outer, outer, 0f, effect.color, alphaMult * fade);
                    glows.quadAtCenter(screenX, screenY, outer * 0.5f, outer * 0.5f, 0f, EXPLOSION_CORE_COLOR, alphaMult * fade);
                }
            }
        }
    }

    private static float lerp(float from, float to, float share) {
        return from + (to - from) * share;
    }
}
