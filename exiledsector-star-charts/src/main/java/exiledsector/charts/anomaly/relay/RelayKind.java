package exiledsector.charts.anomaly.relay;

import exiledsector.data.EnumKeys;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Entities;

public enum RelayKind {
    NAV_BUOY(Entities.NAV_BUOY, true),
    COMM_RELAY(Entities.COMM_RELAY, false),
    SENSOR_ARRAY(Entities.SENSOR_ARRAY, true);

    private final String entityType;
    private final boolean vanillaBonus;

    RelayKind(String entityType, boolean vanillaBonus) {
        this.entityType = entityType;
        this.vanillaBonus = vanillaBonus;
    }

    public String entityType() {
        return entityType;
    }

    public boolean givesVanillaBonus() {
        return vanillaBonus;
    }

    public String key() {
        return EnumKeys.key(this);
    }

    public static RelayKind parse(String key) {
        return EnumKeys.parse(RelayKind.class, key);
    }

    public static RelayKind of(SectorEntityToken entity) {
        if (entity == null || !entity.hasTag(RelayNetwork.RELAY_TAG) || entity.getMemoryWithoutUpdate() == null) {
            return null;
        }
        return parse(entity.getMemoryWithoutUpdate().getString(RelayNetwork.KIND_MEMORY_KEY));
    }
}
