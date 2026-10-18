package exiledsector.charts;

public record ChartFleetSpread(int fleets, float fleetSize, int strengthBonus) {

    static final float BLEED_TO_SIZE = 0.75f;
    static final float BLEED_TO_STRENGTH = 0.25f;

    public static ChartFleetSpread of(int rolledFleets, float tierFleetSize, int extraFleets, int fleetLimit) {
        int rolled = Math.max(1, rolledFleets);
        int kept = Math.min(Math.max(1, fleetLimit), rolled);
        float cutShare = (float) (rolled - kept) / kept;
        int fleets = Math.max(1, Math.min(ChartContentPlanner.MAX_FLEETS, kept + extraFleets));
        float baseSize = Math.max(0f, tierFleetSize);
        int strengthBonus = Math.min(ChartContentPlanner.MAX_OVERFLOW_STRENGTH, Math.round(BLEED_TO_STRENGTH * cutShare * ChartTier.PERCENT));
        return new ChartFleetSpread(fleets, baseSize + BLEED_TO_SIZE * cutShare * baseSize, strengthBonus);
    }

    public float cappedFleetSize() {
        return Math.min(ChartContentPlanner.FLEET_SIZE_CAP, fleetSize);
    }

    public int totalStrengthBonus() {
        return Math.min(ChartContentPlanner.MAX_OVERFLOW_STRENGTH,
                strengthBonus + ChartContentPlanner.overflowStrength(fleetSize, ChartContentPlanner.FLEET_SIZE_CAP));
    }
}
