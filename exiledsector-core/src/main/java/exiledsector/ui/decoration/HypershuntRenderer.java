package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.decoration.Hypershunt;
import exiledsector.skills.decoration.Orbit;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.List;

public final class HypershuntRenderer extends UnitDecorationRenderer<Hypershunt> {

    static final String SPRITE_PATH = "graphics/stations/coronal_shunt2.png";

    record Pose(float treeX, float treeY, float spriteAngleDeg) {
    }

    HypershuntRenderer(List<Hypershunt> hypershunts) {
        super(hypershunts);
    }

    static Pose pose(Hypershunt hypershunt, float atSeconds) {
        Orbit orbit = hypershunt.getOrbit();
        float angleDeg = orbit.angleDegAt(atSeconds);
        double angleRad = Math.toRadians(angleDeg);
        float treeX = hypershunt.getX() + orbit.radius() * (float) Math.cos(angleRad);
        float treeY = hypershunt.getY() + orbit.radius() * (float) Math.sin(angleRad);
        return new Pose(treeX, treeY, -(angleDeg + 90f));
    }

    @Override
    void renderUnit(TreeViewport viewport, Hypershunt hypershunt, float alphaMult) {
        SpriteAPI sprite = spriteCache.sprite(SPRITE_PATH);
        if (sprite == null) {
            return;
        }
        Pose pose = pose(hypershunt, elapsedSeconds);
        float screenX = viewport.screenX(pose.treeX());
        float screenY = viewport.screenY(pose.treeY());
        float screenSize = hypershunt.getSize() * viewport.zoom();
        if (viewport.isVisible(screenX, screenY, screenSize)) {
            SpriteDraw.drawAtCenter(sprite, screenX, screenY, screenSize, screenSize, Color.WHITE, alphaMult, pose.spriteAngleDeg());
        }
    }
}
