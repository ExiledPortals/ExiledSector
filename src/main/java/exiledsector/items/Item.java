package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.StyledText;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierCategory;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Item {

    private final String id;
    private String definitionId;
    private final long seed;
    private List<RolledEffect> effects;
    private List<RolledEffect> implicits;
    private List<RolledModifier> explicitModifiers;
    private List<RolledModifier> implicitModifiers;
    private FrozenName frozenName;
    private transient boolean explicitIdsCurrent;
    private transient boolean implicitIdsCurrent;

    Item(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        this.id = id;
        this.definitionId = ItemIdAliases.currentId(definitionId);
        this.seed = seed;
        this.explicitModifiers = new ArrayList<>(explicitModifiers);
    }

    Item(Item source) {
        this.id = source.id;
        this.definitionId = source.definitionId;
        this.seed = source.seed;
        this.effects = source.effects;
        this.implicits = source.implicits;
        this.explicitModifiers = source.explicitModifiers;
        this.implicitModifiers = source.implicitModifiers;
        this.frozenName = source.frozenName;
    }

    static Item create(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        ItemDefinition definition = ItemDefinitions.get(definitionId);
        ItemType type = definition == null ? null : definition.type();
        if (type == null) {
            return new UnresolvedItem(id, definitionId, seed, explicitModifiers);
        }
        return switch (type) {
            case SUBROUTINE -> new Subroutine(id, definitionId, seed, explicitModifiers);
            case HULL_ITEM -> new HullItem(id, definitionId, seed, explicitModifiers);
        };
    }

    static Item withEffects(String id, String definitionId, long seed, List<RolledEffect> effects) {
        return create(id, definitionId, seed, RolledModifier.fromLegacyEffects(ItemDefinitions.get(definitionId), effects));
    }

    public abstract ItemType type();

    protected Object readResolve() {
        definitionId = ItemIdAliases.currentId(definitionId);
        ItemDefinition definition = definition();
        ItemType definedType = definition == null ? null : definition.type();
        if (definedType == type()) {
            return this;
        }
        if (definedType == null) {
            return new UnresolvedItem(this);
        }
        return switch (definedType) {
            case SUBROUTINE -> new Subroutine(this);
            case HULL_ITEM -> new HullItem(this);
        };
    }

    public ItemKind kind() {
        ItemDefinition definition = definition();
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
        ItemDefinition definition = definition();
        if (explicitModifiers != null) {
            if (!explicitIdsCurrent && definition != null) {
                explicitModifiers = new ArrayList<>(definition.withCurrentIds(explicitModifiers));
                explicitIdsCurrent = true;
            }
            return Collections.unmodifiableList(explicitModifiers);
        }
        List<RolledModifier> converted = RolledModifier.fromLegacyEffects(definition, effects == null ? List.of() : effects);
        if (definition != null || effects == null) {
            explicitModifiers = new ArrayList<>(converted);
            explicitIdsCurrent = definition != null;
            effects = null;
        }
        return Collections.unmodifiableList(converted);
    }

    public List<RolledEffect> effects() {
        return RolledModifier.lines(explicits());
    }

    public List<RolledModifier> implicits() {
        if (implicitModifiers != null) {
            if (!implicitIdsCurrent && ItemImplicits.isLoaded() && kind() != null && !kind().implicits().isEmpty()) {
                implicitModifiers = new ArrayList<>(ItemImplicits.withCurrentIds(kind(), implicitModifiers, seed));
                implicitIdsCurrent = true;
            }
            return Collections.unmodifiableList(implicitModifiers);
        }
        boolean resolvable = ItemImplicits.isLoaded() && kind() != null;
        List<RolledModifier> rolled = implicits == null ? ItemImplicits.roll(kind(), seed)
                : resolvable ? ItemImplicits.withCurrentIds(kind(), implicitsFromLegacy(implicits), seed) : implicitsFromLegacy(implicits);
        if (implicits != null || resolvable) {
            implicitModifiers = new ArrayList<>(rolled);
            implicitIdsCurrent = resolvable && !kind().implicits().isEmpty();
            implicits = null;
        }
        return rolled;
    }

    private static List<RolledModifier> implicitsFromLegacy(List<RolledEffect> legacyImplicits) {
        return legacyImplicits.stream().map(implicit -> RolledModifier.single(ModifierSlot.IMPLICIT, implicit)).toList();
    }

    public ItemDefinition definition() {
        return ItemDefinitions.get(definitionId);
    }

    public ItemName displayName() {
        ItemDefinition definition = definition();
        return ItemNames.render(definition, kind(), explicits(), frozenName(definition));
    }

    FrozenName frozenName() {
        return frozenName(definition());
    }

    private FrozenName frozenName(ItemDefinition definition) {
        if (definition != null && (frozenName == null || frozenName.isAssembledText())) {
            FrozenName freshName = ItemNames.freeze(definition, seed, explicits());
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
        ItemDefinition definition = definition();
        frozenName = definition == null ? null : ItemNames.freeze(definition, seed, explicits());
    }

    public String name() {
        return displayName().title();
    }

    public ItemRarity rarity() {
        return AffixLayout.rarityFor(definition(), explicits().size());
    }

    public String iconPath() {
        ItemDefinition definition = definition();
        return definition == null ? ItemDefinition.FALLBACK_ICON : definition.icon();
    }

    public List<StyledText> tooltipLines() {
        List<StyledText> lines = new ArrayList<>(headerLines());
        lines.addAll(implicitLines(false));
        lines.addAll(effectLines());
        return lines;
    }

    public List<StyledText> headerLines() {
        List<StyledText> lines = new ArrayList<>();
        ItemDefinition definition = definition();
        if (definition != null) {
            StyledText description = definition.descriptionText();
            if (description != null) {
                lines.add(description);
            }
        }
        return lines;
    }

    public boolean canSocketInto(SkillNode socket) {
        return false;
    }

    public boolean canSocketInto(ShipSection section) {
        return false;
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
        ItemKind type = kind();
        if (type == null) {
            return null;
        }
        return type.implicits().stream().filter(modifier -> modifier.id().equals(modifierId)).findFirst().orElse(null);
    }

    private Modifier explicitModifier(String modifierId) {
        ItemDefinition definition = definition();
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
