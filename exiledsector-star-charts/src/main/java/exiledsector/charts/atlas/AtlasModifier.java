package exiledsector.charts.atlas;

import exiledsector.charts.anomaly.DenseNebula;
import exiledsector.data.EnumKeys;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.modifiers.ModifierEffect;

public enum AtlasModifier implements ModifierEffect {

    ATLAS_VISIBILITY_LANES(true),
    ATLAS_DENSE_NEBULA_CHANCE_PERCENT(new AtlasChance(DenseNebula.ANOMALY_ID)),
    ATLAS_OFFICER_TROPHY_DUPLICATION_PERCENT(false),
    ATLAS_OFFICER_SCAVENGE_DUPLICATION_PERCENT(false),
    ATLAS_OFFICER_TROPHY_REFIT(false),
    ATLAS_OFFICER_FIGHT_FREQUENCY_PERCENT(false),
    ATLAS_OFFICER_SCAVENGERS_PERCENT(false),
    ATLAS_OFFICER_LUCKY_SCAVENGING(false);

    static final String KEY_PREFIX = "atlas.modifier.";

    private record AtlasChance(String anomalyId) {
    }

    private final boolean counted;
    private final String atlasChanceAnomalyId;

    AtlasModifier(boolean counted) {
        this.counted = counted;
        this.atlasChanceAnomalyId = null;
    }

    AtlasModifier(AtlasChance atlasChance) {
        this.counted = false;
        this.atlasChanceAnomalyId = atlasChance.anomalyId();
    }

    public boolean isCounted() {
        return counted;
    }

    public String atlasChanceAnomalyId() {
        return atlasChanceAnomalyId;
    }

    public String key() {
        return KEY_PREFIX + name();
    }

    @Override
    public StyledText description(float magnitude) {
        return description(key(), magnitude);
    }

    public StyledText description(String baseKey, float magnitude) {
        if (counted) {
            int count = Math.round(magnitude);
            return Translation.msg(baseKey).count(count).arg("value", count).styled();
        }
        return Translation.msg(baseKey).arg("value", magnitude).styled();
    }

    public static AtlasModifier byNameOrNull(String effectName) {
        return EnumKeys.parse(AtlasModifier.class, effectName);
    }
}
