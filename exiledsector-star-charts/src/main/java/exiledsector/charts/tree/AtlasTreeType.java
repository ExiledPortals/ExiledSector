package exiledsector.charts.tree;

import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillTier;

import java.util.List;

public record AtlasTreeType(String id, String name, String iconPath, SkillTier tier, List<ChartGenContext.AppliedEffect> effects,
                            List<AtlasEffects.Line> atlasEffects, String flavour, List<String> tags) {

    private static final String KEY_PREFIX = "atlasTree.type.";

    public AtlasTreeType {
        tier = tier == null ? SkillTier.SMALL : tier;
        effects = effects == null ? List.of() : List.copyOf(effects);
        atlasEffects = atlasEffects == null ? List.of() : List.copyOf(atlasEffects);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public String displayName() {
        return Translation.data(KEY_PREFIX + id + ".name", name);
    }

    public String flavourText() {
        return flavour == null ? null : Translation.data(KEY_PREFIX + id + ".flavour", flavour);
    }
}
