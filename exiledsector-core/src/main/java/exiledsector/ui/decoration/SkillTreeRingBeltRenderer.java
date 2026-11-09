package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.skills.decoration.RingBelt;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.belt.RingBeltRenderer;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

public class SkillTreeRingBeltRenderer implements DecorationRenderer {

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeRingBeltRenderer.class);
    private final List<RingBelt> ringBelts;
    private final RadialBand[] bandByBelt;
    private float elapsedSeconds = 0f;

    SkillTreeRingBeltRenderer(List<RingBelt> ringBelts) {
        this.ringBelts = List.copyOf(ringBelts);
        this.bandByBelt = new RadialBand[this.ringBelts.size()];
    }

    @Override
    public void advance(float amount) {
        elapsedSeconds += amount;
    }

    @Override
    public void render(DecorationScene scene, DecorationDepth depth) {
        float alphaMult = scene.sceneryAlpha() * scene.treeAlpha();
        if (depth != DecorationDepth.BELTS || alphaMult <= 0f || ringBelts.isEmpty()) return;
        TreeViewport viewport = scene.viewport();
        float zoom = viewport.zoom();
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
