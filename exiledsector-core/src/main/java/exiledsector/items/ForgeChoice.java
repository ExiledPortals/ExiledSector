package exiledsector.items;

public record ForgeChoice(String part, RolledModifier modifier) {

    public static final ForgeChoice WHOLE_ITEM = new ForgeChoice(null, null);

    public static ForgeChoice part(String part) {
        return new ForgeChoice(part, null);
    }

    public static ForgeChoice modifier(RolledModifier modifier) {
        return new ForgeChoice(null, modifier);
    }
}
