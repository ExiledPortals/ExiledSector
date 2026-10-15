package exiledsector.skills.template;

import java.util.Collection;
import java.util.Locale;

public final class TemplateNames {

    public static final int MAX_LENGTH = 32;

    public enum Problem {
        NONE, EMPTY, TOO_LONG, DUPLICATE
    }

    private TemplateNames() {
    }

    public static String normalise(String rawName) {
        return rawName == null ? "" : rawName.strip();
    }

    public static Problem validate(String rawName, String rootNodeId, Collection<SkillTreeTemplate> existing) {
        return validate(rawName, rootNodeId, existing, null);
    }

    public static Problem validate(String rawName, String rootNodeId, Collection<SkillTreeTemplate> existing, String renamedTemplateId) {
        String normalisedName = normalise(rawName);
        if (normalisedName.isEmpty()) {
            return Problem.EMPTY;
        }
        if (normalisedName.codePointCount(0, normalisedName.length()) > MAX_LENGTH) {
            return Problem.TOO_LONG;
        }
        String foldedName = normalisedName.toLowerCase(Locale.ROOT);
        for (SkillTreeTemplate template : existing) {
            if (template.rootNodeId().equals(rootNodeId) && !template.id().equals(renamedTemplateId)
                    && template.name().toLowerCase(Locale.ROOT).equals(foldedName)) {
                return Problem.DUPLICATE;
            }
        }
        return Problem.NONE;
    }

    public static String unique(String baseName, String rootNodeId, Collection<SkillTreeTemplate> existing) {
        String normalisedBase = truncate(normalise(baseName), MAX_LENGTH);
        if (validate(normalisedBase, rootNodeId, existing) == Problem.NONE) {
            return normalisedBase;
        }
        for (int suffix = 2; ; suffix++) {
            String suffixText = " " + suffix;
            String candidate = truncate(normalisedBase, MAX_LENGTH - suffixText.length()).stripTrailing() + suffixText;
            if (validate(candidate, rootNodeId, existing) == Problem.NONE) {
                return candidate;
            }
        }
    }

    private static String truncate(String name, int maxCodePoints) {
        int codePointCount = name.codePointCount(0, name.length());
        return codePointCount <= maxCodePoints ? name : name.substring(0, name.offsetByCodePoints(0, Math.max(0, maxCodePoints)));
    }
}
