package exiledsector.items;

import exiledsector.modifiers.Modifier;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class ModifierParts {

    private static final AtomicReference<Built> CACHED = new AtomicReference<>();

    private record Source(Collection<ItemDefinition> definitions, Collection<Modifier> catalogue, List<Modifier> corruptionPool,
                          Map<String, ModifierSources.Source> reservations, Map<String, List<Modifier>> prototypePools) {

        static Source current() {
            return new Source(ItemDefinitions.all(), ItemModifiers.all(), ItemImplicits.corruptionPool(), ModifierSources.all(),
                    PrototypeImplicits.byBase());
        }

        boolean sameAs(Source other) {
            return definitions == other.definitions && catalogue == other.catalogue && corruptionPool == other.corruptionPool
                    && reservations == other.reservations && prototypePools == other.prototypePools;
        }
    }

    public record Index(Map<String, Set<ItemKind>> partsByModifier, Map<ItemKind, List<Modifier>> modifiersByPart) {

        public Set<ItemKind> parts(String modifierId) {
            return modifierId == null ? Set.of() : partsByModifier.getOrDefault(modifierId, Set.of());
        }

        public List<Modifier> modifiers(ItemKind part) {
            return part == null ? List.of() : modifiersByPart.getOrDefault(part, List.of());
        }
    }

    private record Built(Source source, Index index) {
    }

    private ModifierParts() {
    }

    public static Set<ItemKind> parts(String modifierId) {
        return index().parts(modifierId);
    }

    public static List<Modifier> modifiers(ItemKind part) {
        return index().modifiers(part);
    }

    public static Index index() {
        Source source = Source.current();
        Built cached = CACHED.get();
        if (cached != null && cached.source().sameAs(source)) {
            return cached.index();
        }
        Index built = build(source);
        CACHED.set(new Built(source, built));
        return built;
    }

    private static Index build(Source source) {
        Map<ItemKind, Map<String, Modifier>> modifiersByPart = new EnumMap<>(ItemKind.class);
        for (ItemKind part : ItemKind.sectionItemTypes()) {
            modifiersByPart.put(part, new LinkedHashMap<>());
            ItemImplicits.forType(part).forEach(implicit -> add(modifiersByPart, part, implicit));
        }
        for (ItemDefinition definition : source.definitions()) {
            if (definition.kind() != null && definition.kind().fitsSection()) {
                definition.pool().forEach(entry -> add(modifiersByPart, definition.kind(), entry.modifier()));
            }
        }
        source.prototypePools().forEach((baseId, pool) -> {
            ItemDefinition base = ItemDefinitions.get(baseId);
            if (base != null && base.kind() != null && base.kind().fitsSection()) {
                pool.forEach(implicit -> add(modifiersByPart, base.kind(), implicit));
            }
        });
        for (Modifier implicit : source.corruptionPool()) {
            ItemKind.sectionItemTypes().forEach(part -> add(modifiersByPart, part, implicit));
        }
        source.reservations().forEach((modifierId, reservation) -> {
            Modifier modifier = ItemModifiers.get(modifierId);
            if (modifier != null) {
                reservation.parts().stream().filter(ItemKind::fitsSection).forEach(part -> add(modifiersByPart, part, modifier));
            }
        });
        Map<String, Set<ItemKind>> partsByModifier = new LinkedHashMap<>();
        Map<ItemKind, List<Modifier>> listedByPart = new EnumMap<>(ItemKind.class);
        modifiersByPart.forEach((part, modifiers) -> {
            listedByPart.put(part, List.copyOf(modifiers.values()));
            for (String modifierId : modifiers.keySet()) {
                partsByModifier.computeIfAbsent(modifierId, id -> EnumSet.noneOf(ItemKind.class)).add(part);
            }
        });
        partsByModifier.replaceAll((modifierId, parts) -> Collections.unmodifiableSet(parts));
        return new Index(Collections.unmodifiableMap(partsByModifier), Collections.unmodifiableMap(listedByPart));
    }

    private static void add(Map<ItemKind, Map<String, Modifier>> modifiersByPart, ItemKind part, Modifier modifier) {
        modifiersByPart.get(part).putIfAbsent(modifier.id(), modifier);
    }
}
