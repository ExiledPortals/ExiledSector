package exiledsector.charts.officer.turn;

import java.util.EnumMap;
import java.util.Map;

public final class IntentResolvers {

    public static final IntentResolvers ABSTRACT = new IntentResolvers(Map.of());

    private final Map<IntentKind, IntentResolver> resolversByKind;

    private IntentResolvers(Map<IntentKind, IntentResolver> resolversByKind) {
        this.resolversByKind = resolversByKind;
    }

    public IntentResolvers with(IntentKind kind, IntentResolver resolver) {
        Map<IntentKind, IntentResolver> resolvers = new EnumMap<>(IntentKind.class);
        resolvers.putAll(resolversByKind);
        resolvers.put(kind, resolver);
        return new IntentResolvers(resolvers);
    }

    public IntentResolver forKind(IntentKind kind) {
        return resolversByKind.getOrDefault(kind, IntentResolver.ABSTRACT);
    }
}
