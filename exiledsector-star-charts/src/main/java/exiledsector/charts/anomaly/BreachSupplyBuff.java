package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.BuffManagerAPI.Buff;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public class BreachSupplyBuff implements Buff {

    private final String id;
    private final float percent;
    private final String source;
    private float daysLeft;

    public BreachSupplyBuff(String id, float percent, String source, float days) {
        this.id = id;
        this.percent = percent;
        this.source = source;
        this.daysLeft = days;
    }

    @Override
    public void apply(FleetMemberAPI member) {
        if (member != null && member.getStats() != null) {
            member.getStats().getSuppliesPerMonth().modifyPercent(id, percent, source);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isExpired() {
        return daysLeft <= 0f;
    }

    @Override
    public void advance(float days) {
        daysLeft -= days;
    }

    float percent() {
        return percent;
    }
}
