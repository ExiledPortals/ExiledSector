package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import exiledsector.i18n.Translation;

public final class NodeIntegration {

    private NodeIntegration() {
    }

    public static int availableStoryPoints() {
        MutableCharacterStatsAPI stats = playerStats();
        return stats == null ? 0 : stats.getStoryPoints();
    }

    public static boolean integrate(ShipSkillData data, SkillNode node, String shipName) {
        if (!IntegratedNodes.canIntegrate(data, node)) {
            return false;
        }
        int cost = IntegratedNodes.nextCost(data);
        MutableCharacterStatsAPI stats = playerStats();
        if (stats == null || stats.getStoryPoints() < cost) {
            return false;
        }
        String logText = Translation.msg("node.integration.log").arg("node", node.resolveEffectiveType(data).getDisplayName())
                .arg("ship", shipName).text();
        stats.spendStoryPoints(cost, true, null, true, logText);
        return data.integrate(node.getId());
    }

    public static void refundStoryPoints(int storyPoints) {
        MutableCharacterStatsAPI stats = playerStats();
        if (stats != null && storyPoints > 0) {
            stats.addStoryPoints(storyPoints);
        }
    }

    private static MutableCharacterStatsAPI playerStats() {
        return Global.getSector() == null ? null : Global.getSector().getPlayerStats();
    }
}
