package exiledsector.loot;

public record FleetLevelRange(int lowest, int highest, int extraLevels, float treeShare) {

    public FleetLevelRange(int lowest, int highest, int extraLevels) {
        this(lowest, highest, extraLevels, 1f);
    }
}
