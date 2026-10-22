package exiledsector.ui.inspect;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.effects.NpcBonusScale;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledlib.skills.StatBaseline;
import exiledsector.skills.npc.NpcTreeTag;

import java.util.List;

public final class ShipTreeLookup {

    public record ShipTree(ShipSkillData skillData, List<String> buildThemes, float bonusScale, ShipVariantAPI variant,
                           StatBaseline statBaseline) {

        public ShipTree(ShipSkillData skillData, List<String> buildThemes) {
            this(skillData, buildThemes, 1f);
        }

        public ShipTree(ShipSkillData skillData, List<String> buildThemes, float bonusScale) {
            this(skillData, buildThemes, bonusScale, null, StatBaseline.NONE);
        }
    }

    private ShipTreeLookup() {
    }

    public static ShipTree find(FleetMemberAPI member) {
        if (member == null) {
            return null;
        }
        if (NpcTreeTag.find(member.getVariant()) != null) {
            return npcTree(member, member.getVariant());
        }
        if (!isInPlayerFleet(member)) {
            return null;
        }
        return forShip(member, member.getVariant());
    }

    public static ShipTree forShip(FleetMemberAPI member, ShipVariantAPI variant) {
        if (NpcTreeTag.find(variant) != null) {
            return npcTree(member, variant);
        }
        ShipSkillData skillData = member == null ? null : ShipSkillDataManager.find(member.getId());
        return skillData == null || skillData.isBlank() ? null
                : new ShipTree(skillData, List.of(), 1f, variant, StatBaseline.of(variant));
    }

    public static boolean isLevelledNpc(FleetMemberAPI member) {
        return member != null && NpcTreeTag.find(member.getVariant()) != null;
    }

    private static ShipTree npcTree(FleetMemberAPI member, ShipVariantAPI variant) {
        ShipSkillData skillData = SkillDataResolver.resolve(member, variant);
        return new ShipTree(skillData, NpcBuildLabel.mainThemes(skillData), NpcBonusScale.forShip(member, variant),
                variant, StatBaseline.of(variant));
    }

    private static boolean isInPlayerFleet(FleetMemberAPI member) {
        FleetDataAPI fleetData = member.getFleetData();
        CampaignFleetAPI fleet = fleetData == null ? null : fleetData.getFleet();
        return fleet != null && fleet.isPlayerFleet();
    }
}
