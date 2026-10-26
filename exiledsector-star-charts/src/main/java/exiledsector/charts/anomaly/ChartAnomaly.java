package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.atlas.AtlasTraits;

public interface ChartAnomaly {

    String id();

    default String primaryStar() {
        return null;
    }

    default StarSystemType systemType() {
        return null;
    }

    default boolean replacesStar() {
        return primaryStar() != null || systemType() != null;
    }

    default int minPlanets() {
        return 0;
    }

    default void shape(AnomalyLayoutSite site) {
    }

    default void generate(AnomalyContentSite site) {
    }

    default float atlasWeight(AtlasTraits traits, float weight) {
        return weight;
    }

    default boolean fitsAt(AtlasTraits traits) {
        return true;
    }

    default boolean isActiveIn(LocationAPI location) {
        return true;
    }

    static StarSystemType systemTypeOr(ChartAnomaly anomaly, StarSystemType fallback) {
        return anomaly != null && anomaly.systemType() != null ? anomaly.systemType() : fallback;
    }
}
