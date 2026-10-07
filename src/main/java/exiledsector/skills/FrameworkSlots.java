package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.tags.NodeRequirements;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.socketables.HullFramework;
import exiledsector.socketables.SocketType;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class FrameworkSlots {

    public record Slot(int index, SocketType type, Socketable item, String unmetRequirement) {

        public boolean active() {
            return unmetRequirement == null;
        }

        public boolean appliesItem() {
            return item != null && active() && item.canSocketInto(type);
        }
    }

    private record CachedSlots(int revision, String hullId, Set<String> hullModIds, List<Slot> slots) {
    }

    private static final Map<ShipSkillData, CachedSlots> CACHE = new WeakHashMap<>();

    private FrameworkSlots() {
    }

    public static HullFramework installedFramework(ShipSkillData shipData) {
        return shipData == null ? null : SocketableStore.lookupFramework(shipData.getInstalledFrameworkId());
    }

    public static List<Slot> of(ShipSkillData shipData, Supplier<ShipProfile> currentFit) {
        HullFramework framework = installedFramework(shipData);
        if (framework == null) {
            return List.of();
        }
        List<Slot> slots = new ArrayList<>(framework.slotCount());
        ShipProfile resolvedFit = null;
        for (int slotIndex = 0; slotIndex < framework.slotCount(); slotIndex++) {
            SocketType socketType = framework.socketType(slotIndex);
            if (socketType == null) {
                continue;
            }
            String unmetRequirement = null;
            if (socketType.requirementTag() != null && currentFit != null) {
                if (resolvedFit == null) {
                    resolvedFit = currentFit.get();
                }
                unmetRequirement = unmetRequirement(socketType, resolvedFit);
            }
            slots.add(new Slot(slotIndex, socketType, SocketableStore.lookup(shipData.getFrameworkSocketedItem(slotIndex)), unmetRequirement));
        }
        return Collections.unmodifiableList(slots);
    }

    public static List<Slot> forVariant(ShipSkillData shipData, ShipVariantAPI variant) {
        if (shipData == null || shipData.getInstalledFrameworkId() == null) {
            return List.of();
        }
        String hullId = variant == null || variant.getHullSpec() == null ? null : variant.getHullSpec().getHullId();
        Collection<String> hullModIds = variant == null ? List.of() : variant.getHullMods();
        CachedSlots cached = CACHE.get(shipData);
        if (cached != null && cached.revision() == shipData.revision() && Objects.equals(cached.hullId(), hullId)
                && cached.hullModIds().size() == hullModIds.size() && cached.hullModIds().containsAll(hullModIds)) {
            return cached.slots();
        }
        List<Slot> slots = of(shipData, () -> variant == null ? null : FrameworkFit.profile(variant.getHullSpec(), variant, shipData));
        CACHE.put(shipData, new CachedSlots(shipData.revision(), hullId, Set.copyOf(hullModIds), slots));
        return slots;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static String unmetRequirement(SocketType socketType, ShipProfile fit) {
        if (socketType.requirementTag() == null || fit == null) {
            return null;
        }
        return NodeRequirements.firstUnmet(List.of(socketType.requirementTag()), fit);
    }

    public static List<SkillTypeEffect> appliedEffects(ShipSkillData shipData, Slot slot, HullSize hullSize) {
        if (!slot.appliesItem()) {
            return List.of();
        }
        List<SkillTypeEffect> applied = new ArrayList<>();
        for (SkillTypeEffect effect : slot.item().skillEffects(hullSize)) {
            if (!shipData.isNpcBuild() || effect.effect().appliesToNpcShips()) {
                applied.add(effect);
            }
        }
        return applied;
    }
}
