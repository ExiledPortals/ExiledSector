package exiledsector.tools;

import exiledsector.skills.SkillTier;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.SkillEffectNames;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.tags.SkillTags;
import exiledsector.skills.unlock.BlueprintCategory;
import exiledsector.skills.unlock.UnlockConditionType;
import exiledsector.socketables.SocketableKind;
import exiledsector.socketables.SocketableUnlock;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        json.append("    \"requirement\": ").append(list(SkillTags.REQUIREMENT)).append("\n");
        json.append("  },\n");
        json.append("  \"tiers\": ").append(list(Arrays.stream(SkillTier.values()).map(Enum::name).toList())).append(",\n");
        json.append("  \"unlockConditionTypes\": ")
                .append(list(Arrays.stream(UnlockConditionType.values()).map(type -> camelCase(type.name())).toList())).append(",\n");
        json.append("  \"blueprintCategories\": ")
                .append(list(Arrays.stream(BlueprintCategory.values()).map(category -> camelCase(category.name())).toList())).append(",\n");
        json.append("  \"socketableKinds\": ").append(list(Arrays.stream(SocketableKind.values()).map(SocketableKind::id).toList())).append(",\n");
        json.append("  \"socketableUnlocks\": ")
                .append(list(Arrays.stream(SocketableUnlock.values()).map(SocketableUnlock::id).toList())).append("\n");
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
