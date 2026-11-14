package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.anomaly.AnomalyState;
import exiledsector.charts.run.ChartRun;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

record SkirmishFlagships(SkirmishFlagship attacker, SkirmishFlagship defender) {

    static SkirmishFlagships of(ChartRun run) {
        return run == null ? null : of(AnomalyState.of(run, FactionSkirmish.ANOMALY_ID));
    }

    static SkirmishFlagships of(AnomalyState state) {
        SkirmishFlagship attacker = SkirmishFlagship.decode(FightingSide.ATTACKER, state.text(FightingSide.ATTACKER.key()));
        SkirmishFlagship defender = SkirmishFlagship.decode(FightingSide.DEFENDER, state.text(FightingSide.DEFENDER.key()));
        return attacker == null && defender == null ? null : new SkirmishFlagships(attacker, defender);
    }

    static void record(AnomalyState state, SkirmishFlagship flagship) {
        state.put(flagship.side().key(), flagship.encode());
    }

    List<SkirmishFlagship> tracked() {
        List<SkirmishFlagship> tracked = new ArrayList<>(2);
        if (attacker != null) {
            tracked.add(attacker);
        }
        if (defender != null) {
            tracked.add(defender);
        }
        return tracked;
    }

    List<SkirmishFlagship> standing() {
        return tracked().stream().filter(flagship -> !flagship.hasFallen()).toList();
    }

    int fallenCount() {
        return tracked().size() - standing().size();
    }

    boolean allFallen() {
        return standing().isEmpty();
    }

    SkirmishResult result() {
        if (attacker == null || defender == null) {
            return SkirmishResult.UNDECIDED;
        }
        if (attacker.fall() == FlagshipFall.BEATEN_BY_PLAYER && defender.fall() == FlagshipFall.BEATEN_BY_PLAYER) {
            return SkirmishResult.DROPPED;
        }
        SkirmishFlagship firstFallen = tracked().stream().filter(SkirmishFlagship::hasFallen)
                .min(Comparator.comparingInt(SkirmishFlagship::fallOrder)).orElse(null);
        if (firstFallen == null) {
            return SkirmishResult.UNDECIDED;
        }
        return firstFallen.side() == FightingSide.ATTACKER ? SkirmishResult.DEFENDER_WINS : SkirmishResult.ATTACKER_WINS;
    }
}
