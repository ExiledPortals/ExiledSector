package exiledsector.charts.anomaly.excavation;

import exiledsector.charts.anomaly.StateFields;
import exiledsector.charts.loot.DropType;

public record ExcavationSite(int index, String planetId, Access access, DropType lean, long seed, boolean cleared) {

    static final int FIELDS = 5;

    public enum Access {
        OPEN,
        COLLAPSED
    }

    public ExcavationSite asCleared() {
        return new ExcavationSite(index, planetId, access, lean, seed, true);
    }

    String encode() {
        return StateFields.join(planetId, access.name(), lean.name(), StateFields.seed(seed), StateFields.flag(cleared));
    }

    static ExcavationSite decode(int index, String encoded) {
        return StateFields.decode(encoded, FIELDS, fields -> fields.text(0).isEmpty() ? null
                : new ExcavationSite(index, fields.text(0), fields.enumValue(1, Access.class), fields.enumValue(2, DropType.class), fields.seed(3),
                fields.flag(4)));
    }
}
