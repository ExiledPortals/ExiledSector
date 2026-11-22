package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.effects.ShipTreeSync;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTree;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipLevelSystem;

import java.util.List;

final class LevelShipStep implements HarnessStep {

    private final String shipSelector;
    private final int level;

    LevelShipStep(String shipSelector, int level) {
        if (level < 1) {
            throw new IllegalArgumentException("levelShip needs a level of at least 1, not " + level);
        }
        this.shipSelector = shipSelector;
        this.level = level;
    }

    @Override
    public String name() {
        return "levelShip " + shipSelector + " to " + level;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign(name());
        if (notInCampaign != null) {
            return notInCampaign;
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        FleetMemberAPI member = OpenSkillTreeStep.member(playerFleet, shipSelector);
        if (member == null) {
            return StepOutcome.failed("no fleet member matches \"" + shipSelector + "\"; use flagship, a fleet index, a hull id or a ship name");
        }
        int targetLevel = Math.min(level, ShipLevelConfig.maxLevel());
        ShipSkillData skillData = ShipSkillDataManager.get(member.getId());
        int levelBefore = skillData.getLevel();
        ShipTreeSync.fleetChanged(playerFleet);
        ShipLevelSystem.raiseToLevel(skillData, targetLevel, SkillTree.getAllNodes().values());
        ShipTreeSync.levelsChanged(playerFleet, List.of(member));
        return StepOutcome.passed(member.getShipName() + " went from level " + levelBefore + " to " + skillData.getLevel()
                + (targetLevel < level ? " (the level cap)" : ""));
    }
}
