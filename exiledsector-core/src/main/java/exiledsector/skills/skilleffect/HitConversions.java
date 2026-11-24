package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.DamageType;

import java.util.ArrayList;
import java.util.List;

final class HitConversions {

    static final int EMP = DamageType.values().length;
    static final int PORTIONS = EMP + 1;

    private record Request(int from, int to, float share, boolean extra) {
    }

    private final List<Request> requests = new ArrayList<>();

    static int portionOf(DamageType type) {
        return type.ordinal();
    }

    void convert(int from, int to, float share) {
        if (from != to && share > 0f) {
            requests.add(new Request(from, to, Math.min(1f, share), false));
        }
    }

    void gain(int from, int to, float share) {
        if (share > 0f) {
            requests.add(new Request(from, to, share, true));
        }
    }

    boolean isEmpty() {
        return requests.isEmpty();
    }

    float[] applyTo(float[] original) {
        float[] converted = original.clone();
        for (int from = 0; from < PORTIONS; from++) {
            float amount = original[from];
            if (amount > 0f) {
                spread(from, amount, converted);
            }
        }
        return converted;
    }

    private void spread(int from, float amount, float[] converted) {
        float claimed = 0f;
        for (Request request : requests) {
            if (request.from() == from && !request.extra()) {
                claimed += request.share();
            }
        }
        float claimedScale = claimed > 1f ? 1f / claimed : 1f;
        for (Request request : requests) {
            if (request.from() != from) {
                continue;
            }
            float moved = amount * request.share() * (request.extra() ? 1f : claimedScale);
            if (!request.extra()) {
                converted[from] = Math.max(0f, converted[from] - moved);
            }
            converted[request.to()] += moved;
        }
    }
}
