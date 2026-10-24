package exiledsector.charts.expansion;

import com.fs.starfarer.api.campaign.LocationAPI;
import exiledsector.charts.atlas.AtlasTraits;

public interface ChartExpansion {

    String id();

    void generate(ExpansionSite site);

    default float atlasWeight(AtlasTraits traits, float weight) {
        return weight;
    }

    default boolean isActiveIn(LocationAPI location) {
        return true;
    }
}
