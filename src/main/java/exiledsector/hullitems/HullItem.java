package exiledsector.hullitems;

import exiledsector.modifiers.ModifierCategory;
import exiledsector.modifiers.ModifierSlot;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.StyledText;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HullItem {

    private final String id;
    private final String definitionId;
    private final long seed;
    private final List<RolledEffect> effects;
    private List<RolledEffect> implicits;
    private FrozenName frozenName;

    HullItem(String id, String definitionId, long seed, List<RolledEffect> effects) {
        this.id = id;
        this.definitionId = definitionId;
        this.seed = seed;
        this.effects = new ArrayList<>(effects);
    }

    public HullItemType kind() {
        HullItemDefinition definition = definition();
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

    public List<RolledEffect> implicits() {
        if (implicits != null) {
            return Collections.unmodifiableList(implicits);
        }
        List<RolledEffect> rolled = HullItemImplicits.roll(kind(), seed);
        if (HullItemImplicits.isLoaded() && definition() != null) {
            implicits = new ArrayList<>(rolled);
        }
        return rolled;
    }

    public HullItemDefinition definition() {
        return HullItemDefinitions.get(definitionId);
    }

    public HullItemName displayName() {
        HullItemDefinition definition = definition();
        return HullItemNames.render(definition, kind(), effects, frozenName(definition));
    }

    FrozenName frozenName() {
        return frozenName(definition());
    }

    private FrozenName frozenName(HullItemDefinition definition) {
        if (definition != null && (frozenName == null || frozenName.isAssembledText())) {
            FrozenName freshName = HullItemNames.freeze(definition, seed, effects);
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
        HullItemDefinition definition = definition();
        frozenName = definition == null ? null : HullItemNames.freeze(definition, seed, effects);
    }

    public String name() {
        return displayName().title();
    }

    public HullItemRarity rarity() {
        return AffixLayout.rarityFor(definition(), effects.size());
    }

    public String iconPath() {
        HullItemDefinition definition = definition();
        return definition == null ? HullItemDefinition.FALLBACK_ICON : definition.icon();
    }

    public List<StyledText> tooltipLines() {
        List<StyledText> lines = new ArrayList<>(headerLines());
        lines.addAll(implicitLines(false));
        lines.addAll(effectLines());
        return lines;
    }

    public List<StyledText> headerLines() {
        List<StyledText> lines = new ArrayList<>();
        HullItemDefinition definition = definition();
        if (definition != null) {
            StyledText description = definition.descriptionText();
            if (description != null) {
                lines.add(description);
            }
        }
        return lines;
    }

    public boolean canSocketInto(SkillNode socket) {
        return socket != null && socket.getType().getTier() == SkillTier.SOCKET && kind() == HullItemType.SUBROUTINE;
    }

    public boolean canSocketInto(HullItemType frameworkSocket) {
        return frameworkSocket != null && frameworkSocket.isFramework() && kind() == frameworkSocket;
    }

    public List<SkillTypeEffect> skillEffects() {
        return skillEffects(null);
    }

    public List<SlottedRoll> rolls() {
        HullItemDefinition definition = definition();
        HullItemType type = kind();
        List<SlottedRoll> rolls = new ArrayList<>(effects.size() + 2);
        for (RolledEffect implicit : implicits()) {
            rolls.add(new SlottedRoll(ModifierSlot.IMPLICIT, implicit, HullItemImplicits.line(type, implicit.effectName())));
        }
        for (RolledEffect explicit : effects) {
            PoolEntry poolEntry = definition == null ? null : definition.poolEntry(explicit.effectName());
            ModifierSlot slot = poolEntry == null ? ModifierSlot.SUFFIX : poolEntry.slot();
            rolls.add(new SlottedRoll(slot, explicit, poolEntry == null ? null : poolEntry.line()));
        }
        return rolls;
    }

    public List<SlottedRoll> rolls(ModifierCategory category) {
        return rolls().stream().filter(roll -> roll.category() == category).toList();
    }

    public List<SkillTypeEffect> skillEffects(HullSize hullSize) {
        List<SkillTypeEffect> appliedEffects = new ArrayList<>(effects.size() + 2);
        for (SlottedRoll slottedRoll : rolls()) {
            SkillEffect resolvedEffect = slottedRoll.roll().effect();
            if (resolvedEffect != null) {
                float rolledMagnitude = slottedRoll.roll().magnitude();
                float magnitude = slottedRoll.line() == null ? rolledMagnitude : slottedRoll.line().valueFor(rolledMagnitude, hullSize);
                appliedEffects.add(new SkillTypeEffect(resolvedEffect, magnitude));
            }
        }
        return appliedEffects;
    }

    public List<StyledText> implicitLines(boolean withRollRanges) {
        List<StyledText> lines = new ArrayList<>();
        HullItemType type = kind();
        for (RolledEffect implicit : implicits()) {
            StyledText description = implicit.description(withRollRanges ? HullItemImplicits.line(type, implicit.effectName()) : null);
            if (description != null) {
                lines.add(description);
            }
        }
        return lines;
    }

    public List<StyledText> effectLines() {
        return effectLines(false);
    }

    public List<StyledText> effectLines(boolean withRollRanges) {
        return effectLines(withRollRanges, null);
    }

    public List<StyledText> effectLines(boolean withRollRanges, HullSize hullSize) {
        HullItemDefinition definition = definition();
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
                description = shownEffect.description(withRollRanges ? poolEntry.line() : null);
            }
            if (description != null) {
                lines.add(description);
            }
        }
        return lines;
    }
}
