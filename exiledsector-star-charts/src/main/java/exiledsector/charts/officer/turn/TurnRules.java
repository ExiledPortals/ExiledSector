package exiledsector.charts.officer.turn;

import java.util.Map;

public record TurnRules(int otherBossKillsPerTurn, float expandBrakeSize, float evenChancePercent, float chancePerRankPercent,
                        float defendBonusPercent, int renownPerClaim, int renownPerChallengeWon, int renownPerDefendHeld, int systemsPerRenown,
                        int renownKeptOnLossPercent, int maxConflictsPerTurn, Map<String, IntentWeights> intentWeights) {

    public TurnRules {
        intentWeights = Map.copyOf(intentWeights);
    }

    public TurnRules withIntentWeights(Map<String, IntentWeights> weights) {
        return new TurnRules(otherBossKillsPerTurn, expandBrakeSize, evenChancePercent, chancePerRankPercent, defendBonusPercent, renownPerClaim,
                renownPerChallengeWon, renownPerDefendHeld, systemsPerRenown, renownKeptOnLossPercent, maxConflictsPerTurn, weights);
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
