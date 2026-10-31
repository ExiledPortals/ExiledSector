package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasCatalystUse;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.atlas.AtlasRunChoice;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.UseVerdict;
import exiledsector.ui.socket.BenchModeSupport;

import java.util.List;

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

    default List<AtlasLine> atlasLines(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return List.of();
    }

    default List<String> confirmLines(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return List.of();
    }

    default List<BenchModeSupport.OpenChoice> entryChoices(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return List.of();
    }

    default AtlasRunNode joinRunNode(AtlasRunNode node, AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasRunChoice choice) {
        return node;
    }

    default void runCompleted(SectorAPI sector, ChartRun run) {
    }

    default void runClosed(SectorAPI sector, ChartRun run) {
    }
}
