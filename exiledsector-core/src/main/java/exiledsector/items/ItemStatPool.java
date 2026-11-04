package exiledsector.items;

import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierEffect;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemStatPool {

    static final String VALUE_MARK = "#";
    private static final String LOCALE_SEPARATOR = "\u0000";
    private static final Pattern SHOWN_NUMBER = Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?");
    private static final Pattern LEADING_NON_LETTERS = Pattern.compile("^[^\\p{L}]+");
    private static final AtomicReference<Pools> CACHED = new AtomicReference<>();

    private record Pools(ModifierParts.Index index, Map<ItemKind, Set<String>> statsByKind, Set<String> allStats,
                         Map<String, Float> sampleValues, Map<String, String> labels) {
    }

    private ItemStatPool() {
    }

    public static List<String> stats(Collection<ItemKind> chosenKinds) {
        Pools pools = pools();
        Set<String> stats = new LinkedHashSet<>();
        for (ItemKind kind : chosenKinds) {
            stats.addAll(pools.statsByKind().getOrDefault(kind, Set.of()));
        }
        Collection<String> listed = stats.isEmpty() ? pools.allStats() : stats;
        return listed.stream().sorted(Comparator.comparing(statName -> sortKey(label(pools, statName)))).toList();
    }

    public static String label(String statName) {
        return label(pools(), statName);
    }

    private static String label(Pools pools, String statName) {
        String localeKey = I18n.locale() + LOCALE_SEPARATOR + statName;
        return pools.labels().computeIfAbsent(localeKey, key -> labelFor(statName, pools.sampleValues().get(statName)));
    }

    private static String sortKey(String statLabel) {
        return LEADING_NON_LETTERS.matcher(statLabel).replaceFirst("").toLowerCase(Locale.ROOT);
    }

    private static Pools pools() {
        ModifierParts.Index index = ModifierParts.index();
        Pools cached = CACHED.get();
        if (cached != null && cached.index() == index) {
            return cached;
        }
        Pools built = build(index);
        CACHED.set(built);
        return built;
    }

    private static Pools build(ModifierParts.Index index) {
        ModifierEffects effects = ItemEffects.forType(ItemType.HULL_ITEM);
        Map<String, Float> sampleValues = new LinkedHashMap<>();
        Map<ItemKind, Set<String>> statsByKind = new EnumMap<>(ItemKind.class);
        Set<String> allStats = new LinkedHashSet<>();
        for (ItemKind kind : ItemKind.sectionItemTypes()) {
            Set<String> kindStats = new LinkedHashSet<>();
            index.modifiers(kind).forEach(modifier -> addLines(kindStats, modifier, effects, sampleValues));
            statsByKind.put(kind, kindStats);
            allStats.addAll(kindStats);
        }
        return new Pools(index, statsByKind, Set.copyOf(allStats), sampleValues, new ConcurrentHashMap<>());
    }

    private static void addLines(Set<String> stats, Modifier modifier, ModifierEffects effects, Map<String, Float> sampleValues) {
        for (ModifierLine line : modifier.lines()) {
            String currentName = effects.currentName(line.effectName());
            String statName = currentName == null ? line.effectName() : currentName;
            stats.add(statName);
            sampleValues.putIfAbsent(statName, line.isScaled() ? line.scaledValues().get(0) : line.boundFarthestFromZero());
        }
    }

    static String labelFor(String statName, Float sampleValue) {
        ModifierEffect effect = ItemEffects.forType(ItemType.HULL_ITEM).byName(statName);
        if (effect == null || sampleValue == null) {
            return statName;
        }
        StyledText description = effect.description(sampleValue);
        String plain = description.plain();
        if (!description.spans().isEmpty()) {
            StyledText.Span valueSpan = description.spans().get(0);
            Matcher numberMatcher = SHOWN_NUMBER.matcher(plain).region(valueSpan.start(), valueSpan.end());
            if (numberMatcher.find()) {
                plain = plain.substring(0, numberMatcher.start()) + VALUE_MARK + plain.substring(numberMatcher.end());
            }
        }
        plain = plain.trim();
        return plain.endsWith(".") ? plain.substring(0, plain.length() - 1) : plain;
    }
}
