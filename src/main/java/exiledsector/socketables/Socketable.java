package exiledsector.socketables;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.StyledText;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Socketable {

    private final String id;
    private final String definitionId;
    private final long seed;
    private final List<RolledEffect> effects;
    private FrozenName frozenName;

    Socketable(String id, String definitionId, long seed, List<RolledEffect> effects) {
        this.id = id;
        this.definitionId = definitionId;
        this.seed = seed;
        this.effects = new ArrayList<>(effects);
    }

    public SocketType kind() {
        SocketableDefinition definition = definition();
        return definition == null ? null : definition.kind();
    }

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

    public SocketableName displayName() {
        SocketableDefinition definition = definition();
        return SocketableNames.render(definition, kind(), effects, frozenName(definition));
    }

    FrozenName frozenName() {
        return frozenName(definition());
    }

    private FrozenName frozenName(SocketableDefinition definition) {
        if (definition != null && (frozenName == null || frozenName.isAssembledText())) {
            FrozenName freshName = SocketableNames.freeze(definition, seed, effects);
            if (freshName != null || frozenName == null) {
                frozenName = freshName;
            }
        }
        return frozenName;
    }

    void freezeName() {
        frozenName(definition());
    }

    void freezeName(FrozenName presetName) {
        if (presetName != null) {
            frozenName = presetName;
        }
    }

    void replaceEffects(List<RolledEffect> replacementEffects) {
        effects.clear();
        effects.addAll(replacementEffects);
    }

    void refreezeName() {
        SocketableDefinition definition = definition();
        frozenName = definition == null ? null : SocketableNames.freeze(definition, seed, effects);
    }

    public String name() {
        return displayName().title();
    }

    public SocketableRarity rarity() {
        return AffixLayout.rarityFor(definition(), effects.size());
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
        return socket != null && socket.getType().getTier() == SkillTier.SOCKET && kind() == SocketType.SUBROUTINE;
    }

    public boolean canSocketInto(SocketType frameworkSocket) {
        return frameworkSocket != null && frameworkSocket.isFramework() && kind() == frameworkSocket;
    }

    public List<SkillTypeEffect> skillEffects() {
        return skillEffects(null);
    }

    public List<SkillTypeEffect> skillEffects(HullSize hullSize) {
        SocketableDefinition definition = definition();
        List<SkillTypeEffect> appliedEffects = new ArrayList<>(effects.size());
        for (RolledEffect effect : effects) {
            SkillEffect resolvedEffect = effect.effect();
            if (resolvedEffect != null) {
                PoolEntry poolEntry = definition == null ? null : definition.poolEntry(effect.effectName());
                float magnitude = poolEntry == null ? effect.magnitude() : poolEntry.valueFor(effect.magnitude(), hullSize);
                appliedEffects.add(new SkillTypeEffect(resolvedEffect, magnitude));
            }
        }
        return appliedEffects;
    }

    public List<StyledText> effectLines() {
        return effectLines(false);
    }

    public List<StyledText> effectLines(boolean withRollRanges) {
        return effectLines(withRollRanges, null);
    }

    public List<StyledText> effectLines(boolean withRollRanges, HullSize hullSize) {
        SocketableDefinition definition = definition();
        List<StyledText> lines = new ArrayList<>();
        for (RolledEffect effect : effects) {
            PoolEntry poolEntry = definition == null ? null : definition.poolEntry(effect.effectName());
            StyledText description;
            if (poolEntry == null) {
                description = effect.description(null);
            } else if (poolEntry.listsEveryHullValueFor(hullSize)) {
                description = effect.hullValuesDescription(poolEntry.hullValues());
            } else {
                RolledEffect shownEffect = new RolledEffect(effect.effectName(), poolEntry.valueFor(effect.magnitude(), hullSize));
                description = shownEffect.description(withRollRanges ? poolEntry : null);
            }
            if (description != null) {
                lines.add(description);
            }
        }
        return lines;
    }
}
