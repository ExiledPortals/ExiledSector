package exiledsector.charts.officer.turn;

import java.util.Map;
import java.util.Random;

public record TurnRules(int otherBossKillsPerTurn, float expandBrakeSize, float evenChancePercent, float chancePerRankPercent,
                        float defendBonusPercent, int renownPerClaim, int renownPerChallengeWon, int renownPerDefendHeld, int systemsPerRenown,
                        int renownKeptOnLossPercent, int maxConflictsPerTurn, int scavengersMin, int scavengersMax,
                        Map<String, IntentWeights> intentWeights) {

    public TurnRules {
        intentWeights = Map.copyOf(intentWeights);
    }

    public TurnRules withIntentWeights(Map<String, IntentWeights> weights) {
        return new TurnRules(otherBossKillsPerTurn, expandBrakeSize, evenChancePercent, chancePerRankPercent, defendBonusPercent, renownPerClaim,
                renownPerChallengeWon, renownPerDefendHeld, systemsPerRenown, renownKeptOnLossPercent, maxConflictsPerTurn, scavengersMin, scavengersMax,
                weights);
    }

    public int scavengerCount(Random random) {
        int fewest = Math.max(0, scavengersMin);
        int most = Math.max(fewest, scavengersMax);
        return fewest + random.nextInt(most - fewest + 1);
    }

    public float intentWeight(String rankId, String traitId, IntentKind kind) {
        return weightsOf(rankId).of(kind) * weightsOf(traitId).of(kind);
    }

    private IntentWeights weightsOf(String id) {
        return id == null ? IntentWeights.EVEN : intentWeights.getOrDefault(id, IntentWeights.EVEN);
    }

    public float expandWeight(String rankId, String traitId, int territorySize) {
        float weight = intentWeight(rankId, traitId, IntentKind.EXPAND);
        return expandBrakeSize <= 0f ? weight : weight * expandBrakeSize / (expandBrakeSize + territorySize);
    }

    public float attackerChancePercent(int attackerRankOrder, int defenderRankOrder, boolean defended) {
        float chance = evenChancePercent + chancePerRankPercent * (attackerRankOrder - defenderRankOrder) - (defended ? defendBonusPercent : 0f);
        return Math.max(0f, Math.min(100f, chance));
    }

    public int renownAfterLoss(int renown) {
        return renown * renownKeptOnLossPercent / 100;
    }

    public int holdingsRenown(int systemsHeld) {
        return systemsPerRenown <= 0 ? 0 : systemsHeld / systemsPerRenown;
    }
}
