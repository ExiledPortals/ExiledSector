package exiledsector.charts.atlas;

import exiledsector.charts.generation.ChartTiers;
import exiledsector.data.EnumKeys;
import exiledsector.items.StarChart;

public record AtlasSpecial(String id, Kind kind, Placement placement, float chance, float minSteps, float radius, int count, float steps,
                           String requiredMod, String unlock, String factionId) {

    public static final float MOST_RADIUS = AtlasCell.CHUNK_SIZE - 1f;

    public enum Placement { CHUNK, FIXED }

    public enum Kind {
        INFESTATION, SENSOR_ARRAY, BASTION, UNIQUE;

        public String key() {
            return EnumKeys.key(this);
        }

        public boolean revealsLanes() {
            return this == SENSOR_ARRAY;
        }

        public boolean oneAttempt() {
            return this == BASTION;
        }

        public String nodeEffectsId() {
            return this == INFESTATION ? AtlasOpening.NEXUS : key();
        }

        AtlasOpening.Blocker openBlocker(StarChart chart) {
            return switch (this) {
                case BASTION -> chart == null || chart.tier() < ChartTiers.HIGHEST_TIER ? AtlasOpening.Blocker.BASTION_NEEDS_TOP_TIER : null;
                case INFESTATION -> AtlasOpening.explicitModifiers(chart) < AtlasOpening.NEXUS_MODIFIERS ? AtlasOpening.Blocker.NEXUS_NEEDS_MODIFIERS : null;
                default -> null;
            };
        }

        void onCompleted(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasSpecial special) {
            switch (this) {
                case INFESTATION -> AtlasProgress.cleanse(generator, state, cell, special);
                case BASTION -> state.addKeyFragment();
                default -> {
                }
            }
            if (this != UNIQUE) {
                state.addAtlasPoints(1);
            }
        }

        public static Kind parse(String text) {
            return text == null || text.isBlank() ? UNIQUE : EnumKeys.parse(Kind.class, text);
        }
    }

    public AtlasSpecial {
        kind = kind == null ? Kind.UNIQUE : kind;
        chance = Math.max(0f, Math.min(1f, chance));
        minSteps = Math.max(1f, minSteps);
        radius = Math.max(0f, Math.min(MOST_RADIUS, radius));
        count = Math.max(1, count);
        steps = Math.max(1f, steps);
        requiredMod = requiredMod == null ? "" : requiredMod;
        unlock = unlock == null ? "" : unlock;
        factionId = factionId == null ? "" : factionId;
    }

    public boolean infests() {
        return kind == Kind.INFESTATION;
    }
}
