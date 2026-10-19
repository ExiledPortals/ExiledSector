package exiledsector.items;

import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class SubroutineNodePools {

    static final long NODE_ROLL_SALT = 0x5EB2_0C7A_9D41_E3B7L;
    private static final Logger LOG = Logger.getLogger(SubroutineNodePools.class);
    private static final Map<SubroutineKind, Function<ItemDefinition, Collection<String>>> POOLS_BY_KIND = new ConcurrentHashMap<>();

    private SubroutineNodePools() {
    }

    public static void use(SubroutineKind kind, Function<ItemDefinition, Collection<String>> candidatePool) {
        if (kind == null) {
            throw new IllegalArgumentException("a node pool needs a subroutine kind");
        }
        if (candidatePool == null) {
            POOLS_BY_KIND.remove(kind);
        } else {
            POOLS_BY_KIND.put(kind, candidatePool);
        }
    }

    public static void clear() {
        POOLS_BY_KIND.clear();
    }

    public static List<String> candidates(ItemDefinition definition) {
        SubroutineKind kind = definition == null ? null : definition.subroutineKind();
        Function<ItemDefinition, Collection<String>> candidatePool = kind == null || !kind.rollsNode() ? null : POOLS_BY_KIND.get(kind);
        if (candidatePool == null) {
            return List.of();
        }
        try {
            return sortedCandidates(candidatePool.apply(definition));
        } catch (RuntimeException e) {
            LOG.error("Could not build the node pool for the subroutine " + definition.id() + "; it rolls no node", e);
            return List.of();
        }
    }

    public static String roll(ItemDefinition definition, long seed) {
        return pick(candidates(definition), seed);
    }

    public static String pick(Collection<String> candidateNodeIds, long seed) {
        List<String> sortedNodeIds = sortedCandidates(candidateNodeIds);
        if (sortedNodeIds.isEmpty()) {
            return null;
        }
        return sortedNodeIds.get(new SplittableRandom(seed ^ NODE_ROLL_SALT).nextInt(sortedNodeIds.size()));
    }

    private static List<String> sortedCandidates(Collection<String> candidateNodeIds) {
        if (candidateNodeIds == null) {
            return List.of();
        }
        return candidateNodeIds.stream().filter(Objects::nonNull).map(String::trim).filter(nodeId -> !nodeId.isEmpty())
                .distinct().sorted().toList();
    }
}
