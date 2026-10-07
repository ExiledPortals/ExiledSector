package exiledsector.skills.npc;

import exiledsector.ModSettings;

import java.util.Random;

public final class NpcLevelTable {

    public static final int MIN_PLAYER_LEVEL = 1;
    public static final int MAX_PLAYER_LEVEL = 15;
    public static final int MIN_NODES = 0;
    public static final int MAX_NODES = NpcSkillTreeBuilder.MAX_NODE_COUNT;

    private static final int[] DEFAULT_MIN_NODES = {1, 2, 4, 7, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16};
    private static final int[] DEFAULT_MAX_NODES = {2, 3, 6, 10, 13, 16, 19, 22, 25, 28, 29, 32, 35, 38, 42};

    public record NodeRange(int min, int max) {
    }

    private NpcLevelTable() {
    }

    public static String minNodesFieldId(int playerLevel) {
        return "exiledSector_npcLevel" + playerLevel + "MinNodes";
    }

    public static String maxNodesFieldId(int playerLevel) {
        return "exiledSector_npcLevel" + playerLevel + "MaxNodes";
    }

    public static int defaultMinNodes(int playerLevel) {
        return DEFAULT_MIN_NODES[clampLevel(playerLevel) - MIN_PLAYER_LEVEL];
    }

    public static int defaultMaxNodes(int playerLevel) {
        return DEFAULT_MAX_NODES[clampLevel(playerLevel) - MIN_PLAYER_LEVEL];
    }

    public static NodeRange range(int playerLevel) {
        int clampedLevel = clampLevel(playerLevel);
        int configuredMin = setting(minNodesFieldId(clampedLevel), defaultMinNodes(clampedLevel));
        int configuredMax = setting(maxNodesFieldId(clampedLevel), defaultMaxNodes(clampedLevel));
        return new NodeRange(Math.min(configuredMin, configuredMax), Math.max(configuredMin, configuredMax));
    }

    public static int roll(int playerLevel, Random random) {
        NodeRange nodeRange = range(playerLevel);
        return nodeRange.min() + random.nextInt(nodeRange.max() - nodeRange.min() + 1);
    }

    private static int clampLevel(int playerLevel) {
        return Math.max(MIN_PLAYER_LEVEL, Math.min(MAX_PLAYER_LEVEL, playerLevel));
    }

    private static int setting(String fieldId, int defaultValue) {
        int nodeCount = ModSettings.intOr(fieldId, defaultValue);
        return Math.max(MIN_NODES, Math.min(MAX_NODES, nodeCount));
    }
}
