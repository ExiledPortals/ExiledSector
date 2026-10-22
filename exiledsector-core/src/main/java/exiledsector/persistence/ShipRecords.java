package exiledsector.persistence;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ExiledSector;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.skills.ShipSkillData;

public final class ShipRecords {

    public static final String SHIP_TAG_PREFIX = "exiledSector_ship_";

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
            String shipId = taggedShipId(variant);
            return shipId == null ? null : ShipSkillDataManager.find(shipId);
        }
        ShipSkillData saved = ShipSkillDataManager.find(member.getId());
        return saved != null ? saved : new ShipSkillData();
    }

    static String taggedShipId(ShipVariantAPI variant) {
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
