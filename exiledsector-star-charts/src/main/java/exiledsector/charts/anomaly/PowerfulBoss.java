package exiledsector.charts.anomaly;

public final class PowerfulBoss implements LootShaper {

    public static final String ANOMALY_ID = "powerful_boss";
    public static final PowerfulBoss INSTANCE = new PowerfulBoss();

    private PowerfulBoss() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public boolean guaranteesBossTierUp() {
        return true;
    }
}
