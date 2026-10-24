package exiledsector.persistence;

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
        ShipSkillData borrowed = member.getFleetData() == null ? taggedTree(variant) : null;
        return borrowed != null ? borrowed : EMPTY_RECORDS.computeIfAbsent(member, ignored -> new ShipSkillData());
    }

    public static ShipSkillData taggedTree(ShipVariantAPI variant) {
        String shipId = taggedShipId(variant);
        return shipId == null ? null : ShipSkillDataManager.find(shipId);
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
