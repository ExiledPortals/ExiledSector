package exiledsector.ui.decoration;

import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.List;

final class FlagshipGroup {

    static final float ESCORT_SCALE_BOOST = 1.15f;
    static final float GLOW_LENGTH_SHARE = 1.5f;
    private static final float GLOW_ALPHA = 0.3f;
    private static final String GLOW_PATH = "graphics/fx/hit_glow.png";

    record Escort(RoamingShip ship, EscortPath path) {
    }

    record Flagship(RoamingShip ship, float scale, float engineLevel) {

        float length() {
            return Math.max(ship.width(), ship.height()) * scale;
        }
    }

    private final SpriteCache spriteCache;
    private final Flagship flagship;
    private final GroupPose pose;
    private final List<Escort> escorts;
    private final CommandPulse pulse;
    private final Color colour;

    FlagshipGroup(SpriteCache spriteCache, Flagship flagship, GroupPose pose, List<Escort> escorts, CommandPulse pulse, Color colour) {
        this.spriteCache = spriteCache;
        this.flagship = flagship;
        this.pose = pose;
        this.escorts = List.copyOf(escorts);
        this.pulse = pulse;
        this.colour = colour;
    }

    GroupPose pose() {
        return pose;
    }

    CommandPulse pulse() {
        return pulse;
    }

    float flagshipLength() {
        return flagship.length();
    }

    float shipScale() {
        return flagship.scale();
    }

    int shipCount() {
        return 1 + escorts.size();
    }

    void advance(float amount, float baseHeadingDeg, float seconds) {
        float headingDeg = pose.headingAt(baseHeadingDeg, seconds);
        flagship.ship().drift().advance(amount, headingDeg);
        for (Escort escort : escorts) {
            escort.ship().drift().advance(amount, escort.path().facingAt(seconds, headingDeg));
        }
    }

    void renderGlow(SceneFrame frame) {
        float size = flagship.length() * GLOW_LENGTH_SHARE * frame.zoom();
        SpriteDraw.drawAdditiveAtCenter(spriteCache, GLOW_PATH, frame.screenXOf(pose.x()), frame.screenYOf(pose.y()), size, size, colour,
                frame.alpha() * GLOW_ALPHA);
    }

    void addShips(DrawnShips drawnShips, SceneFrame frame) {
        float seconds = frame.seconds();
        float alpha = frame.alpha();
        float headingDeg = pose.headingAt(frame.baseHeadingDeg(), seconds);
        float centreX = frame.screenXOf(pose.x());
        float centreY = frame.screenYOf(pose.y());
        float flagshipScale = flagship.scale() * frame.zoom();
        if (drawnShips.add(flagship.ship(), centreX, centreY, flagshipScale, flagship.engineLevel(), alpha)) {
            flagship.ship().drawHull(spriteCache, centreX, centreY, flagshipScale, alpha);
        }
        float escortScale = flagshipScale * ESCORT_SCALE_BOOST;
        for (Escort escort : escorts) {
            RoamingShip ship = escort.ship();
            float x = centreX + (escort.path().xAt(seconds, headingDeg) + ship.drift().offsetX()) * frame.zoom();
            float y = centreY + (escort.path().yAt(seconds, headingDeg) + ship.drift().offsetY()) * frame.zoom();
            if (drawnShips.add(ship, x, y, escortScale, escort.path().engineLevelAt(seconds), alpha)) {
                ship.drawHull(spriteCache, x, y, escortScale, alpha);
            }
        }
    }

    void renderPulse(SceneFrame frame) {
        if (pulse == null) {
            return;
        }
        for (int ring = 0; ring < pulse.rings(); ring++) {
            float progress = pulse.ringProgressAt(ring, frame.seconds());
            if (progress < 0f) {
                continue;
            }
            float size = 2f * pulse.reach() * progress * frame.zoom();
            SpriteDraw.drawAdditiveAtCenter(spriteCache, AtlasShipGroup.PING_TEXTURE, frame.screenXOf(pose.x()), frame.screenYOf(pose.y()), size,
                    size, colour, frame.alpha() * pulse.strength() * (1f - progress));
        }
    }
}
