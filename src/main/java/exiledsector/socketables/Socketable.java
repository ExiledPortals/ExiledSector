package exiledsector.socketables;

import exiledsector.i18n.StyledText;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Socketable {

    private final String id;
    private final String definitionId;
    private final long seed;
    private final List<RolledEffect> effects;
    private boolean favourite;

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

    public boolean isFavourite() {
        return favourite;
    }

    public void toggleFavourite() {
        favourite = !favourite;
    }

    public SocketableDefinition definition() {
        return SocketableDefinitions.get(definitionId);
    }

    public SocketableName displayName() {
        return SocketableNames.nameFor(definition(), kind(), seed, effects);
    }

    public String name() {
        return displayName().title();
    }

    public SocketableRarity rarity() {
        return SocketableRarity.of(definition(), effects.size());
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
            StyledText description = definition.descriptionText();
            if (description != null) {
                lines.add(description);
            }
        }
        return lines;
    }

    public boolean canSocketInto(SkillNode socket) {
        return socket != null && socket.getType().getTier() == SkillTier.SOCKET;
    }

    public List<SkillTypeEffect> skillEffects() {
        List<SkillTypeEffect> applied = new ArrayList<>(effects.size());
        for (RolledEffect effect : effects) {
            SkillEffect resolved = effect.effect();
            if (resolved != null) {
                applied.add(new SkillTypeEffect(resolved, effect.magnitude()));
            }
        }
        return applied;
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
