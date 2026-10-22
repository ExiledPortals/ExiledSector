package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.modifiers.ModifierEffect;

import java.util.Locale;

public record ChartFactionEffect(String factionId) implements ModifierEffect {

    static final String NAME_PREFIX = "CHART_FACTION_";
    static final String KEY = ChartEffect.KEY_PREFIX + "CHART_FACTION";

    static String effectName(String factionId) {
        return NAME_PREFIX + factionId.toUpperCase(Locale.ROOT);
    }

    @Override
    public String name() {
        return effectName(factionId);
    }

    @Override
    public StyledText description(float magnitude) {
        return Translation.msg(KEY).arg("faction", displayName(factionId)).styled();
    }

    static String displayName(String factionId) {
        try {
            FactionSpecAPI spec = Global.getSettings().getFactionSpec(factionId);
            return spec == null ? factionId : spec.getDisplayNameWithArticle();
        } catch (RuntimeException e) {
            return factionId;
        }
    }
}
