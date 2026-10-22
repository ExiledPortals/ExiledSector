package exiledsector.skills.npc;

public record NpcLevelLimit(int cap, int flagshipCap, int flagshipLevels) {

    public static final NpcLevelLimit UNCAPPED = of(NpcLevelCaps.UNCAPPED);

    public static NpcLevelLimit of(int cap) {
        return new NpcLevelLimit(cap, cap, 0);
    }

    public int capFor(boolean flagship) {
        return flagship ? flagshipCap : cap;
    }

    public int nodeCount(int rolledLevel, int maxNodes, boolean flagship) {
        int bonusLevels = flagship ? flagshipLevels : 0;
        return Math.min(rolledLevel + bonusLevels, Math.min(capFor(flagship), maxNodes));
    }
}
