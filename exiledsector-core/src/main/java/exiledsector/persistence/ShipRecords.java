package exiledsector.persistence;

import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ExiledSector;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.skills.ShipSkillData;

import java.util.Map;
import java.util.WeakHashMap;

public final class ShipRecords {

    public static final String SHIP_TAG_PREFIX = "exiledSector_ship_";
    private static final Map<FleetMemberAPI, ShipSkillData> EMPTY_RECORDS = new WeakHashMap<>();

    private ShipRecords() {
    }

    public static ShipSkillData resolve(FleetMemberAPI member, ShipVariantAPI variant) {
        for (ExiledSectorDomain domain : ExiledSector.domains()) {
            ShipSkillData domainRecord = domain.shipRecord(variant);
            if (domainRecord != null) {
                return domainRecord;
            }
        }
        if (member == null) {
            return taggedTree(variant);
        }
        ShipSkillData saved = ShipSkillDataManager.find(member.getId());
        if (saved != null) {
            return saved;
        }
        ShipSkillData tagged = taggedTree(variant);
        return tagged != null && isOutsideFleet(member) ? tagged : EMPTY_RECORDS.computeIfAbsent(member, ignored -> new ShipSkillData());
    }

    public static ShipSkillData taggedTree(ShipVariantAPI variant) {
        String shipId = taggedShipId(variant);
        return shipId == null ? null : ShipSkillDataManager.find(shipId);
    }

    public static boolean savedTreeOutOfReach(FleetMemberAPI member, ShipVariantAPI variant) {
        return member != null && member.getId().equals(taggedShipId(variant)) && ShipSkillDataManager.find(member.getId()) == null;
    }

    public static boolean isOutsideFleet(FleetMemberAPI member) {
        FleetDataAPI fleetData = member.getFleetData();
        return fleetData == null || !fleetData.getMembersListCopy().contains(member);
    }

    public static String taggedShipId(ShipVariantAPI variant) {
        if (variant == null) {
            return null;
        }
        for (String tag : variant.getTags()) {
            if (tag.startsWith(SHIP_TAG_PREFIX)) {
                return tag.substring(SHIP_TAG_PREFIX.length());
            }
        }
        return null;
    }
}
