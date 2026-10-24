package exiledsector.charts.feature;

import com.fs.starfarer.api.campaign.SectorEntityToken;

public interface ChartFeature {

    String id();

    SectorEntityToken build(FeatureSite site);
}
