package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipLevelSystem;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CombatXpListener extends BaseCampaignEventListener {

    public CombatXpListener() {
        super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (result == null || playerFleet == null) {
            return;
        }
        float defeatedDp = enemyDeploymentPointsDefeated(result);
        boolean lost = !result.didPlayerWin();
        float lossMultiplier = lost ? ShipLevelConfig.xpLossMultiplier() : 1f;
        float difficultyMultiplier = ShipLevelSystem.difficultyMultiplier(BattleDifficulty.current(),
                ShipLevelConfig.xpDifficultyStrength(), ShipLevelConfig.xpDifficultyMaxMultiplier());
        float xp = defeatedDp * ShipLevelConfig.xpPerDeploymentPoint() * lossMultiplier * difficultyMultiplier;

        SkillTreeInstaller.adoptNpcTrees(playerFleet);
        SkillTreeInstaller.raiseToLevelFloor(playerFleet);
        Map<FleetMemberAPI, Integer> levelsBefore = levelsOf(playerFleet);
        float catchUpMultiplier = ShipLevelSystem.awardXpToFleet(playerFleet, xp);
        I18n.forGameText(() -> report(new CombatXpReport(xp, defeatedDp, lost, difficultyMultiplier, catchUpMultiplier,
                levelUps(levelsBefore))));
    }

    private static float enemyDeploymentPointsDefeated(EngagementResultAPI result) {
        EngagementResultForFleetAPI enemy = result.didPlayerWin() ? result.getLoserResult() : result.getWinnerResult();
        if (enemy == null) {
            return 0f;
        }
        return deploymentPointsOf(enemy.getDestroyed()) + deploymentPointsOf(enemy.getDisabled());
    }

    private static float deploymentPointsOf(List<FleetMemberAPI> members) {
        float total = 0f;
        for (FleetMemberAPI member : members) {
            total += member.getDeploymentPointsCost();
        }
        return total;
    }

    private static Map<FleetMemberAPI, Integer> levelsOf(CampaignFleetAPI fleet) {
        Map<FleetMemberAPI, Integer> levels = new LinkedHashMap<>();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            levels.put(member, ShipSkillDataManager.get(member.getId()).getLevel());
        }
        return levels;
    }

    private static List<String> levelUps(Map<FleetMemberAPI, Integer> levelsBefore) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<FleetMemberAPI, Integer> entry : levelsBefore.entrySet()) {
            FleetMemberAPI member = entry.getKey();
            int level = ShipSkillDataManager.get(member.getId()).getLevel();
            if (level > entry.getValue()) {
                lines.add(Translation.msg("combat.xp.levelUp").arg("ship", member.getShipName())
                        .arg("hull", member.getHullSpec().getHullNameWithDashClass()).arg("level", level).text());
            }
        }
        return lines;
    }

    private static void report(CombatXpReport report) {
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null || dialog.getTextPanel() == null) {
            return;
        }
        TextPanelAPI text = dialog.getTextPanel();
        text.setFontSmallInsignia();
        VanillaText.addPara(text, Translation.styled("combat.xp.header"), Misc.getBasePlayerColor());
        text.setFontInsignia();
        String earnedKey = report.lost() ? "combat.xp.earnedAfterLoss" : "combat.xp.earned";
        VanillaText.addPara(text, Translation.msg(earnedKey).arg("xp", report.xpText()).arg("dp", report.dpText()).styled(),
                Misc.getTextColor());
        if (report.hasDifficultyBonus()) {
            VanillaText.addPara(text, Translation.msg("combat.xp.difficultyBonus").arg("percent", report.difficultyBonusText()).styled(),
                    Misc.getTextColor());
        }
        if (report.hasCatchUpBonus()) {
            VanillaText.addPara(text, Translation.msg("combat.xp.catchUpBonus").arg("multiplier", report.catchUpText()).styled(),
                    Misc.getTextColor());
        }
        for (String levelUp : report.levelUps()) {
            VanillaText.addPara(text, StyledText.of(levelUp), Misc.getPositiveHighlightColor());
        }
    }

    record CombatXpReport(float xp, float defeatedDp, boolean lost, float difficultyMultiplier, float catchUpMultiplier,
                         List<String> levelUps) {

        String xpText() {
            return String.valueOf(Math.round(xp));
        }

        String dpText() {
            return String.valueOf(Math.round(defeatedDp));
        }

        boolean hasDifficultyBonus() {
            return Math.round(xp) > 0 && difficultyBonusPercent() >= 1;
        }

        String difficultyBonusText() {
            return String.valueOf(difficultyBonusPercent());
        }

        boolean hasCatchUpBonus() {
            return Math.round(xp) > 0 && catchUpMultiplier >= 1.05f;
        }

        String catchUpText() {
            int tenths = Math.round(catchUpMultiplier * 10f);
            return tenths % 10 == 0 ? String.valueOf(tenths / 10) : tenths / 10 + "." + tenths % 10;
        }

        private int difficultyBonusPercent() {
            return Math.round((difficultyMultiplier - 1f) * 100f);
        }
    }
}
