package exiledsector.charts.atlas;

import exiledlib.i18n.Translation;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.items.Consumable;
import exiledsector.items.UseVerdict;

import java.util.Locale;
import java.util.Set;

public record AtlasCatalystUse(AtlasCatalysts.Catalyst catalyst, AtlasGenerator generator, AtlasState state, AtlasCell openRunCell,
                               Set<AtlasCell> revealed) implements Consumable<AtlasCell, AtlasAnomalies.Insertion> {

    public static final String FOGGED_KEY = "catalyst.refused.fogged";

    public AtlasCatalystUse {
        revealed = Set.copyOf(revealed);
    }

    public static String refusalKey(AtlasAnomalies.InsertCheck check) {
        return "catalyst.refused." + check.name().toLowerCase(Locale.ROOT);
    }

    public static String anomalyName(String anomalyId) {
        return Translation.text("chart.atlas.anomaly." + anomalyId);
    }

    @Override
    public String itemId() {
        return catalyst.itemId();
    }

    @Override
    public UseVerdict check(AtlasCell cell) {
        String anomalyId = catalyst.anomalyId();
        if (!revealed.contains(cell)) {
            return UseVerdict.refused(FOGGED_KEY);
        }
        AtlasAnomalies.InsertCheck check = AtlasAnomalies.canInsert(generator, state, cell, anomalyId, openRunCell);
        if (check.allows()) {
            return UseVerdict.ALLOWED;
        }
        UseVerdict refused = UseVerdict.refused(refusalKey(check)).with("anomaly", anomalyName(anomalyId));
        if (check == AtlasAnomalies.InsertCheck.CONFLICT) {
            String other = AtlasAnomalies.effective(generator, state, cell).stream()
                    .filter(existing -> ChartAnomalies.conflicts(existing, anomalyId)).findFirst().orElse("");
            refused = refused.with("other", anomalyName(other));
        }
        return refused;
    }

    @Override
    public AtlasAnomalies.Insertion apply(AtlasCell cell) {
        AtlasAnomalies.Insertion insertion = AtlasAnomalies.insert(generator, state, cell, catalyst.anomalyId(), AtlasAnomalyRecord.Source.PLAYER,
                openRunCell);
        return insertion.check().allows() ? insertion : null;
    }
}
