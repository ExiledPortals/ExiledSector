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
        String normalisedName = normalise(rawName);
        if (normalisedName.isEmpty()) {
            return Problem.EMPTY;
        }
        if (normalisedName.codePointCount(0, normalisedName.length()) > MAX_LENGTH) {
            return Problem.TOO_LONG;
        }
        String foldedName = normalisedName.toLowerCase(Locale.ROOT);
        for (SkillTreeTemplate template : existing) {
            if (template.rootNodeId().equals(rootNodeId) && template.name().toLowerCase(Locale.ROOT).equals(foldedName)) {
                return Problem.DUPLICATE;
            }
        }
        return Problem.NONE;
    }
}
