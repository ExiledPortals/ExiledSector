package exiledsector.socketables;

import java.util.Objects;

public final class FrozenName {

    static final FrozenName NONE = new FrozenName(null, null, null, null);

    private final String prefixEffect;
    private final String suffixEffect;
    private final String rareFirst;
    private final String rareSecond;

    FrozenName(String prefixEffect, String suffixEffect, String rareFirst, String rareSecond) {
        this.prefixEffect = prefixEffect;
        this.suffixEffect = suffixEffect;
        this.rareFirst = rareFirst;
        this.rareSecond = rareSecond;
    }

    String prefixEffect() {
        return prefixEffect;
    }

    String suffixEffect() {
        return suffixEffect;
    }

    String rareFirst() {
        return rareFirst;
    }

    String rareSecond() {
        return rareSecond;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FrozenName name && Objects.equals(prefixEffect, name.prefixEffect)
                && Objects.equals(suffixEffect, name.suffixEffect) && Objects.equals(rareFirst, name.rareFirst)
                && Objects.equals(rareSecond, name.rareSecond);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefixEffect, suffixEffect, rareFirst, rareSecond);
    }

    @Override
    public String toString() {
        return "FrozenName[" + prefixEffect + ", " + suffixEffect + ", " + rareFirst + ", " + rareSecond + "]";
    }
}
