package exiledsector.skills.unlock;


public final class UnlockCondition {

    private final UnlockConditionType unlockType;
    private final BlueprintCategory blueprintCategory;
    private final String conditionKey;
    private final int minLevel;

    private UnlockCondition(UnlockConditionType unlockType, BlueprintCategory blueprintCategory, String conditionKey, int minLevel) {
        this.unlockType = unlockType;
        this.blueprintCategory = blueprintCategory;
        this.conditionKey = conditionKey;
        this.minLevel = minLevel;
    }

    public static UnlockCondition blueprint(BlueprintCategory category, String id) {
        return new UnlockCondition(UnlockConditionType.BLUEPRINT, category, id, 0);
    }

    public static UnlockCondition characterStat(String statId) {
        return new UnlockCondition(UnlockConditionType.CHARACTER_STAT, null, statId, 0);
    }

    public static UnlockCondition minShipLevel(int level) {
        return new UnlockCondition(UnlockConditionType.MIN_SHIP_LEVEL, null, null, level);
    }

    public static UnlockCondition memoryFlag(String conditionKey) {
        return new UnlockCondition(UnlockConditionType.MEMORY_FLAG, null, conditionKey, 0);
    }

    public static UnlockCondition ratPlayerExoship() {
        return new UnlockCondition(UnlockConditionType.RAT_PLAYER_EXOSHIP, null, null, 0);
    }

    public static UnlockCondition ratSingularityDefeated() {
        return new UnlockCondition(UnlockConditionType.RAT_SINGULARITY_DEFEATED, null, null, 0);
    }

    public static UnlockCondition nightcrossPascalVisited() {
        return new UnlockCondition(UnlockConditionType.NIGHTCROSS_PASCAL_VISITED, null, null, 0);
    }

    public static UnlockCondition nightcrossLunarFleetDefeated() {
        return new UnlockCondition(UnlockConditionType.NIGHTCROSS_LUNAR_FLEET_DEFEATED, null, null, 0);
    }

    public static UnlockCondition nightcrossAncientPrototypeFound() {
        return new UnlockCondition(UnlockConditionType.NIGHTCROSS_ANCIENT_PROTOTYPE_FOUND, null, null, 0);
    }

    public UnlockConditionType getType() {
        return unlockType;
    }

    public BlueprintCategory getBlueprintCategory() {
        return blueprintCategory;
    }

    public String getKey() {
        return conditionKey;
    }

    public int getMinLevel() {
        return minLevel;
    }

    @Override
    public String toString() {
        return "UnlockCondition{type=" + unlockType + ", blueprintCategory=" + blueprintCategory
                + ", key=" + conditionKey + ", minLevel=" + minLevel + "}";
    }
}
