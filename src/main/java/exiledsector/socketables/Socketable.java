package exiledsector.socketables;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Socketable {

    private final String id;
    private final String definitionId;
    private final long seed;
    private final List<RolledEffect> effects;

    protected Socketable(String id, String definitionId, long seed, List<RolledEffect> effects) {
        this.id = id;
        this.definitionId = definitionId;
        this.seed = seed;
        this.effects = new ArrayList<>(effects);
    }

    public abstract SocketableKind kind();

    public String id() {
        return id;
    }

    public String definitionId() {
        return definitionId;
    }

    public long seed() {
        return seed;
    }

    public List<RolledEffect> effects() {
        return Collections.unmodifiableList(effects);
    }

    public SocketableDefinition definition() {
        return SocketableDefinitions.get(definitionId);
    }

    public String name() {
        SocketableDefinition definition = definition();
        return definition == null ? Translation.text("socketable.unknown") : definition.displayName();
    }

    public String iconPath() {
        SocketableDefinition definition = definition();
        return definition == null ? SocketableDefinition.FALLBACK_ICON : definition.icon();
    }

    public List<StyledText> tooltipLines() {
        List<StyledText> lines = new ArrayList<>(headerLines());
        lines.addAll(effectLines());
        return lines;
    }

    public List<StyledText> headerLines() {
        List<StyledText> lines = new ArrayList<>();
        SocketableDefinition definition = definition();
        if (definition != null) {
            lines.add(Translation.msg("socketable.tooltip.summary")
                    .arg("kind", kind().displayName())
                    .arg("grade", definition.gradeName())
                    .arg("alignment", definition.alignmentName())
                    .styled());
            StyledText description = definition.descriptionText();
            if (description != null) {
                lines.add(description);
            }
        }
        return lines;
    }

    public List<StyledText> effectLines() {
        List<StyledText> lines = new ArrayList<>();
        for (RolledEffect effect : effects) {
            StyledText description = effect.description();
            if (description != null) {
                lines.add(description);
            }
        }
        return lines;
    }
}
