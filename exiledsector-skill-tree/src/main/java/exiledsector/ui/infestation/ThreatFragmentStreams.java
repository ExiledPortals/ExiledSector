package exiledsector.ui.infestation;

import exiledsector.ui.infestation.InfestedGeometry.InfestedLink;

import java.util.ArrayList;
import java.util.List;

import static exiledsector.ui.infestation.InfestationHash.TWO_PI;
import static exiledsector.ui.infestation.InfestationHash.between;
import static exiledsector.ui.infestation.InfestationHash.fraction;
import static exiledsector.ui.infestation.InfestationHash.unit;

final class ThreatFragmentStreams {

    static final float WORLD_LENGTH_PER_FRAGMENT = 34f;
    static final int FEWEST_FRAGMENTS_PER_LINK = 3;
    static final int MOST_FRAGMENTS_PER_LINK = 12;
    private static final float MIN_SPEED_WORLD_PER_SECOND = 90f;
    private static final float MAX_SPEED_WORLD_PER_SECOND = 140f;
    private static final float WIGGLE_WORLD = 6f;
    private static final float WIGGLE_WAVES = 2f;
    private static final float WIGGLE_RATE = 3f;
    private static final float FLICKER_RATE = 17f;
    private static final float DROPOUT_SLOTS_PER_SECOND = 6f;
    private static final float DROPOUT_CHANCE = 0.15f;
    private static final float DROPOUT_BRIGHTNESS = 0.15f;
    private static final float MIN_FRAGMENT_WORLD_SIZE = 6f;
    private static final float MAX_FRAGMENT_WORLD_SIZE = 9f;
    private static final int SALT = 0x3C11;

    record Stream(InfestedLink link, float startShare, float endShare, float speedWorldPerSecond, float wigglePhase, int fragmentCount,
                  float fragmentWorldSize) {
    }

    private final List<Stream> streams;

    ThreatFragmentStreams(InfestedGeometry geometry) {
        this.streams = layout(geometry);
    }

    static int fragmentCount(float worldLength) {
        return InfestationHash.scaledCount(worldLength / WORLD_LENGTH_PER_FRAGMENT, FEWEST_FRAGMENTS_PER_LINK, MOST_FRAGMENTS_PER_LINK);
    }

    void emit(FragmentSink sink, float atSeconds) {
        for (Stream stream : streams) {
            InfestedLink link = stream.link();
            float span = stream.endShare() - stream.startShare();
            if (span <= 0f) {
                continue;
            }
            float directionX = link.directionX();
            float directionY = link.directionY();
            float travelShare = atSeconds * stream.speedWorldPerSecond() / Math.max(1f, link.worldLength() * span);
            for (int index = 0; index < stream.fragmentCount(); index++) {
                float progress = fraction(index / (float) stream.fragmentCount() + travelShare + unit(link.linkSeed(), index, SALT) * 0.3f);
                float alongShare = stream.startShare() + span * progress;
                float envelope = (float) Math.sin(Math.PI * progress);
                float wiggle = WIGGLE_WORLD * envelope
                        * (float) Math.sin(alongShare * TWO_PI * WIGGLE_WAVES + atSeconds * WIGGLE_RATE + stream.wigglePhase());
                float along = alongShare * link.worldLength();
                float fragmentX = link.from().worldX() + directionX * along - directionY * wiggle;
                float fragmentY = link.from().worldY() + directionY * along + directionX * wiggle;
                float brightness = envelope * flicker(link.linkSeed(), index, atSeconds);
                if (brightness > 0f) {
                    sink.fragment(fragmentX, fragmentY, stream.fragmentWorldSize(), atSeconds * 200f + index * 47f, brightness);
                }
            }
        }
    }

    static float flicker(int linkSeed, int index, float atSeconds) {
        int dropoutSlot = (int) Math.floor(atSeconds * DROPOUT_SLOTS_PER_SECOND);
        if (unit(linkSeed + index, dropoutSlot, SALT + 1) < DROPOUT_CHANCE) {
            return DROPOUT_BRIGHTNESS;
        }
        float flicker = (float) Math.sin(atSeconds * FLICKER_RATE + index * 2.3f + linkSeed);
        return 0.55f + 0.45f * Math.max(0f, flicker);
    }

    private static List<Stream> layout(InfestedGeometry geometry) {
        List<Stream> laidOut = new ArrayList<>(geometry.links().size());
        for (InfestedLink link : geometry.links()) {
            float length = link.worldLength();
            if (length <= 0f) {
                continue;
            }
            float startShare = Math.min(0.45f, link.from().visualRadius() / length);
            float endShare = Math.max(0.55f, 1f - link.to().visualRadius() / length);
            int seed = link.linkSeed();
            laidOut.add(new Stream(link, startShare, endShare,
                    between(MIN_SPEED_WORLD_PER_SECOND, MAX_SPEED_WORLD_PER_SECOND, unit(seed, 0, SALT + 2)),
                    TWO_PI * unit(seed, 0, SALT + 3),
                    fragmentCount(length * (endShare - startShare)),
                    between(MIN_FRAGMENT_WORLD_SIZE, MAX_FRAGMENT_WORLD_SIZE, unit(seed, 0, SALT + 4))));
        }
        return List.copyOf(laidOut);
    }
}
