package exiledsector.tools;

import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.SkillEffectNames;
import exiledlib.skills.skilleffect.StatMode;
import exiledsector.charts.ChartDrops;
import exiledsector.charts.ChartEditorVocabulary;
import exiledsector.charts.ChartEffect;
import exiledsector.charts.ChartPoi;
import exiledsector.charts.ChartTiers;
import exiledsector.items.DoctrineBehaviour;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemType;
import exiledsector.items.ItemUnlock;
import exiledsector.items.ModifierScope;
import exiledsector.items.StarChartDefinitions;
import exiledsector.items.SubroutineKind;
import exiledsector.items.UniqueSource;
import exiledsector.skills.SkillTier;
import exiledsector.skills.radius.RadiusSize;
import exiledsector.skills.tags.SkillTags;
import exiledsector.skills.unlock.BlueprintCategory;
import exiledsector.skills.unlock.UnlockConditionType;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;

final class EditorVocabulary {

    static final Path FILE = Path.of("tools/editor_vocabulary.json");

    private EditorVocabulary() {
    }

    static String generate() {
        StringBuilder json = new StringBuilder("{\n");
        json.append("  \"effects\": [\n");
        List<String> effectLines = new ArrayList<>();
        for (String name : new TreeSet<>(SkillEffectNames.all())) {
            SkillEffect effect = SkillEffect.byName(name);
            StatMode statMode = effect.statMode();
            effectLines.add("    {\"name\": " + quote(name) + ", \"statMode\": " + (statMode == null ? "null" : quote(statMode.name()))
                    + ", \"lowerIsBetter\": " + effect.lowerIsBetter() + "}");
        }
        json.append(String.join(",\n", effectLines)).append("\n  ],\n");
        json.append("  \"tags\": {\n");
        json.append("    \"theme\": ").append(list(SkillTags.THEME)).append(",\n");
        json.append("    \"region\": ").append(list(SkillTags.REGION)).append(",\n");
        json.append("    \"requirement\": ").append(list(SkillTags.REQUIREMENT)).append(",\n");
        json.append("    \"gate\": ").append(list(SkillTags.GATE)).append("\n");
        json.append("  },\n");
        json.append("  \"tiers\": ").append(list(Arrays.stream(SkillTier.values()).map(Enum::name).toList())).append(",\n");
        json.append("  \"unlockConditionTypes\": ")
                .append(list(Arrays.stream(UnlockConditionType.values()).map(type -> camelCase(type.name())).toList())).append(",\n");
        json.append("  \"parameterlessUnlockConditionTypes\": ")
                .append(list(Arrays.stream(UnlockConditionType.values()).filter(type -> type.isModGate() && !type.takesVolume())
                        .map(type -> camelCase(type.name())).toList())).append(",\n");
        json.append("  \"blueprintCategories\": ")
                .append(list(Arrays.stream(BlueprintCategory.values()).map(category -> camelCase(category.name())).toList())).append(",\n");
        json.append("  \"itemTypes\": ").append(list(Arrays.stream(ItemType.values()).map(ItemType::id).toList())).append(",\n");
        json.append("  \"itemKinds\": [")
                .append(Arrays.stream(ItemKind.values()).map(kind -> "{\"id\": " + quote(kind.id()) + ", \"type\": " + quote(kind.type().id()) + "}")
                        .collect(Collectors.joining(", ")))
                .append("],\n");
        json.append("  \"subroutineKinds\": ").append(list(Arrays.stream(SubroutineKind.values()).map(SubroutineKind::id).toList())).append(",\n");
        json.append("  \"modifierScopes\": ").append(list(Arrays.stream(ModifierScope.values()).map(ModifierScope::id).toList())).append(",\n");
        json.append("  \"doctrineBehaviours\": ").append(list(Arrays.stream(DoctrineBehaviour.values()).map(DoctrineBehaviour::id).toList())).append(",\n");
        json.append("  \"radiusSizes\": ").append(list(Arrays.stream(RadiusSize.values()).map(RadiusSize::dataKey).toList())).append(",\n");
        json.append("  \"uniqueSourceKinds\": ")
                .append(list(Arrays.stream(UniqueSource.Kind.values()).map(UniqueSource.Kind::prefix).toList())).append(",\n");
        json.append("  \"itemUnlocks\": ")
                .append(list(Arrays.stream(ItemUnlock.values()).map(ItemUnlock::id).toList())).append(",\n");
        json.append("  \"clearedChartTierPrefix\": ").append(quote(ItemUnlock.CLEARED_CHART_TIER_PREFIX)).append(",\n");
        json.append("  \"chartTierRange\": [").append(ChartTiers.LOWEST_TIER).append(", ").append(ChartTiers.HIGHEST_TIER).append("],\n");
        json.append("  \"chartEffects\": ").append(list(Arrays.stream(ChartEffect.values()).map(Enum::name).toList())).append(",\n");
        json.append("  \"chartSystemTypes\": ").append(list(StarChartDefinitions.SYSTEM_TYPES)).append(",\n");
        json.append("  \"chartPoiKinds\": ")
                .append(list(Arrays.stream(ChartPoi.Kind.values()).map(kind -> kind.name().toLowerCase(Locale.ROOT)).toList())).append(",\n");
        json.append("  \"chartDerelictTypes\": ").append(list(ChartEditorVocabulary.derelictTypes())).append(",\n");
        json.append("  \"chartTierDefaults\": [").append(String.join(", ", ChartEditorVocabulary.tierDefaultsJson())).append("],\n");
        json.append("  \"chartDropSources\": [")
                .append(Arrays.stream(ChartDrops.Source.values())
                        .map(source -> "{\"id\": " + quote(source.id()) + ", \"insideSystem\": " + source.insideSystem() + "}")
                        .collect(Collectors.joining(", ")))
                .append("],\n");
        json.append("  \"chartLootDropTypes\": ").append(list(ChartEditorVocabulary.lootDropTypes())).append(",\n");
        json.append("  \"chartLootScopes\": ").append(list(ChartEditorVocabulary.lootScopes())).append(",\n");
        json.append("  \"chartColonyItemGroups\": ").append(list(ChartEditorVocabulary.colonyItemGroups())).append(",\n");
        json.append("  \"chartExpansions\": ").append(list(ChartEditorVocabulary.expansionIds())).append("\n");
        return json.append("}\n").toString();
    }

    static String camelCase(String enumName) {
        StringBuilder camel = new StringBuilder();
        boolean upperNext = false;
        for (char c : enumName.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else {
                camel.append(upperNext ? c : Character.toLowerCase(c));
                upperNext = false;
            }
        }
        return camel.toString();
    }

    private static String list(List<String> values) {
        return values.stream().map(EditorVocabulary::quote).collect(Collectors.joining(", ", "[", "]"));
    }

    private static String quote(String value) {
        return JSONObject.quote(value);
    }
}
