package exiledsector.items;

import java.util.EnumMap;
import java.util.Map;

public record ItemChances(float general, Map<ItemKind, Float> targeted) {

    public static final ItemChances BASE = new ItemChances(1f, Map.of());

    public ItemChances {
        general = Math.max(0f, general);
        targeted = targeted == null ? Map.of() : Map.copyOf(targeted);
    }

    public static ItemChances of(float general) {
        return general == 1f ? BASE : new ItemChances(general, Map.of());
    }

    public ItemChances scaled(float factor) {
        if (factor == 1f) {
            return this;
        }
        Map<ItemKind, Float> scaledTargeted = new EnumMap<>(ItemKind.class);
        targeted.forEach((kind, extraShare) -> scaledTargeted.put(kind, extraShare * factor));
        return new ItemChances(general * factor, scaledTargeted);
    }

    public float targeted(ItemKind kind) {
        Float extraShare = targeted.get(kind);
        return extraShare == null ? 0f : Math.max(0f, extraShare);
    }
}
