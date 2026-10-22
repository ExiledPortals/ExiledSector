package exiledsector.ui;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipLevelSystem;

final class SkillTreeLevelBar {

    private final FleetMemberAPI member;
    private final SkillTreeReadoutBar readoutBar;

    private int shownLevel = -1;
    private int shownBankedFreeAllocations = -1;
    private int shownXp = -1;
    private int xpToNextLevel;
    private String levelLabel;

    SkillTreeLevelBar(FleetMemberAPI member, int row) {
        this.member = member;
        this.readoutBar = new SkillTreeReadoutBar(SkillTreeLevelBar.class, row);
    }

    void advance(float amount, PositionAPI canvasPosition, float mouseX, float mouseY, boolean mouseKnown) {
        refreshIfChanged();
        readoutBar.advance(amount, canvasPosition, shownXp, xpToNextLevel, mouseX, mouseY, mouseKnown);
    }

    private void refreshIfChanged() {
        ShipSkillData skillData = ShipSkillDataManager.get(member.getId());
        int latestXp = Math.round(skillData.getXp());
        if (skillData.getLevel() == shownLevel && skillData.getBankedFreeAllocations() == shownBankedFreeAllocations && latestXp == shownXp) {
            return;
        }
        shownLevel = skillData.getLevel();
        shownBankedFreeAllocations = skillData.getBankedFreeAllocations();
        shownXp = latestXp;
        int maxLevel = ShipLevelConfig.maxLevel();
        xpToNextLevel = shownLevel >= maxLevel ? Math.max(shownXp, 1)
                : Math.round(ShipLevelSystem.xpToReachNextLevel(shownLevel, ShipLevelConfig.xpBase(), ShipLevelConfig.xpGrowth(),
                        ShipLevelConfig.xpGrowthCutoffLevel()));
        levelLabel = label();
    }

    void render(PositionAPI canvasPosition, float alphaMult) {
        if (levelLabel == null) {
            refreshIfChanged();
        }
        readoutBar.render(canvasPosition, alphaMult, levelLabel);
    }

    private String label() {
        if (shownBankedFreeAllocations > 0) {
            return Translation.msg("ui.levelBar.levelWithFree").arg("level", shownLevel).arg("free", shownBankedFreeAllocations).text();
        }
        return Translation.msg("ui.levelBar.level").arg("level", shownLevel).text();
    }

    boolean isHovered(PositionAPI canvasPosition, float x, float y) {
        return readoutBar.isHovered(canvasPosition, x, y);
    }
}
