package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.SkillTree;
import exiledsector.skills.layout.RingBelt;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.belt.RingBeltRenderer;
import exiledsector.ui.util.SpriteCache;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

public class SkillTreeRingBeltRenderer {

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeRingBeltRenderer.class);
    private float elapsedSeconds = 0f;

    public void advance(float amount) {
        elapsedSeconds += amount;
    }

    public void render(TreeViewport viewport, float alphaMult) {
        if (alphaMult <= 0f) return;
        float zoom = viewport.zoom();
        List<RingBelt> ringBelts = SkillTree.getRingBelts();
        if (ringBelts.isEmpty()) return;

        for (RingBelt belt : ringBelts) {
            String ringArtPath = belt.getRingArtPath();
            SpriteAPI ringSprite = ringArtPath == null || ringArtPath.isEmpty() ? null : spriteCache.texture(ringArtPath);
            if (ringSprite == null) continue;

            float screenX = viewport.screenX(belt.getX());
            float screenY = viewport.screenY(belt.getY());
            float rotationDeg = belt.getRotation() + belt.getRotationSpeed() * elapsedSeconds;
            RadialBand band = new RadialBand(new Vector2f(screenX, screenY), belt.getInnerRadius() * zoom, belt.getOuterRadius() * zoom);
            RingBeltRenderer.render(ringSprite, band, Color.WHITE, alphaMult, rotationDeg, viewport);
        }
    }
}
