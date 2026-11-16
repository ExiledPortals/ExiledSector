package exiledsector.charts.anomaly.excavation;

import exiledsector.charts.anomaly.StateFields;

public record ExcavationSite(int index, String planetId, Access access, long seed, boolean cleared) {

    static final int FIELDS = 4;
    static final int FIELDS_WITH_LEAN = 5;

    public enum Access {
        OPEN,
        COLLAPSED,
        BOTH
    }

    public ExcavationSite asCleared() {
        return new ExcavationSite(index, planetId, access, seed, true);
    }

    public boolean hasOpenShaft() {
        return !cleared && (access == Access.OPEN || access == Access.BOTH);
    }

    public boolean hasCollapsedShaft() {
        return !cleared && (access == Access.COLLAPSED || access == Access.BOTH);
    }

    public ExcavationSite withoutShaft(Access shaft) {
        if (access == Access.BOTH && shaft != Access.BOTH) {
            return new ExcavationSite(index, planetId, shaft == Access.OPEN ? Access.COLLAPSED : Access.OPEN, seed, cleared);
        }
        return access == shaft || shaft == Access.BOTH ? asCleared() : this;
    }

    String encode() {
        return StateFields.join(planetId, access.name(), StateFields.seed(seed), StateFields.flag(cleared));
    }

    static ExcavationSite decode(int index, String encoded) {
        ExcavationSite site = StateFields.decode(encoded, FIELDS, fields -> read(index, fields, 2));
        return site != null ? site : StateFields.decode(encoded, FIELDS_WITH_LEAN, fields -> read(index, fields, 3));
    }

    private static ExcavationSite read(int index, StateFields fields, int seedField) {
        return fields.text(0).isEmpty() ? null
                : new ExcavationSite(index, fields.text(0), fields.enumValue(1, Access.class), fields.seed(seedField), fields.flag(seedField + 1));
    }
}
