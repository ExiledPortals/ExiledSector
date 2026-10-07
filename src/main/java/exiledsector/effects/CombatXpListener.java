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
    public void reportPlayerEngagement(EngagementResultAPI engagementResult) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (engagementResult == null || playerFleet == null) {
            return;
        }
        float defeatedDp = enemyDeploymentPointsDefeated(engagementResult);
        boolean lost = !engagementResult.didPlayerWin();
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

    static float enemyDeploymentPointsDefeated(EngagementResultAPI engagementResult) {
        EngagementResultForFleetAPI enemyResult = engagementResult.didPlayerWin() ? engagementResult.getLoserResult() : engagementResult.getWinnerResult();
        if (enemyResult == null) {
            return 0f;
        }
        return deploymentPointsOf(enemyResult.getDestroyed()) + deploymentPointsOf(enemyResult.getDisabled());
    }

    private static float deploymentPointsOf(List<FleetMemberAPI> members) {
        float totalDp = 0f;
        for (FleetMemberAPI member : members) {
            totalDp += member.getDeploymentPointsCost();
        }
        return totalDp;
    }

    private static Map<FleetMemberAPI, Integer> levelsOf(CampaignFleetAPI fleet) {
        Map<FleetMemberAPI, Integer> levels = new LinkedHashMap<>();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            levels.put(member, ShipSkillDataManager.get(member.getId()).getLevel());
        }
        return levels;
    }

    private static List<String> levelUps(Map<FleetMemberAPI, Integer> levelsBefore) {
        List<String> levelUpLines = new ArrayList<>();
        for (Map.Entry<FleetMemberAPI, Integer> entry : levelsBefore.entrySet()) {
            FleetMemberAPI levelledMember = entry.getKey();
            int newLevel = ShipSkillDataManager.get(levelledMember.getId()).getLevel();
            if (newLevel > entry.getValue()) {
                levelUpLines.add(Translation.msg("combat.xp.levelUp").arg("ship", levelledMember.getShipName())
                        .arg("hull", levelledMember.getHullSpec().getHullNameWithDashClass()).arg("level", newLevel).text());
            }
        }
        return levelUpLines;
    }

    private static void report(CombatXpReport report) {
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null || dialog.getTextPanel() == null) {
            return;
        }
        TextPanelAPI textPanel = dialog.getTextPanel();
        textPanel.setFontSmallInsignia();
        VanillaText.addPara(textPanel, Translation.styled("combat.xp.header"), Misc.getBasePlayerColor());
        textPanel.setFontInsignia();
        String earnedKey = report.lost() ? "combat.xp.earnedAfterLoss" : "combat.xp.earned";
        VanillaText.addPara(textPanel, Translation.msg(earnedKey).arg("xp", report.xpText()).arg("dp", report.dpText()).styled(),
                Misc.getTextColor());
        if (report.hasDifficultyBonus()) {
            VanillaText.addPara(textPanel, Translation.msg("combat.xp.difficultyBonus").arg("percent", report.difficultyBonusText()).styled(),
                    Misc.getTextColor());
        }
        if (report.hasCatchUpBonus()) {
            VanillaText.addPara(textPanel, Translation.msg("combat.xp.catchUpBonus").arg("multiplier", report.catchUpText()).styled(),
                    Misc.getTextColor());
        }
        for (String levelUp : report.levelUps()) {
            VanillaText.addPara(textPanel, StyledText.of(levelUp), Misc.getPositiveHighlightColor());
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
