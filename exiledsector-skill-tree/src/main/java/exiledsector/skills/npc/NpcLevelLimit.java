package exiledsector.skills.npc;

import java.util.Random;

public record NpcLevelLimit(int cap, int flagshipCap, int flagshipLevels, int fixedMin) {

    public static final int FOLLOWS_PLAYER = -1;
    public static final NpcLevelLimit UNCAPPED = of(NpcLevelCaps.UNCAPPED);

    public NpcLevelLimit(int cap, int flagshipCap, int flagshipLevels) {
        this(cap, flagshipCap, flagshipLevels, FOLLOWS_PLAYER);
    }

    public static NpcLevelLimit of(int cap) {
        return new NpcLevelLimit(cap, cap, 0);
    }

    public NpcLevelLimit fixedFrom(int min) {
        return new NpcLevelLimit(cap, flagshipCap, flagshipLevels, Math.max(0, Math.min(min, cap)));
    }

    public boolean isFixed() {
        return fixedMin >= 0;
    }

    public int rollFixed(Random random) {
        return fixedMin + random.nextInt(cap - fixedMin + 1);
    }

    public int capFor(boolean flagship) {
        return flagship ? flagshipCap : cap;
    }

    public int nodeCount(int rolledLevel, int maxNodes, boolean flagship) {
        int bonusLevels = flagship ? flagshipLevels : 0;
        return Math.min(rolledLevel + bonusLevels, Math.min(capFor(flagship), maxNodes));
    }
}
