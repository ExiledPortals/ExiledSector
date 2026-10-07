package exiledsector.ui.decoration;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.layout.Star;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SkillTreeStarfieldRenderer {

    private static final String[] STAR_SPRITE_PATHS = {
            "graphics/backgrounds/star0.png",
            "graphics/backgrounds/star1.png",
            "graphics/backgrounds/star2.png",
            "graphics/backgrounds/star3.png"
    };

    private static final float REFERENCE_WIDTH = 1920f;
    private static final float REFERENCE_HEIGHT = 1080f;

    private static final class LayerSpec {
        final int baseCount;
        final float parallaxFactor;
        final float baseSize;
        final float baseAlpha;

        LayerSpec(int baseCount, float parallaxFactor, float baseSize, float baseAlpha) {
            this.baseCount = baseCount;
            this.parallaxFactor = parallaxFactor;
            this.baseSize = baseSize;
            this.baseAlpha = baseAlpha;
        }
    }

    private static final LayerSpec[] LAYER_SPECS = {
            new LayerSpec(70, 0.15f, 8f, 0.45f),
            new LayerSpec(45, 0.3f, 12f, 0.65f),
            new LayerSpec(25, 0.5f, 18f, 0.9f)
    };

    private static final Color STANDARD_STAR_COLOR = Color.WHITE;

    private static final class Star {
        float baseX;
        float baseY;
        int spriteIndex;
        float spriteSize;
        float baseAlpha;
        float twinkleSpeed;
        float twinklePhase;
        boolean useAccentColor;
    }

    private final SkillTreePanelStyle panelStyle;
    private final SpriteCache spriteCache = new SpriteCache(SkillTreeStarfieldRenderer.class);
    private final Random random = new Random();

    private boolean initialized = false;
    private float fieldWidth;
    private float fieldHeight;
    private List<Star>[] starLayers;
    private float twinkleElapsedSeconds = 0f;

    public SkillTreeStarfieldRenderer(SkillTreePanelStyle panelStyle) {
        this.panelStyle = panelStyle;
    }

    public void advance(float amount) {
        twinkleElapsedSeconds += amount;
    }

    public void render(PositionAPI panelPosition, float panX, float panY, float alphaMult) {
        if (panelPosition == null) return;

        if (!initialized) {
            initStars(panelPosition.getWidth(), panelPosition.getHeight());
        }

        float panelCenterX = panelPosition.getX() + panelPosition.getWidth() / 2f;
        float panelCenterY = panelPosition.getY() + panelPosition.getHeight() / 2f;
        Color accentColor = panelStyle.getAccentColor();

        for (int i = 0; i < LAYER_SPECS.length; i++) {
            LayerSpec layerSpec = LAYER_SPECS[i];
            float offsetX = panX * layerSpec.parallaxFactor;
            float offsetY = panY * layerSpec.parallaxFactor;

            for (Star star : starLayers[i]) {
                float wrappedX = wrap(star.baseX + offsetX, fieldWidth);
                float wrappedY = wrap(star.baseY + offsetY, fieldHeight);
                float screenX = panelCenterX + wrappedX - fieldWidth / 2f;
                float screenY = panelCenterY + wrappedY - fieldHeight / 2f;

                float twinkle = 0.6f + 0.4f * (float) Math.sin(twinkleElapsedSeconds * star.twinkleSpeed + star.twinklePhase);

                SpriteDraw.drawAtCenter(spriteCache, STAR_SPRITE_PATHS[star.spriteIndex], screenX, screenY,
                        star.spriteSize, star.spriteSize, star.useAccentColor ? accentColor : STANDARD_STAR_COLOR,
                        alphaMult * star.baseAlpha * twinkle);
            }
        }
    }

    // generic array creation isn't allowed directly; the raw List[] is only ever populated with List<Star>
    @SuppressWarnings("unchecked")
    private void initStars(float panelWidth, float panelHeight) {
        fieldWidth = panelWidth * 1.2f;
        fieldHeight = panelHeight * 1.2f;
        float areaScale = (fieldWidth * fieldHeight) / (REFERENCE_WIDTH * REFERENCE_HEIGHT);

        starLayers = new List[LAYER_SPECS.length];
        for (int i = 0; i < LAYER_SPECS.length; i++) {
            LayerSpec layerSpec = LAYER_SPECS[i];
            int starCount = Math.max(4, Math.round(layerSpec.baseCount * areaScale));
            List<Star> stars = new ArrayList<>(starCount);
            for (int j = 0; j < starCount; j++) {
                Star star = new Star();
                star.baseX = random.nextFloat() * fieldWidth;
                star.baseY = random.nextFloat() * fieldHeight;
                star.spriteIndex = random.nextInt(STAR_SPRITE_PATHS.length);
                star.spriteSize = layerSpec.baseSize * (0.75f + random.nextFloat() * 0.5f);
                star.baseAlpha = layerSpec.baseAlpha * (0.8f + random.nextFloat() * 0.2f);
                star.twinkleSpeed = 0.5f + random.nextFloat();
                star.twinklePhase = random.nextFloat() * (float) (Math.PI * 2);
                star.useAccentColor = random.nextBoolean();
                stars.add(star);
            }
            starLayers[i] = stars;
        }
        initialized = true;
    }

    private static float wrap(float value, float size) {
        float wrapped = value % size;
        if (wrapped < 0) wrapped += size;
        return wrapped;
    }
}
