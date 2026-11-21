package exiledsector.charts.anomaly.holdouts;

import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.AtlasRule;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasRunChoice;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.content.ChartFactions;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;

import java.util.List;

public final class Holdouts implements AtlasRule, RunParticipant, ContentPlacer {

    public static final String ANOMALY_ID = "holdouts";
    public static final Holdouts INSTANCE = new Holdouts();

    private Holdouts() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public float atlasWeight(AtlasTraits traits, float weight) {
        return 0f;
    }

    @Override
    public boolean derivedFromAtlas() {
        return true;
    }

    @Override
    public boolean holdsAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return holdoutFactionAt(generator, state, cell) != null;
    }

    public static String holdoutFactionAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        String owner = state.claimedFaction(cell);
        if (owner == null || !owner.equals(AtlasOpening.ownerOf(generator, state, cell))) {
            return null;
        }
        return generator.holdoutFactionAt(cell, owner);
    }

    @Override
    public List<AtlasLine> atlasLines(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        String holdoutFactionId = holdoutFactionAt(generator, state, cell);
        if (holdoutFactionId == null) {
            return List.of();
        }
        return List.of(new AtlasLine(Translation.msg("chart.atlas.node.holdouts")
                .arg("holdouts", ChartFactions.displayName(holdoutFactionId))
                .arg("owner", ChartFactions.displayName(state.claimedFaction(cell))).text(), AtlasLine.Tone.PLAIN));
    }

    @Override
    public AtlasRunNode joinRunNode(AtlasRunNode node, AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasRunChoice choice) {
        String holdoutFactionId = holdoutFactionAt(generator, state, cell);
        return holdoutFactionId == null ? node : node.withAnomalyData(ANOMALY_ID, holdoutFactionId);
    }

    @Override
    public String intelText(AnomalyRun run) {
        String holdoutFactionId = run.context().anomalyData(ANOMALY_ID);
        return holdoutFactionId == null ? null : I18n.forGameText(() -> Translation.msg("chart.anomaly.holdouts.intel")
                .arg("holdouts", ChartFactions.displayName(holdoutFactionId)).text());
    }

    @Override
    public void generate(AnomalyContentSite site) {
        String holdoutFactionId = site.context() == null ? null : site.context().anomalyData(ANOMALY_ID);
        if (holdoutFactionId != null && !holdoutFactionId.equals(site.factionId())) {
            HoldoutFleets.spawn(site, holdoutFactionId);
        }
    }
}
