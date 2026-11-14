package exiledsector.charts.anomaly.relay;

import exiledsector.charts.officer.reward.RewardGroup;

public record RelayChoice(RewardGroup group, String scarId) {

    static final String FIELD_SEPARATOR = ":";

    String encoded() {
        return group.id() + FIELD_SEPARATOR + scarId;
    }

    static RelayChoice decode(String encoded) {
        int separator = encoded == null ? -1 : encoded.indexOf(FIELD_SEPARATOR);
        if (separator <= 0 || separator == encoded.length() - 1) {
            return null;
        }
        RewardGroup group = RewardGroup.byId(encoded.substring(0, separator));
        return group == null ? null : new RelayChoice(group, encoded.substring(separator + 1));
    }
}
