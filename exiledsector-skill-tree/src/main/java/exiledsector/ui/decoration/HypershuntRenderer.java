package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.decoration.Hypershunt;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.List;

public final class HypershuntRenderer implements DecorationRenderer {

    static final String SPRITE_PATH = "graphics/stations/coronal_shunt2.png";
    static final float MIN_ORBIT_SECONDS = 1f;

    record Pose(float treeX, float treeY, float spriteAngleDeg) {
    }

    private final SpriteCache spriteCache = new SpriteCache(HypershuntRenderer.class);
    private final List<Hypershunt> hypershunts;
    private float elapsedSeconds;

    HypershuntRenderer(List<Hypershunt> hypershunts) {
        this.hypershunts = List.copyOf(hypershunts);
    }

    @Override
    public void advance(float amount) {
        if (!hypershunts.isEmpty()) {
            elapsedSeconds += amount;
        }
    }

    static Pose pose(Hypershunt hypershunt, float atSeconds) {
        Hypershunt.Orbit orbit = hypershunt.getOrbit();
        float direction = orbit.clockwise() ? 1f : -1f;
        float angleDeg = orbit.startAngleDegrees() + direction * 360f * atSeconds / Math.max(MIN_ORBIT_SECONDS, orbit.seconds());
        double angleRad = Math.toRadians(angleDeg);
        float treeX = hypershunt.getX() + orbit.radius() * (float) Math.cos(angleRad);
        float treeY = hypershunt.getY() + orbit.radius() * (float) Math.sin(angleRad);
        return new Pose(treeX, treeY, -(angleDeg + 90f));
    }

    @Override
    public void render(DecorationScene scene, DecorationDepth depth) {
        if (depth != DecorationDepth.UNITS || hypershunts.isEmpty() || scene.sceneryAlpha() <= 0f) {
            return;
        }
        SpriteAPI sprite = spriteCache.sprite(SPRITE_PATH);
        if (sprite == null) {
            return;
        }
        TreeViewport viewport = scene.viewport();
        float zoom = viewport.zoom();
        for (Hypershunt hypershunt : hypershunts) {
            float alpha = scene.alphaFor(hypershunt.getId());
            Pose pose = pose(hypershunt, elapsedSeconds);
            float screenX = viewport.screenX(pose.treeX());
            float screenY = viewport.screenY(pose.treeY());
            float screenSize = hypershunt.getSize() * zoom;
            if (alpha > 0f && viewport.isVisible(screenX, screenY, screenSize)) {
                SpriteDraw.drawAtCenter(sprite, screenX, screenY, screenSize, screenSize, Color.WHITE, alpha, pose.spriteAngleDeg());
            }
        }
    }
}
