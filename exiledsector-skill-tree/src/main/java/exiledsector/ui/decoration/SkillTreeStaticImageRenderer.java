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
            String imagePath = image.getImagePath();
            if (imagePath == null || imagePath.isEmpty()) continue;
            float imageAlpha = keptIds.contains(image.getId()) ? alphaMult : alphaMult * othersAlphaMult;
            if (imageAlpha <= 0f) continue;

            float screenX = viewport.screenX(image.getX());
            float screenY = viewport.screenY(image.getY());
            float imageWidth = image.getWidth() * zoom;
            float imageHeight = image.getHeight() * zoom;
            if (!viewport.isVisible(screenX, screenY, (float) Math.hypot(imageWidth, imageHeight) / 2f)) {
                continue;
            }
            float angleDeg = -(image.getRotation() + image.getRotationSpeed() * elapsedSeconds);
            SpriteDraw.drawAtCenter(spriteCache, imagePath, screenX, screenY,
                    imageWidth, imageHeight, null, imageAlpha, angleDeg);
        }
    }
}
