package exiledsector.socketables;

import java.util.Objects;

public final class FrozenName {

    static final FrozenName NONE = new FrozenName(null, null, null, null);

    private final String prefixEffect;
    private final String suffixEffect;
    private final String rareFirst;
    private final String rareSecond;
    private final String rareBrand;
    private final String rareModel;
    private final boolean product;

    FrozenName(String prefixEffect, String suffixEffect, String rareFirst, String rareSecond) {
        this(prefixEffect, suffixEffect, rareFirst, rareSecond, null, null, false);
    }

    private FrozenName(String prefixEffect, String suffixEffect, String rareFirst, String rareSecond, String rareBrand, String rareModel,
                       boolean product) {
        this.prefixEffect = prefixEffect;
        this.suffixEffect = suffixEffect;
        this.rareFirst = rareFirst;
        this.rareSecond = rareSecond;
        this.rareBrand = rareBrand;
        this.rareModel = rareModel;
        this.product = product;
    }

    static FrozenName product(String firstWord, String brandWord, String secondWord, String modelWord) {
        return new FrozenName(null, null, firstWord, secondWord, brandWord, modelWord, true);
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

    String rareBrand() {
        return rareBrand;
    }

    String rareModel() {
        return rareModel;
    }

    boolean isProduct() {
        return product;
    }

    boolean isAssembledText() {
        return rareFirst != null && rareSecond == null;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FrozenName otherName && Objects.equals(prefixEffect, otherName.prefixEffect)
                && Objects.equals(suffixEffect, otherName.suffixEffect) && Objects.equals(rareFirst, otherName.rareFirst)
                && Objects.equals(rareSecond, otherName.rareSecond) && Objects.equals(rareBrand, otherName.rareBrand)
                && Objects.equals(rareModel, otherName.rareModel) && product == otherName.product;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefixEffect, suffixEffect, rareFirst, rareSecond, rareBrand, rareModel, product);
    }

    @Override
    public String toString() {
        return "FrozenName[" + prefixEffect + ", " + suffixEffect + ", " + rareFirst + ", " + rareSecond + ", " + rareBrand + ", "
                + rareModel + (product ? ", product" : "") + "]";
    }
}
