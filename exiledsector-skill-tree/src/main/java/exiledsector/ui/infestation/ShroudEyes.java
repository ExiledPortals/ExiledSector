package exiledsector.ui.infestation;

import exiledsector.ui.infestation.InfestedGeometry.InfestedNode;

import java.util.ArrayList;
import java.util.List;

import static exiledsector.ui.infestation.InfestationHash.TWO_PI;
import static exiledsector.ui.infestation.InfestationHash.between;
import static exiledsector.ui.infestation.InfestationHash.unit;

final class ShroudEyes {

    static final int LARGE_NODE_EYES = 3;
    static final int SMALL_NODE_EYES = 1;
    static final float SECOND_SMALL_EYE_CHANCE = 0.35f;
    static final float OPENING_START = 0.1f;
    static final float OPENING_SHARE = 0.12f;
    static final float CLOSING_END = 0.75f;
    static final float CLOSING_SHARE = 0.08f;
    private static final float EYE_DISTANCE_SHARE = 1f;
    private static final float MIN_EYE_SIZE_SHARE = 0.42f;
    private static final float MAX_EYE_SIZE_SHARE = 0.58f;
    private static final float EYE_HEIGHT_SHARE = 0.82f;
    private static final float SOCKET_SIZE_MULT = 1.4f;
    private static final float SOCKET_ALPHA = 0.8f;
    private static final float MIN_CYCLE_SECONDS = 4f;
    private static final float EXTRA_CYCLE_SECONDS = 5f;
    private static final float CREEP_RAD_PER_SECOND = 0.04f;
    private static final float GLOW_ALPHA = 0.55f;
    private static final float GLOW_PULSE = 0.35f;
    private static final float GLOW_PULSE_SECONDS = 1.7f;
    private static final int SALT = 0x3E7E;

    record Eye(InfestedNode node, int eyeKey, float baseAngle, float creepDirection, float sizeShare, float cycleSeconds, float cycleOffset) {
    }

    @FunctionalInterface
    interface EyeSink {
        void eye(float worldX, float worldY, float worldWidth, float worldHeight, float angleDeg, float alpha);
    }

    private final List<Eye> eyes;

    ShroudEyes(InfestedGeometry geometry) {
        this.eyes = layout(geometry);
    }

    static float openness(float cycleShare) {
        float opening = (cycleShare - OPENING_START) / OPENING_SHARE;
        float closing = (CLOSING_END - cycleShare) / CLOSING_SHARE;
        return InfestationHash.clamp(Math.min(opening, closing), 0f, 1f);
    }

    List<Eye> eyes() {
        return eyes;
    }

    void emitSockets(PuffSink sink, float atSeconds) {
        for (Eye eye : eyes) {
            float openness = opennessAt(eye, atSeconds);
            if (openness <= 0f) {
                continue;
            }
            float angle = angleAt(eye, atSeconds);
            float visualRadius = eye.node().visualRadius();
            float eyeX = eye.node().worldX() + (float) Math.cos(angle) * EYE_DISTANCE_SHARE * visualRadius;
            float eyeY = eye.node().worldY() + (float) Math.sin(angle) * EYE_DISTANCE_SHARE * visualRadius;
            sink.puff(eyeX, eyeY, eye.sizeShare() * visualRadius * SOCKET_SIZE_MULT, -(float) Math.toDegrees(angle),
                    ShroudPuffs.cell(eye.eyeKey(), 0), ShroudPuffs.DARKNESS, SOCKET_ALPHA * Math.min(1f, openness * 2f + 0.3f));
        }
    }

    void emitGlows(EyeSink sink, float atSeconds) {
        for (Eye eye : eyes) {
            float openness = opennessAt(eye, atSeconds);
            if (openness <= 0f) {
                continue;
            }
            float angle = angleAt(eye, atSeconds);
            float visualRadius = eye.node().visualRadius();
            float eyeX = eye.node().worldX() + (float) Math.cos(angle) * EYE_DISTANCE_SHARE * visualRadius;
            float eyeY = eye.node().worldY() + (float) Math.sin(angle) * EYE_DISTANCE_SHARE * visualRadius;
            float eyeWidth = eye.sizeShare() * visualRadius;
            float pulse = GLOW_ALPHA + GLOW_PULSE * (float) Math.sin(TWO_PI * atSeconds / GLOW_PULSE_SECONDS + eye.cycleOffset() * TWO_PI);
            sink.eye(eyeX, eyeY, eyeWidth, eyeWidth * EYE_HEIGHT_SHARE * openness, -(float) Math.toDegrees(angle) + 90f, pulse * openness);
        }
    }

    private static float opennessAt(Eye eye, float atSeconds) {
        return openness(InfestationHash.fraction(atSeconds / eye.cycleSeconds() + eye.cycleOffset()));
    }

    private static float angleAt(Eye eye, float atSeconds) {
        return eye.baseAngle() + eye.creepDirection() * CREEP_RAD_PER_SECOND * atSeconds;
    }

    private static List<Eye> layout(InfestedGeometry geometry) {
        List<Eye> laidOut = new ArrayList<>();
        for (InfestedNode node : geometry.nodes()) {
            int seed = node.nodeSeed();
            int count = node.large() ? LARGE_NODE_EYES : SMALL_NODE_EYES + (unit(seed, 0, SALT) < SECOND_SMALL_EYE_CHANCE ? 1 : 0);
            float firstAngle = TWO_PI * unit(seed, 0, SALT + 1);
            for (int index = 0; index < count; index++) {
                int eyeKey = seed * 17 + index;
                float spread = TWO_PI * index / count + (unit(eyeKey, 0, SALT + 2) - 0.5f) * 0.8f;
                laidOut.add(new Eye(node, eyeKey, firstAngle + spread,
                        InfestationHash.direction(unit(eyeKey, 0, SALT + 3)),
                        between(MIN_EYE_SIZE_SHARE, MAX_EYE_SIZE_SHARE, unit(eyeKey, 0, SALT + 4)),
                        MIN_CYCLE_SECONDS + EXTRA_CYCLE_SECONDS * unit(eyeKey, 0, SALT + 5),
                        unit(eyeKey, 0, SALT + 6)));
            }
        }
        return List.copyOf(laidOut);
    }
}
