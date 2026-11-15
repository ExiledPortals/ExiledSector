package exiledsector.charts.layout.feature;

import com.fs.starfarer.api.campaign.SectorEntityToken;

public interface ChartFeature {

    String id();

    FeaturePlan plan(FeatureRolls rolls);

    default SectorEntityToken build(FeatureSite site) {
        return site.place(plan(site.rolls()));
    }
}
