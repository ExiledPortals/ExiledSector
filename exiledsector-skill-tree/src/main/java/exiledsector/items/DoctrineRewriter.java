package exiledsector.items;

import exiledlib.Seeds;
import exiledlib.items.PoolEntry;
import exiledlib.items.WeightedPick;
import exiledlib.modifiers.ModifierLine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class DoctrineRewriter {

    static final int LOWEST_REVISION = 1000;
    static final int REVISION_SPAN = 9000;
    private static final long REVISION_SALT = 0x5EED_0D0C_7215_EL;
    private static final long CANDIDATE_SALT = 0x0C0D_E5C4_A11B_0A7EL;
    private static final long AUGMENT_SALT = 0x0A06_3E47_1E55_0BADL;
    private static final long NODE_SPREAD = 0x9E3779B97F4A7C15L;
    private static final long CANDIDATE_SPREAD = 0xC2B2AE3D27D4EB4FL;

    public record AugmentLine(String modifierId, ModifierLine line, float rolledMagnitude) {
    }

    private record RankedCandidate(String typeId, long rank) {
    }

    private DoctrineRewriter() {
    }

    public static int revision(long seed) {
        return LOWEST_REVISION + (int) Math.floorMod(Seeds.scramble(seed ^ REVISION_SALT), (long) REVISION_SPAN);
    }

    public static List<String> candidateOrder(long seed, String nodeId, List<String> pool) {
        long nodeKey = nodeKey(seed, nodeId, CANDIDATE_SALT);
        List<RankedCandidate> ranked = new ArrayList<>(pool.size());
        for (String typeId : pool) {
            if (ranked.stream().noneMatch(candidate -> candidate.typeId().equals(typeId))) {
                ranked.add(new RankedCandidate(typeId, Seeds.scramble(nodeKey ^ typeId.hashCode() * CANDIDATE_SPREAD)));
            }
        }
        ranked.sort(Comparator.comparingLong(RankedCandidate::rank).thenComparing(RankedCandidate::typeId));
        return ranked.stream().map(RankedCandidate::typeId).toList();
    }

    public static List<AugmentLine> augmentLines(DoctrineArchive archive, long seed, String nodeId) {
        List<PoolEntry> pool = archive.modifierPool();
        if (pool.isEmpty()) {
            return List.of();
        }
        Random random = new Random(nodeKey(seed, nodeId, AUGMENT_SALT));
        int lineCount = archive.minLines() + random.nextInt(archive.maxLines() - archive.minLines() + 1);
        List<PoolEntry> remainingPool = new ArrayList<>(pool);
        List<AugmentLine> lines = new ArrayList<>();
        for (int drawsLeft = Math.min(lineCount, pool.size()); drawsLeft > 0; drawsLeft--) {
            PoolEntry entry = remainingPool.remove(WeightedPick.index(remainingPool, PoolEntry::weight, random.nextFloat()));
            for (ModifierLine line : entry.modifier().lines()) {
                lines.add(new AugmentLine(entry.modifier().id(), line, line.roll(random)));
            }
        }
        return List.copyOf(lines);
    }

    private static long nodeKey(long seed, String nodeId, long salt) {
        return Seeds.scramble(Seeds.scramble(seed ^ salt) ^ nodeId.hashCode() * NODE_SPREAD);
    }
}
