package exiledsector.items;

import exiledlib.items.ItemDefinition;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class SubroutineNodePools {

    public interface NodeCount {

        int roll(ItemDefinition definition, long seed);
    }

    private record Pool(Function<ItemDefinition, Collection<String>> candidates, NodeCount nodeCount) {
    }

    static final long NODE_ROLL_SALT = 0x5EB2_0C7A_9D41_E3B7L;
    private static final NodeCount ONE_NODE = (definition, seed) -> 1;
    private static final Logger LOG = Logger.getLogger(SubroutineNodePools.class);
    private static final Map<SubroutineKind, Pool> POOLS_BY_KIND = new ConcurrentHashMap<>();

    private SubroutineNodePools() {
    }

    public static void use(SubroutineKind kind, Function<ItemDefinition, Collection<String>> candidatePool) {
        use(kind, candidatePool, ONE_NODE);
    }

    public static void use(SubroutineKind kind, Function<ItemDefinition, Collection<String>> candidatePool, NodeCount nodeCount) {
        if (kind == null) {
            throw new IllegalArgumentException("a node pool needs a subroutine kind");
        }
        if (candidatePool == null) {
            POOLS_BY_KIND.remove(kind);
        } else {
            POOLS_BY_KIND.put(kind, new Pool(candidatePool, nodeCount == null ? ONE_NODE : nodeCount));
        }
    }

    public static void clear() {
        POOLS_BY_KIND.clear();
    }

    public static List<String> candidates(ItemDefinition definition) {
        Pool pool = pool(definition);
        if (pool == null) {
            return List.of();
        }
        try {
            return sortedCandidates(pool.candidates().apply(definition));
        } catch (RuntimeException e) {
            LOG.error("Could not build the node pool for the subroutine " + definition.id() + "; it rolls no node", e);
            return List.of();
        }
    }

    public static List<String> roll(ItemDefinition definition, long seed) {
        Pool pool = pool(definition);
        if (pool == null) {
            return List.of();
        }
        return pick(candidates(definition), seed, pool.nodeCount().roll(definition, seed));
    }

    public static String pick(Collection<String> candidateNodeIds, long seed) {
        List<String> picked = pick(candidateNodeIds, seed, 1);
        return picked.isEmpty() ? null : picked.get(0);
    }

    public static List<String> pick(Collection<String> candidateNodeIds, long seed, int count) {
        List<String> shuffled = new ArrayList<>(sortedCandidates(candidateNodeIds));
        int pickedCount = Math.min(Math.max(0, count), shuffled.size());
        SplittableRandom random = new SplittableRandom(seed ^ NODE_ROLL_SALT);
        for (int index = 0; index < pickedCount; index++) {
            Collections.swap(shuffled, index, index + random.nextInt(shuffled.size() - index));
        }
        return List.copyOf(shuffled.subList(0, pickedCount));
    }

    private static Pool pool(ItemDefinition definition) {
        SubroutineKind kind = SubroutineKind.of(definition);
        return kind == null || !kind.rollsNode() ? null : POOLS_BY_KIND.get(kind);
    }

    private static List<String> sortedCandidates(Collection<String> candidateNodeIds) {
        if (candidateNodeIds == null) {
            return List.of();
        }
        return candidateNodeIds.stream().filter(Objects::nonNull).map(String::trim).filter(nodeId -> !nodeId.isEmpty())
                .distinct().sorted().toList();
    }
}
