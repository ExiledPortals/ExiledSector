package exiledsector.charts.anomaly.relay;

import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.run.ChartRun;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.List;

final class RelayText {

    static final String PREFIX = "chart.anomaly.relay_network.";

    private RelayText() {
    }

    static int awake(ChartRun run) {
        int awake = 0;
        for (RelayKind kind : RelayKind.values()) {
            if (RelayStates.isWoken(run, kind)) {
                awake++;
            }
        }
        return awake;
    }

    static String part(AtlasAnomalyRecord.Chain stage) {
        if (stage == null || stage.stage() <= 1) {
            return "first";
        }
        return stage.isLast() ? "last" : "middle";
    }

    static String trailKey(AtlasAnomalyRecord.Chain stage) {
        return PREFIX + "trail." + part(stage);
    }

    static String logKey(RelayKind kind, AtlasAnomalyRecord.Chain stage) {
        return PREFIX + "log." + part(stage) + "." + kind.key();
    }

    static String nextStep(ChartRun run, AtlasAnomalyRecord.Chain stage) {
        if (stage == null || run.isObjectiveLost()) {
            return null;
        }
        if (stage.isLast()) {
            return run.isObjectiveComplete() ? null : "last";
        }
        return run.isObjectiveComplete() ? "traced" : "next";
    }

    static List<String> wokenMessages(ChartRun run, String relayName, AtlasAnomalyRecord.Chain stage) {
        List<String> messages = new ArrayList<>();
        int dormant = RelayKind.values().length - awake(run);
        if (dormant > 0) {
            messages.add(I18n.forGameText(() -> Translation.msg(PREFIX + "message.wokenDormant").arg("relay", relayName).count(dormant).text()));
            return messages;
        }
        messages.add(I18n.forGameText(() -> Translation.msg(PREFIX + "message.woken").arg("relay", relayName).text()));
        messages.add(Translation.gameText(PREFIX + "message.allWoken"));
        String step = nextStep(run, stage);
        if (step != null) {
            messages.add(stepText(PREFIX + "message." + step, stage));
        }
        return messages;
    }

    static String status(ChartRun run, AtlasAnomalyRecord.Chain stage) {
        int awake = awake(run);
        if (awake == 0) {
            return Translation.gameText(PREFIX + "intel");
        }
        if (awake < RelayKind.values().length) {
            return I18n.forGameText(() -> Translation.msg(PREFIX + "intel.progress").count(awake).text());
        }
        String step = nextStep(run, stage);
        return step == null ? Translation.gameText(PREFIX + "intel.allWoken") : stepText(PREFIX + "intel." + step, stage);
    }

    private static String stepText(String key, AtlasAnomalyRecord.Chain stage) {
        int next = stage.stage() + 1;
        int stages = stage.stages();
        return I18n.forGameText(() -> Translation.msg(key).arg("stage", next).arg("stages", stages).text());
    }
}
