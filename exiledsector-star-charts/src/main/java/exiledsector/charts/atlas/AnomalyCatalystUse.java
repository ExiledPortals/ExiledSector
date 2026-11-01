package exiledsector.charts.atlas;

import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AtlasRule;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.items.UseVerdict;

import java.util.Map;
import java.util.Set;

public record AnomalyCatalystUse(AtlasCatalysts.Catalyst catalyst, AtlasGenerator generator, AtlasState state, AtlasCell openRunCell,
                                 Set<AtlasCell> revealed) implements AtlasCatalystUse {

    public AnomalyCatalystUse {
        revealed = Set.copyOf(revealed);
    }

    @Override
    public UseVerdict check(AtlasCell cell) {
        String anomalyId = catalyst.anomalyId();
        if (!revealed.contains(cell)) {
            return UseVerdict.refused(FOGGED_KEY);
        }
        AtlasAnomalies.InsertCheck check = AtlasAnomalies.canInsert(generator, state, cell, anomalyId, openRunCell, forcesStar());
        if (check.allows()) {
            return UseVerdict.ALLOW;
        }
        UseVerdict refused = (check == AtlasAnomalies.InsertCheck.DOES_NOT_FIT
                ? AtlasRule.of(ChartAnomalies.byId(anomalyId)).insertVerdict(generator, state, cell)
                : UseVerdict.refused(AtlasCatalystUse.refusalKey(check))).with("anomaly", AtlasNames.anomaly(anomalyId));
        if (check == AtlasAnomalies.InsertCheck.CONFLICT) {
            String other = AtlasAnomalies.effective(generator, state, cell).stream()
                    .filter(existing -> ChartAnomalies.conflicts(existing, anomalyId)).findFirst().orElse("");
            refused = refused.with("other", AtlasNames.anomaly(other));
        }
        return refused;
    }

    @Override
    public Outcome apply(AtlasCell cell) {
        AtlasAnomalies.Insertion insertion = AtlasAnomalies.insert(generator, state, cell, catalyst.anomalyId(), AtlasAnomalyRecord.Source.PLAYER,
                openRunCell, null, forcesStar());
        return insertion.check().allows() ? new Outcome("catalyst.window.used", Map.of("anomaly", anomalyName())) : null;
    }

    @Override
    public String usableText() {
        return Translation.msg("catalyst.target.usable").arg("anomaly", anomalyName()).text();
    }

    @Override
    public String confirmBody(String systemName) {
        return Translation.msg("catalyst.confirm.body").arg("anomaly", anomalyName()).arg("system", systemName).text();
    }

    private boolean forcesStar() {
        return catalyst.effect() == AtlasCatalysts.Effect.STAR;
    }

    private String anomalyName() {
        return AtlasNames.anomaly(catalyst.anomalyId());
    }
}
