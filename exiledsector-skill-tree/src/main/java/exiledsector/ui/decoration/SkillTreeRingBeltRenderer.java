package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.SkillTree;
import exiledsector.skills.layout.RingBelt;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.belt.RingBeltRenderer;
import exiledlib.skills.skilleffect.SpriteCache;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

public class SkillTreeRingBeltRenderer {

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeRingBeltRenderer.class);
    private RadialBand[] bandByBelt = new RadialBand[0];
    private float elapsedSeconds = 0f;

    public void advance(float amount) {
        elapsedSeconds += amount;
    }

    public void render(TreeViewport viewport, float alphaMult) {
        if (alphaMult <= 0f) return;
        float zoom = viewport.zoom();
        List<RingBelt> ringBelts = SkillTree.getRingBelts();
        if (ringBelts.isEmpty()) return;

        if (bandByBelt.length != ringBelts.size()) {
            bandByBelt = new RadialBand[ringBelts.size()];
        }
        for (int beltIndex = 0; beltIndex < ringBelts.size(); beltIndex++) {
            RingBelt belt = ringBelts.get(beltIndex);
            String ringArtPath = belt.getRingArtPath();
            SpriteAPI ringSprite = ringArtPath == null || ringArtPath.isEmpty() ? null : spriteCache.texture(ringArtPath);
            if (ringSprite == null) continue;

            float rotationDeg = belt.getRotation() + belt.getRotationSpeed() * elapsedSeconds;
            RadialBand band = screenBand(beltIndex, viewport.screenX(belt.getX()), viewport.screenY(belt.getY()),
                    belt.getInnerRadius() * zoom, belt.getOuterRadius() * zoom);
            RingBeltRenderer.render(ringSprite, band, Color.WHITE, alphaMult, rotationDeg, viewport);
        }
    }

    private RadialBand screenBand(int beltIndex, float screenX, float screenY, float innerRadius, float outerRadius) {
        RadialBand cached = bandByBelt[beltIndex];
        boolean unchanged = cached != null && cached.center().x == screenX && cached.center().y == screenY
                && cached.innerRadius() == innerRadius && cached.outerRadius() == outerRadius;
        if (!unchanged) {
            cached = new RadialBand(new Vector2f(screenX, screenY), innerRadius, outerRadius);
            bandByBelt[beltIndex] = cached;
        }
        return cached;
    }
}
