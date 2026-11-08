package exiledsector.skills.skilleffect;

public final class ParameterBonus {

    private ParameterBonus() {
    }

    public static float plusFlat(float base, float flatBonus) {
        return Math.max(0f, base + flatBonus);
    }

    public static float increasedBy(float base, float increasePercent) {
        return base * Math.max(0f, 1f + increasePercent / 100f);
    }
}
