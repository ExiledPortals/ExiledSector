package exiledsector.charts.atlas;

public final class AtlasNoise {

    private AtlasNoise() {
    }

    public static float twoOctave(long seed, long salt, long octaveSalt, float octaveShare, float x, float y) {
        return (valueNoise(seed, salt, x, y) + octaveShare * valueNoise(seed, salt ^ octaveSalt, x * 2f, y * 2f)) / (1f + octaveShare);
    }

    static float valueNoise(long seed, long salt, float x, float y) {
        int cornerX = (int) Math.floor(x);
        int cornerY = (int) Math.floor(y);
        float fractionX = smooth(x - cornerX);
        float fractionY = smooth(y - cornerY);
        float bottom = lerp(corner(seed, salt, cornerX, cornerY), corner(seed, salt, cornerX + 1, cornerY), fractionX);
        float top = lerp(corner(seed, salt, cornerX, cornerY + 1), corner(seed, salt, cornerX + 1, cornerY + 1), fractionX);
        return lerp(bottom, top, fractionY);
    }

    private static float corner(long seed, long salt, int x, int y) {
        return (float) AtlasHash.unit(AtlasHash.of(seed, salt, x, y));
    }

    private static float smooth(float fraction) {
        return fraction * fraction * (3f - 2f * fraction);
    }

    private static float lerp(float from, float to, float fraction) {
        return from + (to - from) * fraction;
    }
}
