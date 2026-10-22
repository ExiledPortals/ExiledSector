package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledlib.skills.SkillTypeEffect;
import exiledsector.items.HullItem;
import exiledsector.skills.tags.NodeRequirements;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.items.ShipSection;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class ShipSections {

    public record UnlockedSection(ShipSection section, Item item, String unmetRequirement) {

        public boolean active() {
            return unmetRequirement == null;
        }

        public boolean appliesItem() {
            return item != null && active() && HullItem.fits(item, section);
        }
    }

    private record CachedSections(int revision, String hullId, Set<String> hullModIds, List<UnlockedSection> sections) {
    }

    private static final Map<ShipSkillData, CachedSections> CACHE = new WeakHashMap<>();

    private ShipSections() {
    }

    public static List<UnlockedSection> of(ShipSkillData shipData, Supplier<ShipProfile> currentFit) {
        if (shipData == null || shipData.getUnlockedSocketTypeIds().isEmpty()) {
            return List.of();
        }
        List<UnlockedSection> sections = new ArrayList<>();
        ShipProfile resolvedFit = null;
        for (String sectionId : shipData.getUnlockedSocketTypeIds()) {
            ShipSection section = ShipSection.byIdOrNull(sectionId);
            if (section == null) {
                continue;
            }
            String unmetRequirement = null;
            if (section.requirementTag() != null && currentFit != null) {
                if (resolvedFit == null) {
                    resolvedFit = currentFit.get();
                }
                unmetRequirement = unmetRequirement(section, resolvedFit);
            }
            sections.add(new UnlockedSection(section, ItemStore.lookup(shipData.getSectionItem(sectionId)), unmetRequirement));
        }
        return Collections.unmodifiableList(sections);
    }

    public static List<UnlockedSection> forVariant(ShipSkillData shipData, ShipVariantAPI variant) {
        if (shipData == null || shipData.getUnlockedSocketTypeIds().isEmpty()) {
            return List.of();
        }
        String hullId = variant == null || variant.getHullSpec() == null ? null : variant.getHullSpec().getHullId();
        Collection<String> hullModIds = variant == null ? List.of() : variant.getHullMods();
        CachedSections cached = CACHE.get(shipData);
        if (cached != null && cached.revision() == shipData.revision() && Objects.equals(cached.hullId(), hullId)
                && cached.hullModIds().size() == hullModIds.size() && cached.hullModIds().containsAll(hullModIds)) {
            return cached.sections();
        }
        List<UnlockedSection> sections = of(shipData, () -> variant == null ? null : FittedShipProfile.profile(variant.getHullSpec(), variant, shipData));
        CACHE.put(shipData, new CachedSections(shipData.revision(), hullId, Set.copyOf(hullModIds), sections));
        return sections;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static String unmetRequirement(ShipSection section, ShipProfile fit) {
        if (section.requirementTag() == null || fit == null) {
            return null;
        }
        return NodeRequirements.firstUnmet(List.of(section.requirementTag()), fit);
    }

    public static List<SkillTypeEffect> appliedEffects(ShipSkillData shipData, UnlockedSection section, HullSize hullSize) {
        if (!section.appliesItem()) {
            return List.of();
        }
        List<SkillTypeEffect> applied = new ArrayList<>();
        for (SkillTypeEffect effect : section.item().skillEffects(hullSize)) {
            if (!shipData.isNpcBuild() || effect.effect().appliesToNpcShips()) {
                applied.add(effect);
            }
        }
        return applied;
    }
}
