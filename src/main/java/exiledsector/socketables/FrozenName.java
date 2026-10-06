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

    static FrozenName product(String first, String brand, String second, String model) {
        return new FrozenName(null, null, first, second, brand, model, true);
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
        return other instanceof FrozenName name && Objects.equals(prefixEffect, name.prefixEffect)
                && Objects.equals(suffixEffect, name.suffixEffect) && Objects.equals(rareFirst, name.rareFirst)
                && Objects.equals(rareSecond, name.rareSecond) && Objects.equals(rareBrand, name.rareBrand)
                && Objects.equals(rareModel, name.rareModel) && product == name.product;
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
