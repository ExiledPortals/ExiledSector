package exiledsector.charts.atlas;

public record AtlasSpecial(String id, String kind, Placement placement, float chance, float minSteps, float radius, int count, float steps,
                           String requiredMod, String unlock, String factionId) {

    public static final String INFESTATION = "infestation";
    public static final float MOST_RADIUS = AtlasCell.CHUNK_SIZE - 1f;

    public enum Placement { CHUNK, FIXED }

    public AtlasSpecial {
        kind = kind == null ? "" : kind;
        chance = Math.max(0f, Math.min(1f, chance));
        minSteps = Math.max(1f, minSteps);
        radius = Math.max(0f, Math.min(MOST_RADIUS, radius));
        count = Math.max(1, count);
        steps = Math.max(1f, steps);
        requiredMod = requiredMod == null ? "" : requiredMod;
        unlock = unlock == null ? "" : unlock;
        factionId = factionId == null ? "" : factionId;
    }

    public AtlasSpecial(String id, String kind, Placement placement, float chance, float minSteps, float radius, int count, float steps,
                        String requiredMod, String unlock) {
        this(id, kind, placement, chance, minSteps, radius, count, steps, requiredMod, unlock, "");
    }

    public boolean infests() {
        return INFESTATION.equals(kind);
    }
}
