package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
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

public final class CombatXpAward {

    private CombatXpAward() {
    }

    static void award(PlayerEngagement engagement) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            return;
        }
        float defeatedDp = engagement.enemyDeploymentPointsDefeated();
        boolean lost = !engagement.playerWon();
        float lossMultiplier = lost ? ShipLevelConfig.xpLossMultiplier() : 1f;
        float difficultyMultiplier = ShipLevelSystem.difficultyMultiplier(engagement.difficulty(),
                ShipLevelConfig.xpDifficultyStrength(), ShipLevelConfig.xpDifficultyMaxMultiplier());
        float xp = defeatedDp * ShipLevelConfig.xpPerDeploymentPoint() * lossMultiplier * difficultyMultiplier;

        Map<FleetMemberAPI, Integer> levelsBefore = levelsOf(playerFleet);
        float catchUpMultiplier = ShipLevelSystem.awardXpToFleet(playerFleet, xp);
        List<FleetMemberAPI> levelledMembers = levelledMembers(levelsBefore);
        ShipTreeSync.levelsChanged(playerFleet, levelledMembers);
        NpcBonusScale.invalidate();
        I18n.forGameText(() -> report(new CombatXpReport(xp, defeatedDp, lost, difficultyMultiplier, catchUpMultiplier,
                levelUpLines(levelledMembers))));
    }

    private static Map<FleetMemberAPI, Integer> levelsOf(CampaignFleetAPI fleet) {
        Map<FleetMemberAPI, Integer> levels = new LinkedHashMap<>();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            levels.put(member, ShipSkillDataManager.get(member.getId()).getLevel());
        }
        return levels;
    }

    private static List<FleetMemberAPI> levelledMembers(Map<FleetMemberAPI, Integer> levelsBefore) {
        List<FleetMemberAPI> levelledMembers = new ArrayList<>();
        levelsBefore.forEach((member, levelBefore) -> {
            if (ShipSkillDataManager.get(member.getId()).getLevel() > levelBefore) {
                levelledMembers.add(member);
            }
        });
        return levelledMembers;
    }

    private static List<String> levelUpLines(List<FleetMemberAPI> levelledMembers) {
        List<String> levelUpLines = new ArrayList<>();
        for (FleetMemberAPI levelledMember : levelledMembers) {
            int newLevel = ShipSkillDataManager.get(levelledMember.getId()).getLevel();
            levelUpLines.add(Translation.msg("combat.xp.levelUp").arg("ship", levelledMember.getShipName())
                    .arg("hull", levelledMember.getHullSpec().getHullNameWithDashClass()).arg("level", newLevel).text());
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
