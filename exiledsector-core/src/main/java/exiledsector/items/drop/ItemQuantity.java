package exiledsector.items.drop;

public record ItemQuantity(float increasedPercent, float share) {

    public static final float PERCENT = 100f;
    public static final ItemQuantity BASE = new ItemQuantity(0f, 1f);

    public ItemQuantity {
        share = Math.max(0f, share);
    }

    public static ItemQuantity increasedBy(float increasedPercent) {
        return new ItemQuantity(increasedPercent, 1f);
    }

    public static ItemQuantity atShare(float share) {
        return new ItemQuantity(0f, share);
    }

    public ItemQuantity plusPercent(float extraIncreasedPercent) {
        return extraIncreasedPercent == 0f ? this : new ItemQuantity(increasedPercent + extraIncreasedPercent, share);
    }

    public ItemQuantity scaled(float factor) {
        return factor == 1f ? this : new ItemQuantity(increasedPercent, share * factor);
    }

    public float mult() {
        return share * Math.max(0f, 1f + increasedPercent / PERCENT);
    }
}
