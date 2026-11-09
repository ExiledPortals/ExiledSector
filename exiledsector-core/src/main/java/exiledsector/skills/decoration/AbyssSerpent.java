package exiledsector.skills.decoration;

import exiledsector.skills.layout.TreeTags;

public class AbyssSerpent extends Decoration {

    private final Orbit orbit;
    private final Wobble wobble;

    public record Wobble(float share, int waves, float phaseDegrees) {
        public static final Wobble NONE = new Wobble(0f, 0, 0f);
    }

    public AbyssSerpent(String id, float x, float y, Orbit orbit, Wobble wobble, TreeTags tags) {
        super(id, x, y, tags);
        this.orbit = orbit;
        this.wobble = wobble;
    }

    @Override
    public DecorationKind kind() {
        return DecorationKind.SERPENT;
    }

    public Orbit getOrbit() {
        return orbit;
    }

    public Wobble getWobble() {
        return wobble;
    }
}
