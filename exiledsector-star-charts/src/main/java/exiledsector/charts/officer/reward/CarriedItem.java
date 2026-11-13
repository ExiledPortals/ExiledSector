package exiledsector.charts.officer.reward;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public record CarriedItem(RewardKind kind, String id, String variant, int baseCount) {

    public static final String SEPARATOR = ":";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final int PARTS_WITHOUT_VARIANT = 3;
    private static final int PARTS_WITH_VARIANT = 4;
    private static final int MAX_COUNT_DIGITS = 9;

    public CarriedItem {
        Objects.requireNonNull(kind, "kind");
        if (!isSafeId(id)) {
            throw new IllegalArgumentException("Carried item id must match " + SAFE_ID + ": " + id);
        }
        if (variant != null && !isSafeId(variant)) {
            throw new IllegalArgumentException("Carried item variant must match " + SAFE_ID + ": " + variant);
        }
        if (baseCount < 1) {
            throw new IllegalArgumentException("Carried item count must be at least 1: " + baseCount);
        }
    }

    public static CarriedItem of(RewardKind kind, String id) {
        return new CarriedItem(kind, id, null, 1);
    }

    public static boolean isSafeId(String text) {
        return text != null && SAFE_ID.matcher(text).matches();
    }

    public String toToken() {
        String token = kind.id() + SEPARATOR + baseCount + SEPARATOR + id;
        return variant == null ? token : token + SEPARATOR + variant;
    }

    public static CarriedItem fromToken(String token) {
        if (token == null) {
            return null;
        }
        String[] parts = token.split(SEPARATOR, -1);
        if (parts.length != PARTS_WITHOUT_VARIANT && parts.length != PARTS_WITH_VARIANT) {
            return null;
        }
        RewardKind kind = RewardKind.byId(parts[0]);
        Integer count = positiveCount(parts[1]);
        String variant = parts.length == PARTS_WITH_VARIANT ? parts[3] : null;
        if (kind == null || count == null || !isSafeId(parts[2]) || (variant != null && !isSafeId(variant))) {
            return null;
        }
        return new CarriedItem(kind, parts[2], variant, count);
    }

    private static Integer positiveCount(String text) {
        if (text.isEmpty() || text.length() > MAX_COUNT_DIGITS || !text.chars().allMatch(Character::isDigit)) {
            return null;
        }
        int count = Integer.parseInt(text);
        return count >= 1 ? count : null;
    }

    public CarriedItem withCount(int count) {
        return new CarriedItem(kind, id, variant, count);
    }

    public boolean sameItemAs(CarriedItem other) {
        return other != null && kind == other.kind && id.equals(other.id) && Objects.equals(variant, other.variant);
    }

    public int entries() {
        return kind.craftingItem() ? 1 : baseCount;
    }

    public static List<CarriedItem> joined(List<CarriedItem> carried, Collection<CarriedItem> added) {
        List<CarriedItem> joined = new ArrayList<>(carried);
        joined.addAll(added);
        return List.copyOf(joined);
    }
}
