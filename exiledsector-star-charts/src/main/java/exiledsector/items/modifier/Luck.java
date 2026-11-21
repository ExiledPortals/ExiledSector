package exiledsector.items.modifier;

public record Luck(int luckyModifiers, boolean everyModifier, boolean luckyImplicits, int rolls) {

    public static final int BASE_ROLLS = 2;
    public static final Luck NONE = new Luck(0, false, false, BASE_ROLLS);
    public static final Luck ONE_MODIFIER = new Luck(1, false, false, BASE_ROLLS);

    public Luck {
        luckyModifiers = Math.max(0, luckyModifiers);
        rolls = Math.max(BASE_ROLLS, rolls);
    }

    public boolean isNone() {
        return luckyModifiers == 0 && !everyModifier && !luckyImplicits;
    }
}
