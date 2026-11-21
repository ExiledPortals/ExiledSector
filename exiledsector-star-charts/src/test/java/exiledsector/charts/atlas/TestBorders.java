package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;

import java.util.List;
import java.util.Map;

public final class TestBorders {

    public static final long SEED = 0x5C1A7L;
    public static final String NEUTRAL = "remnant";
    public static final AtlasRules RULES = new AtlasRules(List.of(AtlasRegion.DEFAULT_NORMAL), Map.of("compact", 1f),
            Map.of("hegemony", 1f, "tritachyon", 1f, "pirates", 1f, NEUTRAL, 1f), AtlasAnomalyChance.NEVER, AtlasAnomalyChance.NEVER,
            List.of(), List.of());

    public record Border(AtlasCell contested, AtlasOfficer attacker, AtlasOfficer defender) {
    }

    private TestBorders() {
    }

    public static AtlasGenerator generator(AtlasState state) {
        return new AtlasGenerator(SEED, RULES, AtlasUnlocks.NONE, state);
    }

    public static Border border(AtlasGenerator generator, AtlasState state, String attackerFactionId, String defenderFactionId) {
        for (AtlasCell contested : AtlasCell.ORIGIN.around(20)) {
            if (!open(generator, state, contested) || generator.node(contested).specialId() != null) {
                continue;
            }
            List<AtlasCell> neighbours = generator.lanes(contested).stream().filter(lane -> open(generator, state, lane)).toList();
            if (neighbours.size() >= 2) {
                AtlasOfficer defender = TestTerritories.establish(state, neighbours.get(0), defenderFactionId);
                state.claim(contested, defender.commandCentre());
                AtlasOfficer attacker = TestTerritories.establish(state, neighbours.get(1), attackerFactionId);
                return new Border(contested, attacker, defender);
            }
        }
        throw new AssertionError("no border system");
    }

    public static boolean open(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return !cell.equals(AtlasCell.ORIGIN) && !generator.isVoid(cell) && !state.isClaimed(cell) && generator.infestationAt(cell) == null
                && generator.specialAt(cell) == null;
    }
}
