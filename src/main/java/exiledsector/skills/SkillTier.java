package exiledsector.skills;

public enum SkillTier {

    SMALL(1f),
    NOTABLE(1f),
    KEYSTONE(1f),
    WORMHOLE(1.3f),
    ROOT(2.5f),
    SOCKET(1.2f);

    private final float sizeMultiplier;

    SkillTier(float sizeMultiplier) {
        this.sizeMultiplier = sizeMultiplier;
    }

    public float getSizeMultiplier() {
        return sizeMultiplier;
    }
}
