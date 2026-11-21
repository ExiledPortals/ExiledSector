package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;

public record Intent(int officerRecordId, IntentKind kind, AtlasCell target, int targetRecordId) {

    public static final int NO_OFFICER = 0;
    static final String SEPARATOR = ",";
    private static final int FIELD_COUNT = 4;

    public static Intent expand(int officerRecordId, AtlasCell cell) {
        return new Intent(officerRecordId, IntentKind.EXPAND, cell, NO_OFFICER);
    }

    public static Intent push(int officerRecordId, AtlasCell cell, int defenderRecordId) {
        return new Intent(officerRecordId, IntentKind.PUSH, cell, defenderRecordId);
    }

    public static Intent challenge(int officerRecordId, int defenderRecordId) {
        return new Intent(officerRecordId, IntentKind.CHALLENGE, null, defenderRecordId);
    }

    public static Intent defend(int officerRecordId) {
        return new Intent(officerRecordId, IntentKind.DEFEND, null, NO_OFFICER);
    }

    public static Intent hold(int officerRecordId) {
        return new Intent(officerRecordId, IntentKind.HOLD, null, NO_OFFICER);
    }

    public boolean targets(int recordId) {
        return kind.isConflict() && targetRecordId == recordId;
    }

    public String encode() {
        return String.join(SEPARATOR, kind.id(), Integer.toString(officerRecordId), target == null ? "" : Long.toString(target.pack()),
                Integer.toString(targetRecordId));
    }

    public static Intent decode(String line) {
        String[] fields = line == null ? new String[0] : line.split(SEPARATOR, -1);
        IntentKind kind = fields.length == FIELD_COUNT ? IntentKind.fromId(fields[0]) : null;
        if (kind == null) {
            return null;
        }
        try {
            return new Intent(Integer.parseInt(fields[1]), kind, fields[2].isEmpty() ? null : AtlasCell.unpack(Long.parseLong(fields[2])),
                    Integer.parseInt(fields[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
