package exiledsector.ui.slipstream;

import exiledsector.ui.util.TexturedQuadBatch;

import java.awt.Color;
import java.util.Random;

final class BlipCloud {

    static final float SPREAD_SHARE = 0.6f;
    static final float MIN_BLIP_SECONDS = 0.5f;
    static final float MAX_BLIP_SECONDS = 1.5f;
    static final float FIRST_BLIP_SHARE = 0.5f;
    static final float BLIPS_PER_SECOND = 30f;

    private final float radius;
    private final int maxBlips;
    private final float[] offsetX;
    private final float[] offsetY;
    private final float[] age;
    private final float[] duration;
    private final float[] placed = new float[SlipstreamProjection.PLACED_LENGTH];
    private int count;
    private float spawnBudget;
    private boolean spawnedAny;

    BlipCloud(float radius, int maxBlips) {
        this.radius = radius;
        this.maxBlips = maxBlips;
        offsetX = new float[maxBlips];
        offsetY = new float[maxBlips];
        age = new float[maxBlips];
        duration = new float[maxBlips];
    }

    int count() {
        return count;
    }

    void advance(float amount, boolean spawning, Random random) {
        for (int i = count - 1; i >= 0; i--) {
            age[i] += amount;
            if (age[i] >= duration[i]) {
                remove(i);
            }
        }
        if (!spawning) {
            spawnBudget = 0f;
            return;
        }
        spawnBudget = Math.min(maxBlips, spawnBudget + amount * BLIPS_PER_SECOND);
        while (spawnBudget >= 1f && count < maxBlips) {
            spawnBudget -= 1f;
            spawn(random);
        }
    }

    void draw(TexturedQuadBatch quads, SlipstreamProjection projection, float centerX, float centerY, float blipPixels, Color colour, float alphaMult) {
        for (int i = 0; i < count; i++) {
            float half = duration[i] / 2f;
            float bounce = age[i] < half ? age[i] / half : (duration[i] - age[i]) / half;
            projection.place(centerX + offsetX[i], centerY + offsetY[i], placed);
            quads.quadAtCenter(placed[SlipstreamProjection.X], placed[SlipstreamProjection.Y], blipPixels, blipPixels, 0f,
                    colour, Math.max(0f, Math.min(1f, bounce)) * alphaMult);
        }
    }

    private void spawn(Random random) {
        int i = count++;
        double angle = random.nextDouble() * Math.PI * 2.0;
        float reach = radius * SPREAD_SHARE * (float) Math.sqrt(random.nextFloat());
        offsetX[i] = (float) Math.cos(angle) * reach;
        offsetY[i] = (float) Math.sin(angle) * reach;
        age[i] = 0f;
        duration[i] = MIN_BLIP_SECONDS + random.nextFloat() * (MAX_BLIP_SECONDS - MIN_BLIP_SECONDS);
        if (!spawnedAny) {
            duration[i] *= FIRST_BLIP_SHARE;
            spawnedAny = true;
        }
    }

    private void remove(int i) {
        int last = --count;
        offsetX[i] = offsetX[last];
        offsetY[i] = offsetY[last];
        age[i] = age[last];
        duration[i] = duration[last];
    }
}
