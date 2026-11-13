package exiledsector.charts.content;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class ChartOfficerLevels {

    static final String OFFICER_LEVEL_CAP_SETTING = "officerMaxLevel";
    static final int ELITE_SKILL_LEVEL = 2;

    private ChartOfficerLevels() {
    }

    static void grantFleetEliteSkills(CampaignFleetAPI fleet, int officerLevelBonus, Random random) {
        if (fleet == null || officerLevelBonus <= 0 || fleet.getFleetData() == null) {
            return;
        }
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            PersonAPI captain = member.getCaptain();
            if (captain != null && !captain.isDefault()) {
                grantBonusEliteSkills(captain, officerLevelBonus, random);
            }
        }
    }

    static int stationCommanderLevel(int officerLevel) {
        return Math.min(officerLevel, officerLevelCap());
    }

    static void grantStationEliteSkills(PersonAPI commander, int officerLevel, Random random) {
        if (commander != null) {
            grantBonusEliteSkills(commander, officerLevel - officerLevelCap(), random);
        }
    }

    static int grantBonusEliteSkills(PersonAPI person, int bonusLevels, Random random) {
        if (person.isAICore()) {
            return 0;
        }
        return grantEliteSkills(person, bonusLevels, random);
    }

    static int grantEliteSkills(PersonAPI person, int count, Random random) {
        List<String> eligible = eliteEligibleSkills(person);
        int granted = 0;
        while (granted < count && !eligible.isEmpty()) {
            person.getStats().increaseSkill(eligible.remove(random.nextInt(eligible.size())));
            granted++;
        }
        return granted;
    }

    static List<String> eliteEligibleSkills(PersonAPI person) {
        List<String> eligible = new ArrayList<>();
        for (SkillLevelAPI skillLevel : person.getStats().getSkillsCopy()) {
            SkillSpecAPI skill = skillLevel.getSkill();
            if (skillLevel.getLevel() > 0 && skillLevel.getLevel() < ELITE_SKILL_LEVEL && skill.isCombatOfficerSkill()
                    && !skill.isAptitudeEffect() && !skill.hasTag(Skills.TAG_ELITE_PLAYER_ONLY)) {
                eligible.add(skill.getId());
            }
        }
        return eligible;
    }

    static int officerLevelCap() {
        return Global.getSettings().getInt(OFFICER_LEVEL_CAP_SETTING);
    }
}
