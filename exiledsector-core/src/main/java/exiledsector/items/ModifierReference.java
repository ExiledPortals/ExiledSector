package exiledsector.items;

public record ModifierReference(String id, String weightText, String tierText, boolean wellFormed) {

    static final String FIELD_SEPARATOR = ":";

    public static ModifierReference parse(String entryText) {
        String[] fields = entryText.trim().substring(ItemModifiers.REFERENCE_MARK.length()).split(FIELD_SEPARATOR, -1);
        boolean tierGated = fields.length >= 2 && PoolEntry.isTierRangeText(fields[fields.length - 1]);
        int weightFields = fields.length - 1 - (tierGated ? 1 : 0);
        boolean wellFormed = weightFields <= 1 && fields.length <= 3;
        return new ModifierReference(fields[0].trim(), weightFields == 1 ? fields[1] : null, tierGated ? fields[fields.length - 1] : null,
                wellFormed);
    }

    public boolean hasShape(boolean allowWeight, boolean allowTier) {
        return wellFormed && (allowWeight || weightText == null) && (allowTier || tierText == null);
    }
}
