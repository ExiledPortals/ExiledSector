package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.hull.HullItem;
import exiledsector.items.hull.ShipSection;
import exiledsector.items.pattern.HullPatternClones;
import exiledsector.skills.tags.NodeRequirements;
import exiledsector.skills.tags.ShipProfile;

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

    public record UnlockedSection(ShipSection section, Item item, String unmetRequirement, boolean cloned) {

        public UnlockedSection(ShipSection section, Item item, String unmetRequirement) {
            this(section, item, unmetRequirement, false);
        }

        public boolean active() {
            return unmetRequirement == null;
        }

        public boolean appliesItem() {
            return item != null && active() && HullItem.fits(item, section);
        }
    }

    private record CachedSections(int revision, String hullId, Set<String> hullModIds, Object cloneStamp, List<UnlockedSection> sections) {
    }

    private static final Map<ShipSkillData, CachedSections> CACHE = new WeakHashMap<>();

    private ShipSections() {
    }

    public static List<UnlockedSection> of(ShipSkillData shipData, Supplier<ShipProfile> currentFit) {
        if (shipData == null || shipData.getUnlockedSectionIds().isEmpty()) {
            return List.of();
        }
        return of(shipData, currentFit, HullPatternClones.sourceFor(shipData));
    }

    private static List<UnlockedSection> of(ShipSkillData shipData, Supplier<ShipProfile> currentFit, HullPatternClones.Source cloneSource) {
        List<UnlockedSection> sections = new ArrayList<>();
        ShipProfile resolvedFit = null;
        for (String sectionId : shipData.getUnlockedSectionIds()) {
            ShipSection section = ShipSection.byIdOrNull(sectionId);
            if (section == null) {
                continue;
            }
            Item item = ItemStore.lookup(shipData.getSectionItem(sectionId));
            boolean cloned = false;
            if (item == null && cloneSource != null) {
                item = cloneSource.itemFor(section);
                cloned = item != null;
            }
            HullItem restrictedItem = item instanceof HullItem hullItem && hullItem.requirementTag() != null ? hullItem : null;
            String unmetRequirement = null;
            if ((section.requirementTag() != null || restrictedItem != null) && currentFit != null) {
                if (resolvedFit == null) {
                    resolvedFit = currentFit.get();
                }
                unmetRequirement = unmetRequirement(section, resolvedFit);
                if (unmetRequirement == null && restrictedItem != null) {
                    unmetRequirement = restrictedItem.unmetRequirement(resolvedFit);
                }
            }
            sections.add(new UnlockedSection(section, item, unmetRequirement, cloned));
        }
        return Collections.unmodifiableList(sections);
    }

    public static List<UnlockedSection> forVariant(ShipSkillData shipData, ShipVariantAPI variant) {
        if (shipData == null || shipData.getUnlockedSectionIds().isEmpty()) {
            return List.of();
        }
        String hullId = variant == null || variant.getHullSpec() == null ? null : variant.getHullSpec().getHullId();
        Collection<String> hullModIds = variant == null ? List.of() : variant.getHullMods();
        HullPatternClones.Source cloneSource = HullPatternClones.sourceFor(shipData);
        Object cloneStamp = cloneSource == null ? null : cloneSource.stamp();
        CachedSections cached = CACHE.get(shipData);
        if (cached != null && cached.revision() == shipData.revision() && Objects.equals(cached.hullId(), hullId)
                && cached.hullModIds().size() == hullModIds.size() && cached.hullModIds().containsAll(hullModIds)
                && Objects.equals(cached.cloneStamp(), cloneStamp)) {
            return cached.sections();
        }
        List<UnlockedSection> sections = of(shipData, () -> variant == null ? null : FittedShipProfile.profile(variant.getHullSpec(), variant, shipData),
                cloneSource);
        CACHE.put(shipData, new CachedSections(shipData.revision(), hullId, Set.copyOf(hullModIds), cloneStamp, sections));
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

    public static List<SkillTypeEffect> appliedEffects(UnlockedSection section, HullSize hullSize) {
        return section.appliesItem() ? section.item().skillEffects(hullSize) : List.of();
    }

    public static ShipSection targetSection(UnlockedSection section) {
        return section.appliesItem() && section.item() instanceof HullItem hullItem ? hullItem.targetSection() : null;
    }
}
