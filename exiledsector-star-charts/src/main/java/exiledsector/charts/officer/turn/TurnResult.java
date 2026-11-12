package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.CommissionHandover.Handover;
import exiledsector.charts.officer.AtlasOfficer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record TurnResult(int turn, TurnCause cause, List<StandingChange> standingChanges, List<ResolvedIntent> resolvedIntents,
                         List<OfficerLoss> losses, List<Replacement> replacements, Map<Integer, AtlasOfficer> fallen) {

    public static final TurnResult NONE = new TurnResult(0, TurnCause.forced(), List.of(), List.of(), List.of(), List.of(), Map.of());
    static final String SEPARATOR = ",";
    static final String TURN_LINE = "turn";
    static final String STANDING_LINE = "standing";
    static final String INTENT_LINE = "intent";
    static final String LOSS_LINE = "loss";
    static final String REPLACEMENT_LINE = "replacement";
    static final String FALLEN_LINE = "fallen";

    public TurnResult {
        standingChanges = List.copyOf(standingChanges);
        resolvedIntents = List.copyOf(resolvedIntents);
        losses = List.copyOf(losses);
        replacements = List.copyOf(replacements);
        fallen = Map.copyOf(fallen);
    }

    public record Standing(String rankId, int renown) {

        public static Standing of(AtlasOfficer officer) {
            return new Standing(officer.rankId(), officer.renown());
        }
    }

    public record StandingChange(int recordId, Standing before, Standing after) {

        public boolean rankChanged() {
            return before != null && after != null && !before.rankId().equals(after.rankId());
        }
    }

    public record ResolvedIntent(IntentKind kind, int attackerRecordId, int defenderRecordId, AtlasCell target, boolean succeeded,
                                 boolean defended) {
    }

    public enum LossKind {
        BEATEN_BY_PLAYER,
        LOST_CHALLENGE
    }

    public record OfficerLoss(int recordId, LossKind kind, int winnerRecordId, AtlasCell cell, boolean fled, boolean died) {
    }

    public record Replacement(int fallenRecordId, int replacementRecordId) {
    }

    public TurnResult withHandovers(List<Handover> handovers) {
        Map<Integer, StandingChange> changes = new TreeMap<>();
        standingChanges.forEach(change -> changes.put(change.recordId(), change));
        List<Replacement> mergedReplacements = new ArrayList<>(replacements);
        Map<Integer, AtlasOfficer> mergedFallen = new LinkedHashMap<>(fallen);
        for (Handover handover : handovers) {
            AtlasOfficer predecessor = handover.predecessor();
            AtlasOfficer successor = handover.successor();
            StandingChange leaving = changes.get(predecessor.recordId());
            changes.put(predecessor.recordId(), new StandingChange(predecessor.recordId(),
                    leaving != null ? leaving.before() : Standing.of(predecessor), null));
            StandingChange joining = changes.get(successor.recordId());
            changes.put(successor.recordId(), new StandingChange(successor.recordId(), null,
                    joining != null ? joining.after() : Standing.of(successor)));
            mergedReplacements.add(new Replacement(predecessor.recordId(), successor.recordId()));
            mergedFallen.put(predecessor.recordId(), predecessor);
        }
        List<StandingChange> mergedChanges = changes.values().stream().filter(change -> !Objects.equals(change.before(), change.after())).toList();
        return new TurnResult(turn, cause, mergedChanges, resolvedIntents, losses, mergedReplacements, mergedFallen);
    }

    public List<StandingChange> rankChanges() {
        return standingChanges.stream().filter(StandingChange::rankChanged).toList();
    }

    public StandingChange standingChange(int recordId) {
        return standingChanges.stream().filter(change -> change.recordId() == recordId).findFirst().orElse(null);
    }

    public OfficerLoss lossOf(int recordId) {
        return losses.stream().filter(loss -> loss.recordId() == recordId).findFirst().orElse(null);
    }

    public List<String> encode() {
        List<String> lines = new ArrayList<>();
        lines.add(join(TURN_LINE, Integer.toString(turn), cause.kind().name(), Integer.toString(cause.officerRecordId())));
        for (StandingChange change : standingChanges) {
            lines.add(join(STANDING_LINE, Integer.toString(change.recordId()), rankOf(change.before()), renownOf(change.before()),
                    rankOf(change.after()), renownOf(change.after())));
        }
        for (ResolvedIntent intent : resolvedIntents) {
            lines.add(join(INTENT_LINE, intent.kind().id(), Integer.toString(intent.attackerRecordId()), Integer.toString(intent.defenderRecordId()),
                    cell(intent.target()), flag(intent.succeeded()), flag(intent.defended())));
        }
        for (OfficerLoss loss : losses) {
            lines.add(join(LOSS_LINE, Integer.toString(loss.recordId()), loss.kind().name(), Integer.toString(loss.winnerRecordId()), cell(loss.cell()),
                    flag(loss.fled()), flag(loss.died())));
        }
        for (Replacement replacement : replacements) {
            lines.add(join(REPLACEMENT_LINE, Integer.toString(replacement.fallenRecordId()), Integer.toString(replacement.replacementRecordId())));
        }
        fallen.keySet().stream().sorted().forEach(recordId -> lines.add(join(FALLEN_LINE, Integer.toString(recordId), fallen.get(recordId).encode())));
        return lines;
    }

    public static TurnResult decode(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return NONE;
        }
        try {
            String[] header = lines.get(0).split(SEPARATOR, -1);
            if (header.length != 4 || !TURN_LINE.equals(header[0])) {
                return NONE;
            }
            TurnCause cause = new TurnCause(TurnCause.Kind.valueOf(header[2]), Integer.parseInt(header[3]));
            List<StandingChange> standings = new ArrayList<>();
            List<ResolvedIntent> intents = new ArrayList<>();
            List<OfficerLoss> losses = new ArrayList<>();
            List<Replacement> replacements = new ArrayList<>();
            Map<Integer, AtlasOfficer> fallen = new LinkedHashMap<>();
            for (String line : lines.subList(1, lines.size())) {
                decodeLine(line, standings, intents, losses, replacements, fallen);
            }
            return new TurnResult(Integer.parseInt(header[1]), cause, standings, intents, losses, replacements, fallen);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return NONE;
        }
    }

    private static void decodeLine(String line, List<StandingChange> standings, List<ResolvedIntent> intents, List<OfficerLoss> losses,
                                   List<Replacement> replacements, Map<Integer, AtlasOfficer> fallen) {
        if (line.startsWith(FALLEN_LINE + SEPARATOR)) {
            String[] fields = line.split(SEPARATOR, 3);
            int recordId = Integer.parseInt(fields[1]);
            AtlasOfficer officer = AtlasOfficer.decode(recordId, fields[2]);
            if (officer != null) {
                fallen.put(recordId, officer);
            }
            return;
        }
        String[] fields = line.split(SEPARATOR, -1);
        switch (fields[0]) {
            case STANDING_LINE -> standings.add(new StandingChange(Integer.parseInt(fields[1]), standing(fields[2], fields[3]),
                    standing(fields[4], fields[5])));
            case INTENT_LINE -> intents.add(new ResolvedIntent(kind(fields[1]),
                    Integer.parseInt(fields[2]), Integer.parseInt(fields[3]), uncell(fields[4]), unflag(fields[5]), unflag(fields[6])));
            case LOSS_LINE -> losses.add(new OfficerLoss(Integer.parseInt(fields[1]), LossKind.valueOf(fields[2]), Integer.parseInt(fields[3]),
                    uncell(fields[4]), unflag(fields[5]), unflag(fields[6])));
            case REPLACEMENT_LINE -> replacements.add(new Replacement(Integer.parseInt(fields[1]), Integer.parseInt(fields[2])));
            default -> {
            }
        }
    }

    private static IntentKind kind(String id) {
        IntentKind kind = IntentKind.fromId(id);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown intent kind: " + id);
        }
        return kind;
    }

    private static String join(String... fields) {
        return String.join(SEPARATOR, fields);
    }

    private static String rankOf(Standing standing) {
        return standing == null ? "" : standing.rankId();
    }

    private static String renownOf(Standing standing) {
        return standing == null ? "" : Integer.toString(standing.renown());
    }

    private static Standing standing(String rankId, String renown) {
        return rankId.isEmpty() ? null : new Standing(rankId, Integer.parseInt(renown));
    }

    private static String cell(AtlasCell cell) {
        return cell == null ? "" : Long.toString(cell.pack());
    }

    private static AtlasCell uncell(String text) {
        return text.isEmpty() ? null : AtlasCell.unpack(Long.parseLong(text));
    }

    private static String flag(boolean value) {
        return value ? "1" : "0";
    }

    private static boolean unflag(String text) {
        return "1".equals(text);
    }
}
