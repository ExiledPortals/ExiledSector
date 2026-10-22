package exiledsector.ui.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UnitCircle {

    private static final int CACHE_SIZE = 64;
    private static final Map<Integer, UnitCircle> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, UnitCircle> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private final float[] cos;
    private final float[] sin;

    private UnitCircle(int segments) {
        cos = new float[segments];
        sin = new float[segments];
        for (int i = 0; i < segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            cos[i] = (float) Math.cos(angle);
            sin[i] = (float) Math.sin(angle);
        }
    }

    public static UnitCircle of(int segments) {
        return CACHE.computeIfAbsent(Math.max(1, segments), UnitCircle::new);
    }

    public int segments() {
        return cos.length;
    }

    public float cos(int index) {
        return cos[index];
    }

    public float sin(int index) {
        return sin[index];
    }
}
