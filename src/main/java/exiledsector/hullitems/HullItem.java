package exiledsector.hullitems;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.StyledText;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierCategory;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
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
    private List<RolledEffect> effects;
    private List<RolledEffect> implicits;
    private List<RolledModifier> explicitModifiers;
    private List<RolledModifier> implicitModifiers;
    private FrozenName frozenName;

    HullItem(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        this.id = id;
        this.definitionId = definitionId;
        this.seed = seed;
        this.explicitModifiers = new ArrayList<>(explicitModifiers);
    }

    static HullItem withEffects(String id, String definitionId, long seed, List<RolledEffect> effects) {
        return new HullItem(id, definitionId, seed, RolledModifier.fromLegacyEffects(HullItemDefinitions.get(definitionId), effects));
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

    public List<RolledModifier> explicits() {
        if (explicitModifiers == null) {
            explicitModifiers = RolledModifier.fromLegacyEffects(definition(), effects == null ? List.of() : effects);
            effects = null;
        }
        return Collections.unmodifiableList(explicitModifiers);
    }

    public List<RolledEffect> effects() {
        return RolledModifier.lines(explicits());
    }

    public List<RolledModifier> implicits() {
        if (implicitModifiers != null) {
            return Collections.unmodifiableList(implicitModifiers);
        }
        List<RolledModifier> rolled = implicits != null ? implicitsFromLegacy(implicits) : HullItemImplicits.roll(kind(), seed);
        if (implicits != null || (HullItemImplicits.isLoaded() && definition() != null)) {
            implicitModifiers = new ArrayList<>(rolled);
            implicits = null;
        }
        return rolled;
    }

    private static List<RolledModifier> implicitsFromLegacy(List<RolledEffect> legacyImplicits) {
        return legacyImplicits.stream().map(implicit -> RolledModifier.single(ModifierSlot.IMPLICIT, implicit)).toList();
    }

    public HullItemDefinition definition() {
        return HullItemDefinitions.get(definitionId);
    }

    public HullItemName displayName() {
        HullItemDefinition definition = definition();
        return HullItemNames.render(definition, kind(), explicits(), frozenName(definition));
    }

    FrozenName frozenName() {
        return frozenName(definition());
    }

    private FrozenName frozenName(HullItemDefinition definition) {
        if (definition != null && (frozenName == null || frozenName.isAssembledText())) {
            FrozenName freshName = HullItemNames.freeze(definition, seed, explicits());
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

    void replaceExplicits(List<RolledModifier> replacementModifiers) {
        explicitModifiers = new ArrayList<>(replacementModifiers);
        effects = null;
    }

    void refreezeName() {
        HullItemDefinition definition = definition();
        frozenName = definition == null ? null : HullItemNames.freeze(definition, seed, explicits());
    }

    public String name() {
        return displayName().title();
    }

    public HullItemRarity rarity() {
        return AffixLayout.rarityFor(definition(), explicits().size());
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

    public boolean canSocketInto(ShipSection section) {
        return section != null && kind() != null && kind().section() == section;
    }

    public List<SkillTypeEffect> skillEffects() {
        return skillEffects(null);
    }

    public List<SlottedRoll> rolls() {
        List<SlottedRoll> rolls = new ArrayList<>();
        for (RolledModifier implicit : implicits()) {
            addRolls(rolls, implicit, implicitModifier(implicit.modifierId()));
        }
        for (RolledModifier explicit : explicits()) {
            addRolls(rolls, explicit, explicitModifier(explicit.modifierId()));
        }
        return rolls;
    }

    private static void addRolls(List<SlottedRoll> rolls, RolledModifier rolledModifier, Modifier modifier) {
        for (RolledEffect line : rolledModifier.lines()) {
            rolls.add(new SlottedRoll(rolledModifier.slot(), line, modifier == null ? null : modifier.line(line.effectName())));
        }
    }

    private Modifier implicitModifier(String modifierId) {
        HullItemType type = kind();
        if (type == null) {
            return null;
        }
        return type.implicits().stream().filter(modifier -> modifier.id().equals(modifierId)).findFirst().orElse(null);
    }

    private Modifier explicitModifier(String modifierId) {
        HullItemDefinition definition = definition();
        PoolEntry entry = definition == null ? null : definition.poolEntryFor(modifierId);
        return entry == null ? null : entry.modifier();
    }

    public List<SlottedRoll> rolls(ModifierCategory category) {
        return rolls().stream().filter(roll -> roll.category() == category).toList();
    }

    public List<SkillTypeEffect> skillEffects(HullSize hullSize) {
        List<SkillTypeEffect> appliedEffects = new ArrayList<>();
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
        for (SlottedRoll implicit : rolls(ModifierCategory.IMPLICIT)) {
            StyledText description = implicit.roll().description(withRollRanges ? implicit.line() : null);
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
        List<StyledText> lines = new ArrayList<>();
        for (SlottedRoll explicit : rolls(ModifierCategory.EXPLICIT)) {
            RolledEffect effect = explicit.roll();
            ModifierLine line = explicit.line();
            StyledText description;
            if (line == null) {
                description = effect.description(null);
            } else if (line.listsEveryHullValueFor(hullSize)) {
                description = effect.hullValuesDescription(line.hullValues());
            } else {
                RolledEffect shownEffect = new RolledEffect(effect.effectName(), line.valueFor(effect.magnitude(), hullSize));
                description = shownEffect.description(withRollRanges ? line : null);
            }
            if (description != null) {
                lines.add(description);
            }
        }
        return lines;
    }
}
