package exiledsector.ui.decoration;

import exiledsector.skills.SkillTree;
import exiledsector.skills.layout.StaticImage;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.util.List;
import java.util.Set;

public class SkillTreeStaticImageRenderer {

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeStaticImageRenderer.class);
    private float elapsedSeconds = 0f;

    public void advance(float amount) {
        elapsedSeconds += amount;
    }

    public void render(TreeViewport viewport, float alphaMult, Set<String> keptIds, float othersAlphaMult) {
        float zoom = viewport.zoom();
        List<StaticImage> images = SkillTree.getStaticImages();
        if (images.isEmpty()) return;

        for (StaticImage image : images) {
            String path = image.getImagePath();
            if (path == null || path.isEmpty()) continue;
            float alpha = keptIds.contains(image.getId()) ? alphaMult : alphaMult * othersAlphaMult;
            if (alpha <= 0f) continue;

            float screenX = viewport.screenX(image.getX());
            float screenY = viewport.screenY(image.getY());
            float width = image.getWidth() * zoom;
            float height = image.getHeight() * zoom;
            if (!viewport.isVisible(screenX, screenY, (float) Math.hypot(width, height) / 2f)) {
                continue;
            }
            float angleDeg = -(image.getRotation() + image.getRotationSpeed() * elapsedSeconds);
            SpriteDraw.drawAtCenter(spriteCache, path, screenX, screenY,
                    width, height, null, alpha, angleDeg);
        }
    }
}
