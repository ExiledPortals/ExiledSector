package exiledsector.charts.anomaly;

public final class PowerfulBoss implements ChartAnomaly {

    public static final String ID = "powerful_boss";
    public static final PowerfulBoss INSTANCE = new PowerfulBoss();

    private PowerfulBoss() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean guaranteesBossTierUp() {
        return true;
    }
}
