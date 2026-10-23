package exiledsector.charts.expansion;

import com.fs.starfarer.api.campaign.LocationAPI;

public interface ChartExpansion {

    String id();

    void generate(ExpansionSite site);

    default boolean isActiveIn(LocationAPI location) {
        return true;
    }
}
