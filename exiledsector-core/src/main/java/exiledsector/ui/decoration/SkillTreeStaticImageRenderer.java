package exiledsector.ui.decoration;

import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.skills.decoration.StaticImage;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.SpriteDraw;

import java.util.List;

public class SkillTreeStaticImageRenderer implements DecorationRenderer {

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeStaticImageRenderer.class);
    private final List<StaticImage> images;
    private float elapsedSeconds = 0f;

    SkillTreeStaticImageRenderer(List<StaticImage> images) {
        this.images = List.copyOf(images);
    }

    @Override
    public void advance(float amount) {
        elapsedSeconds += amount;
    }

    @Override
    public void render(DecorationScene scene, DecorationDepth depth) {
        if (depth == DecorationDepth.BEHIND_STARS) {
            render(scene, true);
        } else if (depth == DecorationDepth.FOREGROUND_IMAGES) {
            render(scene, false);
        }
    }

    private void render(DecorationScene scene, boolean behindStars) {
        if (images.isEmpty()) return;
        TreeViewport viewport = scene.viewport();
        float zoom = viewport.zoom();
        float viewCenterX = (viewport.left() + viewport.right()) / 2f;
        float viewCenterY = (viewport.bottom() + viewport.top()) / 2f;
        boolean viewBounded = Float.isFinite(viewCenterX) && Float.isFinite(viewCenterY);

        for (StaticImage image : images) {
            String imagePath = image.getImagePath();
            if (imagePath == null || imagePath.isEmpty() || image.isBehindStars() != behindStars) continue;
            float imageAlpha = scene.alphaFor(image.getId());
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
