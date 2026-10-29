package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.hullmodule.ModuleShare;
import exiledsector.skills.skilleffect.hullmodule.StationModuleSkillEffect;
import exiledsector.skills.unlock.UnlockCondition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkillType {

    private static final String KEY_PREFIX = "skillType.";

    private final String id;
    private final String displayName;
    private final String iconPath;
    private final List<SkillTypeEffect> effects;
    private final List<HullSizeSkillEffect> hullSizeEffects;
    private final SkillTier tier;
    private final String vanillaHullModId;
    private final SkillItemCost itemCost;
    private final Float temporaryAfterDeploymentSeconds;
    private final Set<HullSize> requiredHullSizes;
    private final String flavour;
    private final String flavourAutomated;
    private final List<String> effectText;
    private final String todo;
    private final List<String> optionalOptionIds;
    private final Set<String> optionalOptionIdSet;
    private final List<String> exclusiveHullModIds;
    private final List<String> phantomHullModIds;
    private final List<String> ownHullModIds;
    private final List<String> exclusiveAndOwnHullModIds;
    private final List<String> exclusiveSkillTypeIds;
    private final List<UnlockCondition> unlockConditions;
    private final List<String> tags;
    private final ModuleShareOverride moduleShareOverride;

    private SkillType(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.iconPath = builder.iconPath;
        this.effects = builder.effects == null ? Collections.emptyList() : builder.effects;
        this.hullSizeEffects = builder.hullSizeEffects == null ? Collections.emptyList() : builder.hullSizeEffects;
        this.tier = builder.tier;
        this.vanillaHullModId = builder.vanillaHullModId;
        this.itemCost = builder.itemCost;
        this.temporaryAfterDeploymentSeconds = builder.temporaryAfterDeploymentSeconds;
        this.requiredHullSizes = builder.requiredHullSizes == null || builder.requiredHullSizes.isEmpty()
                ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(builder.requiredHullSizes));
        this.flavour = builder.flavour == null || builder.flavour.isBlank() ? null : builder.flavour;
        this.flavourAutomated = builder.flavourAutomated == null || builder.flavourAutomated.isBlank() ? null : builder.flavourAutomated;
        this.effectText = builder.effectText == null ? Collections.emptyList() : builder.effectText;
        this.todo = builder.todo;
        this.optionalOptionIds = builder.optionalOptionIds == null ? Collections.emptyList() : builder.optionalOptionIds;
        this.optionalOptionIdSet = Collections.unmodifiableSet(new HashSet<>(optionalOptionIds));
        this.exclusiveHullModIds = builder.exclusiveHullModIds == null ? Collections.emptyList() : builder.exclusiveHullModIds;
        this.phantomHullModIds = builder.phantomHullModIds == null ? Collections.emptyList() : builder.phantomHullModIds;
        this.ownHullModIds = ownHullModIds(vanillaHullModId, phantomHullModIds);
        this.exclusiveAndOwnHullModIds = exclusiveAndOwnHullModIds(exclusiveHullModIds, ownHullModIds);
        this.exclusiveSkillTypeIds = builder.exclusiveSkillTypeIds == null ? Collections.emptyList() : builder.exclusiveSkillTypeIds;
        this.unlockConditions = builder.unlockConditions == null ? Collections.emptyList() : builder.unlockConditions;
        this.tags = builder.tags == null ? Collections.emptyList() : builder.tags;
        this.moduleShareOverride = builder.moduleShareOverride == null ? ModuleShareOverride.AUTO : builder.moduleShareOverride;
    }

    public static final class Builder {
        private final String id;
        private final String displayName;
        private final String iconPath;
        private final SkillTier tier;
        private List<SkillTypeEffect> effects = Collections.emptyList();
        private List<HullSizeSkillEffect> hullSizeEffects = Collections.emptyList();
        private String vanillaHullModId;
        private SkillItemCost itemCost;
        private Float temporaryAfterDeploymentSeconds;
        private Collection<HullSize> requiredHullSizes;
        private String flavour;
        private String flavourAutomated;
        private List<String> effectText = Collections.emptyList();
        private String todo;
        private List<String> optionalOptionIds = Collections.emptyList();
        private List<String> exclusiveHullModIds = Collections.emptyList();
        private List<String> phantomHullModIds = Collections.emptyList();
        private List<String> exclusiveSkillTypeIds = Collections.emptyList();
        private List<UnlockCondition> unlockConditions = Collections.emptyList();
        private List<String> tags = Collections.emptyList();
        private ModuleShareOverride moduleShareOverride = ModuleShareOverride.AUTO;

        public Builder(String id, String displayName, String iconPath, SkillTier tier) {
            this.id = id;
            this.displayName = displayName;
            this.iconPath = iconPath;
            this.tier = tier;
        }

        public Builder effects(List<SkillTypeEffect> effects) {
            this.effects = effects;
            return this;
        }

        public Builder hullSizeEffects(List<HullSizeSkillEffect> hullSizeEffects) {
            this.hullSizeEffects = hullSizeEffects;
            return this;
        }

        public Builder vanillaHullModId(String vanillaHullModId) {
            this.vanillaHullModId = vanillaHullModId;
            return this;
        }

        public Builder itemCost(SkillItemCost itemCost) {
            this.itemCost = itemCost;
            return this;
        }

        public Builder temporaryAfterDeploymentSeconds(Float temporaryAfterDeploymentSeconds) {
            this.temporaryAfterDeploymentSeconds = temporaryAfterDeploymentSeconds;
            return this;
        }

        public Builder requiredHullSizes(Collection<HullSize> requiredHullSizes) {
            this.requiredHullSizes = requiredHullSizes;
            return this;
        }

        public Builder flavour(String flavour) {
            this.flavour = flavour;
            return this;
        }

        public Builder flavourAutomated(String flavourAutomated) {
            this.flavourAutomated = flavourAutomated;
            return this;
        }

        public Builder effectText(List<String> effectText) {
            this.effectText = effectText;
            return this;
        }

        public Builder todo(String todo) {
            this.todo = todo;
            return this;
        }

        public Builder optionalOptionIds(List<String> optionalOptionIds) {
            this.optionalOptionIds = optionalOptionIds;
            return this;
        }

        public Builder exclusiveHullModIds(List<String> exclusiveHullModIds) {
            this.exclusiveHullModIds = exclusiveHullModIds;
            return this;
        }

        public Builder phantomHullModIds(List<String> phantomHullModIds) {
            this.phantomHullModIds = phantomHullModIds;
            return this;
        }

        public Builder exclusiveSkillTypeIds(List<String> exclusiveSkillTypeIds) {
            this.exclusiveSkillTypeIds = exclusiveSkillTypeIds;
            return this;
        }

        public Builder unlockConditions(List<UnlockCondition> unlockConditions) {
            this.unlockConditions = unlockConditions;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder moduleShareOverride(ModuleShareOverride moduleShareOverride) {
            this.moduleShareOverride = moduleShareOverride;
            return this;
        }

        public SkillType build() {
            return new SkillType(this);
        }
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return Translation.data(KEY_PREFIX + id + ".name", displayName);
    }

    public String getSourceName() {
        return displayName;
    }

    public String getFlavourText() {
        return flavour == null ? null : Translation.data(KEY_PREFIX + id + ".flavour", flavour);
    }

    public String getFlavourSource() {
        return flavour;
    }

    public String getFlavourText(boolean automated) {
        String automatedText = automated ? getAutomatedFlavourText() : null;
        return automatedText == null ? getFlavourText() : automatedText;
    }

    public String getAutomatedFlavourText() {
        return flavourAutomated == null ? null : Translation.data(KEY_PREFIX + id + ".flavourAutomated", flavourAutomated);
    }

    public List<StyledText> getEffectTextLines() {
        List<StyledText> lines = new ArrayList<>(effectText.size());
        for (int index = 0; index < effectText.size(); index++) {
            lines.add(Translation.dataStyled(KEY_PREFIX + id + ".effectText." + index, effectText.get(index)));
        }
        return lines;
    }

    public String getIconPath() {
        return iconPath;
    }

    public List<SkillTypeEffect> getEffects() {
        return effects;
    }

    public SkillTier getTier() {
        return tier;
    }

    public String getVanillaHullModId() {
        return vanillaHullModId;
    }

    public SkillItemCost getItemCost() {
        return itemCost;
    }

    public Float getTemporaryAfterDeploymentSeconds() {
        return temporaryAfterDeploymentSeconds;
    }

    public Set<HullSize> getRequiredHullSizes() {
        return requiredHullSizes;
    }

    public boolean allowsHullSize(HullSize hullSize) {
        return requiredHullSizes.isEmpty() || requiredHullSizes.contains(hullSize);
    }

    public String getTodo() {
        return todo;
    }

    public List<String> getOptionalOptionIds() {
        return optionalOptionIds;
    }

    public boolean isOptional() {
        return !optionalOptionIds.isEmpty();
    }

    public boolean offersOption(String typeId) {
        return optionalOptionIdSet.contains(typeId);
    }

    public List<String> getExclusiveAndOwnHullModIds() {
        return exclusiveAndOwnHullModIds;
    }

    private static List<String> exclusiveAndOwnHullModIds(List<String> exclusiveHullModIds, List<String> ownHullModIds) {
        List<String> combined = new ArrayList<>(exclusiveHullModIds);
        for (String ownHullModId : ownHullModIds) {
            if (!combined.contains(ownHullModId)) {
                combined.add(ownHullModId);
            }
        }
        return Collections.unmodifiableList(combined);
    }

    public List<String> getOwnHullModIds() {
        return ownHullModIds;
    }

    private static List<String> ownHullModIds(String vanillaHullModId, List<String> phantomHullModIds) {
        if (vanillaHullModId == null) {
            return phantomHullModIds;
        }
        List<String> own = new ArrayList<>();
        own.add(vanillaHullModId);
        for (String phantomHullModId : phantomHullModIds) {
            if (!own.contains(phantomHullModId)) {
                own.add(phantomHullModId);
            }
        }
        return Collections.unmodifiableList(own);
    }

    public String getEquivalentHullModId() {
        List<String> own = getOwnHullModIds();
        if (!own.isEmpty()) {
            return own.get(0);
        }
        return exclusiveHullModIds.isEmpty() ? null : exclusiveHullModIds.get(0);
    }

    public List<String> getPhantomHullModIds() {
        return phantomHullModIds;
    }

    public List<String> getExclusiveSkillTypeIds() {
        return exclusiveSkillTypeIds;
    }

    public boolean isExclusiveWith(SkillType other) {
        if (exclusiveSkillTypeIds.contains(other.getId()) || other.getExclusiveSkillTypeIds().contains(id)) {
            return true;
        }
        return !id.equals(other.getId()) && (excludesOwnHullModOf(other) || other.excludesOwnHullModOf(this));
    }

    private boolean excludesOwnHullModOf(SkillType other) {
        List<String> otherOwn = other.getOwnHullModIds();
        if (otherOwn.isEmpty()) {
            return false;
        }
        for (String hullModId : exclusiveHullModIds) {
            if (otherOwn.contains(hullModId)) {
                return true;
            }
        }
        return false;
    }

    public List<UnlockCondition> getUnlockConditions() {
        return unlockConditions;
    }

    public List<String> getTags() {
        return tags;
    }

    public ModuleShareOverride getModuleShareOverride() {
        return moduleShareOverride;
    }

    public boolean sharesWithModules(HullSize hullSize) {
        if (moduleShareOverride == ModuleShareOverride.NONE || vanillaHullModId != null || temporaryAfterDeploymentSeconds != null) {
            return false;
        }
        return moduleShareOverride == ModuleShareOverride.ALL
                || effectsFor(hullSize).stream().noneMatch(effect -> effect.effect().moduleShare() == ModuleShare.EXCLUDE);
    }

    public List<SkillTypeEffect> moduleSharedEffects(HullSize hullSize) {
        if (!sharesWithModules(hullSize)) {
            return Collections.emptyList();
        }
        return effectsFor(hullSize).stream().filter(effect -> effect.effect().moduleShare().isShared()).toList();
    }

    public ModuleShareExtent moduleShareExtent(HullSize hullSize) {
        boolean shares = sharesWithModules(hullSize);
        int consideredCount = 0;
        int sharedCount = 0;
        for (SkillTypeEffect effect : effectsFor(hullSize)) {
            if (effect.effect() instanceof StationModuleSkillEffect) {
                continue;
            }
            consideredCount++;
            if (shares && (effect.effect().moduleShare().isShared() || AffinitySteps.isAffinity(effect.effect()))) {
                sharedCount++;
            }
        }
        if (consideredCount == 0) {
            return vanillaHullModId == null ? null : ModuleShareExtent.NONE;
        }
        if (sharedCount == 0) {
            return ModuleShareExtent.NONE;
        }
        return sharedCount == consideredCount ? ModuleShareExtent.ALL : ModuleShareExtent.SOME;
    }

    public List<SkillTypeEffect> effectsFor(HullSize hullSize) {
        if (hullSizeEffects.isEmpty()) {
            return effects;
        }
        List<SkillTypeEffect> combined = new ArrayList<>(effects);
        for (HullSizeSkillEffect effect : hullSizeEffects) {
            combined.add(new SkillTypeEffect(effect.effect(), effect.valueFor(hullSize)));
        }
        return combined;
    }
}
