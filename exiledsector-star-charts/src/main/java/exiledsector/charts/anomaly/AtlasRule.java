package exiledsector.charts.anomaly;

import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasCatalystUse;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.items.UseVerdict;

public interface AtlasRule extends ChartAnomaly {

    static AtlasRule of(ChartAnomaly anomaly) {
        return anomaly instanceof AtlasRule rule ? rule : anomaly::id;
    }

    default float atlasWeight(AtlasTraits traits, float weight) {
        return weight;
    }

    default boolean fitsAt(AtlasTraits traits) {
        return true;
    }

    default boolean holdsAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return true;
    }

    default UseVerdict insertVerdict(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return fitsAt(generator.traits(cell)) && holdsAt(generator, state, cell) ? UseVerdict.ALLOW
                : UseVerdict.refused(AtlasCatalystUse.refusalKey(AtlasAnomalies.InsertCheck.DOES_NOT_FIT));
    }
}
