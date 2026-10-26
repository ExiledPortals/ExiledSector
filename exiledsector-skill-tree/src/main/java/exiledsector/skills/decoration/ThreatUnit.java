package exiledsector.skills.decoration;

import java.util.List;

public class ThreatUnit extends Decoration {

    public static final String FABRICATOR = "fabricator";
    public static final String HIVE = "hive";

    private final String unitType;
    private final float hullLength;
    private final float rotationDegrees;

    public ThreatUnit(String id, float x, float y, String unitType, float hullLength, float rotationDegrees, List<String> tags) {
        super(id, x, y, tags);
        this.unitType = unitType;
        this.hullLength = hullLength;
        this.rotationDegrees = rotationDegrees;
    }

    @Override
    public DecorationKind kind() {
        return DecorationKind.THREAT_UNIT;
    }

    public String getUnitType() {
        return unitType;
    }

    public float getLength() {
        return hullLength;
    }

    public float getRotation() {
        return rotationDegrees;
    }
}
