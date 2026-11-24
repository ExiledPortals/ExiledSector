package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.DamageType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.ToDoubleFunction;

public final class TypeShares {

    private static final DamageType[] TYPES = DamageType.values();
    private static final TypeShares[] SINGLES = singles();
    private static final float NEGLIGIBLE_SHARE = 1e-6f;

    private final float[] sharesByOrdinal;

    private TypeShares(float[] sharesByOrdinal) {
        this.sharesByOrdinal = sharesByOrdinal;
    }

    public static TypeShares single(DamageType type) {
        return type == null ? SINGLES[DamageType.OTHER.ordinal()] : SINGLES[type.ordinal()];
    }

    static TypeShares ofAmounts(float[] amountsByPortion) {
        float total = 0f;
        for (DamageType type : TYPES) {
            total += Math.max(0f, amountsByPortion[type.ordinal()]);
        }
        if (total <= 0f) {
            return null;
        }
        float[] shares = new float[TYPES.length];
        for (DamageType type : TYPES) {
            shares[type.ordinal()] = Math.max(0f, amountsByPortion[type.ordinal()]) / total;
        }
        return new TypeShares(shares);
    }

    public float share(DamageType type) {
        return type == null ? 0f : sharesByOrdinal[type.ordinal()];
    }

    public boolean mixed() {
        int typesWithShare = 0;
        for (float share : sharesByOrdinal) {
            if (share > NEGLIGIBLE_SHARE) {
                typesWithShare++;
            }
        }
        return typesWithShare >= 2;
    }

    public Set<DamageType> types() {
        Set<DamageType> types = EnumSet.noneOf(DamageType.class);
        for (DamageType type : TYPES) {
            if (share(type) > NEGLIGIBLE_SHARE) {
                types.add(type);
            }
        }
        return types;
    }

    public DamageType largest() {
        DamageType largest = null;
        for (DamageType type : TYPES) {
            if (largest == null || share(type) > share(largest)) {
                largest = type;
            }
        }
        return largest;
    }

    public float weighted(ToDoubleFunction<DamageType> valueByType) {
        float total = 0f;
        for (DamageType type : TYPES) {
            float share = share(type);
            if (share > 0f) {
                total += share * (float) valueByType.applyAsDouble(type);
            }
        }
        return total;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TypeShares shares && Arrays.equals(sharesByOrdinal, shares.sharesByOrdinal);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(sharesByOrdinal);
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("TypeShares[");
        for (DamageType type : types()) {
            text.append(type).append('=').append(share(type)).append(' ');
        }
        return text.append(']').toString();
    }

    private static TypeShares[] singles() {
        TypeShares[] singles = new TypeShares[TYPES.length];
        for (DamageType type : TYPES) {
            float[] shares = new float[TYPES.length];
            shares[type.ordinal()] = 1f;
            singles[type.ordinal()] = new TypeShares(shares);
        }
        return singles;
    }
}
