package exiledsector.skills.decoration;

import exiledsector.skills.layout.TreeTags;

public class Hypershunt extends Decoration {

    private final Orbit orbit;
    private final float spriteSize;

    public Hypershunt(String id, float x, float y, Orbit orbit, float spriteSize, TreeTags tags) {
        super(id, x, y, tags);
        this.orbit = orbit;
        this.spriteSize = spriteSize;
    }

    @Override
    public DecorationKind kind() {
        return DecorationKind.HYPERSHUNT;
    }

    public Orbit getOrbit() {
        return orbit;
    }

    public float getSize() {
        return spriteSize;
    }
}
