package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.Translation;

public final class NodeIntegration {

    private NodeIntegration() {
    }

    public static int availableStoryPoints() {
        MutableCharacterStatsAPI stats = playerStats();
        return stats == null ? 0 : stats.getStoryPoints();
    }

    public static boolean integrate(ShipSkillData data, SkillNode node, HullSize hullSize, String shipName) {
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
        stats.spendStoryPoints(cost, true, null, true, bonusXpFraction(hullSize), logText);
        return data.integrate(node.getId());
    }

    public static void refundStoryPoints(int storyPoints) {
        MutableCharacterStatsAPI stats = playerStats();
        if (stats != null && storyPoints > 0) {
            stats.addStoryPoints(storyPoints);
        }
    }

    static float bonusXpFraction(HullSize hullSize) {
        String key = switch (hullSize == null ? HullSize.CAPITAL_SHIP : hullSize) {
            case FRIGATE -> "permModFrigate";
            case DESTROYER -> "permModDestroyer";
            case CRUISER -> "permModCruiser";
            default -> "permModCapital";
        };
        return Math.max(0f, Math.min(1f, Global.getSettings().getBonusXP(key)));
    }

    private static MutableCharacterStatsAPI playerStats() {
        return Global.getSector() == null ? null : Global.getSector().getPlayerStats();
    }
}
