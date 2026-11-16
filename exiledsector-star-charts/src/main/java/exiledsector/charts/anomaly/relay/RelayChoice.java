package exiledsector.charts.anomaly.relay;

import exiledsector.charts.officer.reward.RewardGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record RelayChoice(RewardGroup group, String scarId, List<String> extraScarIds) {

    static final String FIELD_SEPARATOR = ":";

    public RelayChoice {
        extraScarIds = extraScarIds == null ? List.of() : List.copyOf(extraScarIds);
    }

    public RelayChoice(RewardGroup group, String scarId) {
        this(group, scarId, List.of());
    }

    public List<String> scarIds() {
        List<String> scarIds = new ArrayList<>();
        scarIds.add(scarId);
        scarIds.addAll(extraScarIds);
        return List.copyOf(scarIds);
    }

    String encoded() {
        return group.id() + FIELD_SEPARATOR + String.join(FIELD_SEPARATOR, scarIds());
    }

    static RelayChoice decode(String encoded) {
        String[] fields = encoded == null ? new String[0] : encoded.split(FIELD_SEPARATOR, -1);
        if (fields.length < 2 || fields[0].isEmpty() || Arrays.stream(fields).skip(1).anyMatch(String::isEmpty)) {
            return null;
        }
        RewardGroup group = RewardGroup.byId(fields[0]);
        return group == null ? null : new RelayChoice(group, fields[1], List.of(fields).subList(2, fields.length));
    }
}
