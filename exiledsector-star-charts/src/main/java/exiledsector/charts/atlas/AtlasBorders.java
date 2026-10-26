package exiledsector.charts.atlas;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class AtlasBorders {

    private static final long ATTACKER_SALT = 0x41545441434bL;

    private AtlasBorders() {
    }

    public static String attacker(AtlasGenerator generator, AtlasState state, AtlasCell cell, Predicate<String> takesSides) {
        String defender = AtlasOpening.ownerOf(generator, state, cell);
        Map<String, Float> lanesInto = new LinkedHashMap<>();
        for (AtlasCell neighbour : generator.lanes(cell)) {
            if (neighbour.equals(AtlasCell.ORIGIN)) {
                continue;
            }
            String owner = AtlasOpening.ownerOf(generator, state, neighbour);
            if (owner != null && !owner.equals(defender) && takesSides.test(owner)) {
                lanesInto.merge(owner, 1f, Float::sum);
            }
        }
        return AtlasHash.race(generator.seed(), ATTACKER_SALT, cell.gridX(), cell.gridY(), lanesInto);
    }
}
