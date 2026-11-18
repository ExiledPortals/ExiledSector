package exiledsector.ui.infestation;

import exiledsector.ui.infestation.InfestedGeometry.InfestedNode;

import java.util.ArrayList;
import java.util.List;

import static exiledsector.ui.infestation.InfestationHash.TWO_PI;
import static exiledsector.ui.infestation.InfestationHash.between;
import static exiledsector.ui.infestation.InfestationHash.unit;

final class ThreatFabricationPulse {

    static final float PULSE_PERIOD_SECONDS = 7f;
    static final float WAVE_SECONDS = 3.2f;
    static final float SPARK_SECONDS = 1.6f;
    static final float NODE_FLARE_SECONDS = 1.1f;
    static final float WORLD_RADIUS_PER_SPARK = 14f;
    static final int FEWEST_SPARKS = 30;
    static final int MOST_SPARKS = 90;
    static final float FARTHEST_SPARK_SHARE = 0.95f;
    private static final float SPARK_GROW_RATE = 4f;
    private static final float SPARK_DRIFT_WORLD_PER_SECOND = 12f;
    private static final float MIN_SPARK_WORLD_SIZE = 8f;
    private static final float MAX_SPARK_WORLD_SIZE = 13f;
    private static final float SPARK_GLOW_SIZE_MULT = 2.2f;
    private static final float NODE_FLARE_RADII = 3f;
    private static final float NODE_FLARE_BRIGHTNESS = 0.9f;
    private static final int SALT = 0x7A85;

    record Spark(float distanceShare, float angleCos, float angleSin, float worldSize, float spinDeg) {
    }

    private final InfestedGeometry geometry;
    private final List<Spark> sparks;

    ThreatFabricationPulse(InfestedGeometry geometry) {
        this.geometry = geometry;
        this.sparks = layout(geometry);
    }

    static float pulseSeconds(float atSeconds) {
        return InfestationHash.fraction(atSeconds / PULSE_PERIOD_SECONDS) * PULSE_PERIOD_SECONDS;
    }

    static float frontShare(float pulseSeconds) {
        float progress = Math.min(1f, pulseSeconds / WAVE_SECONDS);
        float remaining = 1f - progress;
        return 1f - remaining * remaining;
    }

    static float arrivalSeconds(float distanceShare) {
        float clamped = Math.max(0f, Math.min(1f, distanceShare));
        return (1f - (float) Math.sqrt(1f - clamped)) * WAVE_SECONDS;
    }

    static float waveAlpha(float pulseSeconds) {
        if (pulseSeconds >= WAVE_SECONDS) {
            return 0f;
        }
        return (float) Math.pow(1f - pulseSeconds / WAVE_SECONDS, 0.7);
    }

    float frontWorldRadius(float atSeconds) {
        return frontShare(pulseSeconds(atSeconds)) * geometry.worldRadius();
    }

    void emitSparks(FragmentSink sink, float atSeconds) {
        float pulseSeconds = pulseSeconds(atSeconds);
        float worldRadius = geometry.worldRadius();
        for (Spark spark : sparks) {
            float sinceArrival = pulseSeconds - arrivalSeconds(spark.distanceShare());
            if (sinceArrival < 0f || sinceArrival >= SPARK_SECONDS) {
                continue;
            }
            float brightness = 1f - sinceArrival / SPARK_SECONDS;
            float grown = Math.min(1f, sinceArrival * SPARK_GROW_RATE);
            float distance = spark.distanceShare() * worldRadius + sinceArrival * SPARK_DRIFT_WORLD_PER_SECOND;
            float sparkX = geometry.socketX() + spark.angleCos() * distance;
            float sparkY = geometry.socketY() + spark.angleSin() * distance;
            float size = spark.worldSize() * (0.4f + 0.6f * grown);
            sink.glow(sparkX, sparkY, size * SPARK_GLOW_SIZE_MULT, brightness);
            sink.fragment(sparkX, sparkY, size, spark.spinDeg() + sinceArrival * 120f, brightness);
        }
    }

    void emitNodeFlares(FragmentSink sink, float atSeconds) {
        float pulseSeconds = pulseSeconds(atSeconds);
        float worldRadius = geometry.worldRadius();
        if (worldRadius <= 0f) {
            return;
        }
        for (InfestedNode node : geometry.nodes()) {
            float distanceShare = (float) Math.hypot(node.worldX() - geometry.socketX(), node.worldY() - geometry.socketY()) / worldRadius;
            float sinceArrival = pulseSeconds - arrivalSeconds(distanceShare);
            if (sinceArrival < 0f || sinceArrival >= NODE_FLARE_SECONDS) {
                continue;
            }
            float brightness = NODE_FLARE_BRIGHTNESS * (float) Math.sin(Math.PI * sinceArrival / NODE_FLARE_SECONDS);
            sink.glow(node.worldX(), node.worldY(), node.visualRadius() * NODE_FLARE_RADII, brightness);
        }
    }

    private static List<Spark> layout(InfestedGeometry geometry) {
        int count = InfestationHash.scaledCount(geometry.worldRadius() / WORLD_RADIUS_PER_SPARK, FEWEST_SPARKS, MOST_SPARKS);
        int seed = geometry.sceneSeed();
        List<Spark> laidOut = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            float angle = TWO_PI * unit(seed, index, SALT);
            laidOut.add(new Spark(
                    FARTHEST_SPARK_SHARE * (float) Math.sqrt(unit(seed, index, SALT + 1)),
                    (float) Math.cos(angle),
                    (float) Math.sin(angle),
                    between(MIN_SPARK_WORLD_SIZE, MAX_SPARK_WORLD_SIZE, unit(seed, index, SALT + 2)),
                    360f * unit(seed, index, SALT + 3)));
        }
        return List.copyOf(laidOut);
    }
}
