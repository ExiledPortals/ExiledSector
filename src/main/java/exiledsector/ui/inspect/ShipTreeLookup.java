package exiledsector.ui.inspect;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.npc.NpcTreeTag;

import java.util.List;

public final class ShipTreeLookup {

    public record ShipTree(ShipSkillData data, List<String> buildThemes) {
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
        ShipSkillData data = ShipSkillDataManager.get(member.getId());
        return data.isBlank() ? null : new ShipTree(data, List.of());
    }

    public static ShipTree forShip(FleetMemberAPI member, ShipVariantAPI variant) {
        if (NpcTreeTag.find(variant) != null) {
            return npcTree(member, variant);
        }
        ShipSkillData data = member == null ? null : ShipSkillDataManager.find(member.getId());
        return data == null || data.isBlank() ? null : new ShipTree(data, List.of());
    }

    public static boolean isLevelledNpc(FleetMemberAPI member) {
        return member != null && NpcTreeTag.find(member.getVariant()) != null;
    }

    private static ShipTree npcTree(FleetMemberAPI member, ShipVariantAPI variant) {
        ShipSkillData data = SkillDataResolver.resolve(member, variant);
        return new ShipTree(data, NpcBuildLabel.mainThemes(data));
    }

    private static boolean isInPlayerFleet(FleetMemberAPI member) {
        FleetDataAPI fleetData = member.getFleetData();
        CampaignFleetAPI fleet = fleetData == null ? null : fleetData.getFleet();
        return fleet != null && fleet.isPlayerFleet();
    }
}
