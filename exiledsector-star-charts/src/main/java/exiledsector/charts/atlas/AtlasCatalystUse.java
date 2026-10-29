package exiledsector.charts.atlas;

import exiledsector.i18n.Message;
import exiledsector.i18n.Translation;
import exiledsector.items.Consumable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public sealed interface AtlasCatalystUse extends Consumable<AtlasCell, AtlasCatalystUse.Outcome> permits AnomalyCatalystUse, RedeploymentCatalystUse {

    String FOGGED_KEY = "catalyst.refused.fogged";

    record Outcome(String noticeKey, Map<String, String> args) {

        public Outcome {
            args = Map.copyOf(args);
        }

        public String text(String systemName) {
            Message message = Translation.msg(noticeKey).arg("system", systemName);
            args.forEach(message::arg);
            return message.text();
        }
    }

    static AtlasCatalystUse of(AtlasCatalysts.Catalyst catalyst, AtlasGenerator generator, AtlasState state, AtlasCell openRunCell,
                               Set<AtlasCell> revealed) {
        return catalyst.effect() == AtlasCatalysts.Effect.REDEPLOY
                ? new RedeploymentCatalystUse(catalyst, generator, state, openRunCell, revealed)
                : new AnomalyCatalystUse(catalyst, generator, state, openRunCell, revealed);
    }

    static String refusalKey(AtlasAnomalies.InsertCheck check) {
        return "catalyst.refused." + check.name().toLowerCase(Locale.ROOT);
    }

    static String anomalyName(String anomalyId) {
        return Translation.text("chart.atlas.anomaly." + anomalyId);
    }

    AtlasCatalysts.Catalyst catalyst();

    String usableText();

    String confirmBody(String systemName);

    @Override
    default String itemId() {
        return catalyst().itemId();
    }
}
