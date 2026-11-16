package exiledsector.charts.anomaly.excavation;

public record ExcavationGuardians(boolean secondCavern, boolean corrupted) {

    public static final ExcavationGuardians FIRST_CAVERN = new ExcavationGuardians(false, false);

    public ExcavationGuardians asCorrupted() {
        return new ExcavationGuardians(secondCavern, true);
    }
}
