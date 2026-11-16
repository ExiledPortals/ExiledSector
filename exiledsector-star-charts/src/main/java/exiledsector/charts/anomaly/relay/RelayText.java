package exiledsector.charts.anomaly.relay;

import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.officer.reward.OfficerRewardText;
import exiledsector.charts.officer.scar.ScarText;
import exiledsector.charts.run.ChartRun;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.List;

final class RelayText {

    static final String PREFIX = "chart.anomaly.relay_network.";
    static final int LARGEST_WORDED_NUMBER = 6;

    private RelayText() {
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

    static String numberWord(int number) {
        if (number < 1 || number > LARGEST_WORDED_NUMBER) {
            return Integer.toString(number);
        }
        return Translation.gameText(PREFIX + "number." + number);
    }

    static String nextStep(ChartRun run, AtlasAnomalyRecord.Chain stage) {
        if (stage == null) {
            return null;
        }
        if (!stage.isLast()) {
            return "traced";
        }
        return run.isObjectiveComplete() || run.isObjectiveLost() ? null : "last";
    }

    static List<String> wokenMessages(ChartRun run, String relayName, AtlasAnomalyRecord.Chain stage) {
        List<String> messages = new ArrayList<>();
        int dormant = RelayStates.slots(run).size() - RelayStates.awake(run);
        if (dormant > 0) {
            messages.add(I18n.forGameText(() -> Translation.msg(PREFIX + "message.wokenDormant").arg("relay", relayName).count(dormant).text()));
            return messages;
        }
        String relays = numberWord(RelayStates.slots(run).size());
        messages.add(I18n.forGameText(() -> Translation.msg(PREFIX + "message.woken").arg("relay", relayName).text()));
        messages.add(I18n.forGameText(() -> Translation.msg(PREFIX + "message.allWoken").arg("relays", relays).text()));
        String step = nextStep(run, stage);
        if (step != null) {
            messages.add(stepText(PREFIX + "message." + step, stage, relays));
        }
        return messages;
    }

    static String status(ChartRun run, AtlasAnomalyRecord.Chain stage) {
        int awake = RelayStates.awake(run);
        String relays = numberWord(RelayStates.slots(run).size());
        if (awake == 0) {
            return I18n.forGameText(() -> Translation.msg(PREFIX + "intel").arg("relays", relays).text());
        }
        if (awake < RelayStates.slots(run).size()) {
            return I18n.forGameText(() -> Translation.msg(PREFIX + "intel.progress").count(awake).arg("relays", relays).text());
        }
        String step = nextStep(run, stage);
        return step == null ? I18n.forGameText(() -> Translation.msg(PREFIX + "intel.allWoken").arg("relays", relays).text())
                : stepText(PREFIX + "intel." + step, stage, relays);
    }

    static String choiceIntro(int offers) {
        String orders = numberWord(offers);
        return I18n.forGameText(() -> Translation.msg(PREFIX + "choice.intro").arg("orders", orders).text());
    }

    static StyledText offerLine(RelayChoice choice) {
        return I18n.forGameText(() -> Translation.msg(PREFIX + "choice.offer").arg("reward", OfficerRewardText.groupName(choice.group())).styled());
    }

    static StyledText pickedLine(RelayChoice choice) {
        return I18n.forGameText(() -> Translation.msg(PREFIX + "choice.picked").arg("reward", OfficerRewardText.groupName(choice.group())).styled());
    }

    static String optionText(RelayChoice choice) {
        String scars = String.join(Translation.gameText("format.list.sep"), choice.scarIds().stream().map(ScarText::name).toList());
        return I18n.forGameText(() -> Translation.msg(PREFIX + "option.choice").arg("reward", OfficerRewardText.groupName(choice.group()))
                .arg("scar", scars).text());
    }

    static String choiceMessage(String relayName) {
        return I18n.forGameText(() -> Translation.msg(PREFIX + "message.choice").arg("relay", relayName).text());
    }

    private static String stepText(String key, AtlasAnomalyRecord.Chain stage, String relays) {
        int next = stage.stage() + 1;
        int stages = stage.stages();
        return I18n.forGameText(() -> Translation.msg(key).arg("stage", next).arg("stages", stages).arg("relays", relays).text());
    }
}
