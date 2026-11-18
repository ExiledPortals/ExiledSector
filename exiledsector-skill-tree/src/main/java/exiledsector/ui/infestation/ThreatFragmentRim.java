package exiledsector.ui.infestation;

import static exiledsector.ui.infestation.InfestationHash.TWO_PI;
import static exiledsector.ui.infestation.InfestationHash.unit;

final class ThreatFragmentRim {

    static final float WORLD_SPACING = 24f;
    static final int FEWEST_FRAGMENTS = 48;
    static final int MOST_FRAGMENTS = 260;
    private static final float MARCH_WORLD_PER_SECOND = 20f;
    private static final float RIM_WOBBLE_SHARE = 0.006f;
    private static final float COLUMNS_AROUND_RIM = 24f;
    private static final float COLUMN_MARCH_RATE = 1.5f;
    private static final float DIMMEST_BRIGHTNESS = 0.3f;
    private static final float FRAGMENT_WORLD_SIZE = 8f;
    private static final int SALT = 0x2B9D;

    private final InfestedGeometry geometry;
    private final int fragmentCount;
    private final float marchDirection;

    ThreatFragmentRim(InfestedGeometry geometry) {
        this.geometry = geometry;
        this.fragmentCount = fragmentCount(geometry.worldRadius());
        this.marchDirection = InfestationHash.direction(unit(geometry.sceneSeed(), 0, SALT));
    }

    static int fragmentCount(float worldRadius) {
        return InfestationHash.scaledCount(TWO_PI * worldRadius / WORLD_SPACING, FEWEST_FRAGMENTS, MOST_FRAGMENTS);
    }

    void emit(FragmentSink sink, float atSeconds) {
        float worldRadius = geometry.worldRadius();
        if (worldRadius <= 0f) {
            return;
        }
        float marchAngle = marchDirection * atSeconds * MARCH_WORLD_PER_SECOND / worldRadius;
        for (int index = 0; index < fragmentCount; index++) {
            float angle = TWO_PI * index / fragmentCount + marchAngle;
            float distance = worldRadius * (1f - RIM_WOBBLE_SHARE * (1f + (float) Math.sin(index * 1.7f + atSeconds * 2f)));
            float column = Math.max(0f, (float) Math.sin(angle * COLUMNS_AROUND_RIM - marchDirection * atSeconds * COLUMN_MARCH_RATE));
            float brightness = DIMMEST_BRIGHTNESS + (1f - DIMMEST_BRIGHTNESS) * (float) Math.sqrt(column);
            float fragmentX = geometry.socketX() + (float) Math.cos(angle) * distance;
            float fragmentY = geometry.socketY() + (float) Math.sin(angle) * distance;
            sink.fragment(fragmentX, fragmentY, FRAGMENT_WORLD_SIZE, -(float) Math.toDegrees(angle) + 90f, brightness);
        }
    }
}
