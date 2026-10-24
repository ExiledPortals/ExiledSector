package exiledsector.ui.decoration;

import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.skills.SkillTree;
import exiledsector.skills.layout.StaticImage;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.SpriteDraw;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class SkillTreeStaticImageRenderer {

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeStaticImageRenderer.class);
    private float elapsedSeconds = 0f;

    public void advance(float amount) {
        elapsedSeconds += amount;
    }

    public void renderAmongStarfield(TreeViewport viewport, float alphaMult, Set<String> keptIds, float othersAlphaMult,
                                     float fromParallax, float toParallax) {
        render(viewport, alphaMult, keptIds, othersAlphaMult,
                image -> image.getParallax() >= fromParallax && image.getParallax() < toParallax);
    }

    public void render(TreeViewport viewport, float alphaMult, Set<String> keptIds, float othersAlphaMult,
                       boolean behindStars, float starfieldParallax) {
        render(viewport, alphaMult, keptIds, othersAlphaMult,
                image -> image.isBehindStars() == behindStars && image.getParallax() >= starfieldParallax);
    }

    private void render(TreeViewport viewport, float alphaMult, Set<String> keptIds, float othersAlphaMult,
                        Predicate<StaticImage> drawn) {
        float zoom = viewport.zoom();
        List<StaticImage> images = SkillTree.getStaticImages();
        if (images.isEmpty()) return;
        float viewCenterX = (viewport.left() + viewport.right()) / 2f;
        float viewCenterY = (viewport.bottom() + viewport.top()) / 2f;
        boolean viewBounded = Float.isFinite(viewCenterX) && Float.isFinite(viewCenterY);

        for (StaticImage image : images) {
            String imagePath = image.getImagePath();
            if (imagePath == null || imagePath.isEmpty() || !drawn.test(image)) continue;
            float imageAlpha = keptIds.contains(image.getId()) ? alphaMult : alphaMult * othersAlphaMult;
            if (imageAlpha <= 0f) continue;

            float screenX = viewport.screenX(image.getX());
            float screenY = viewport.screenY(image.getY());
            float imageWidth = image.getWidth() * zoom;
            float imageHeight = image.getHeight() * zoom;
            if (viewBounded) {
                float drift = ImageParallax.driftFraction((float) Math.hypot(viewCenterX - screenX, viewCenterY - screenY),
                        image.getParallax(), imageWidth * ImageParallax.REACH_PER_WIDTH);
                screenX += (viewCenterX - screenX) * drift;
                screenY += (viewCenterY - screenY) * drift;
            }
            if (!viewport.isVisible(screenX, screenY, (float) Math.hypot(imageWidth, imageHeight) / 2f)) {
                continue;
            }
            float angleDeg = -(image.getRotation() + image.getRotationSpeed() * elapsedSeconds);
            SpriteDraw.drawAtCenter(spriteCache, imagePath, screenX, screenY,
                    imageWidth, imageHeight, null, imageAlpha, angleDeg);
        }
    }
}
