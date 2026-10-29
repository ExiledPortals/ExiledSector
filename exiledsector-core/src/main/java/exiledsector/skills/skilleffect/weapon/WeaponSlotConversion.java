package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WeaponSlotConversion {

    static final String HULL_TAG = "exiledLib_slotConversion";
    static final String SLOT_TAG_PREFIX = HULL_TAG + ":";
    static final char ORIGINAL_TYPE_SEPARATOR = ':';
    static final String CLONE = "clone";
    static final String SET_WEAPON_TYPE = "setWeaponType";
    private static final MethodType CLONE_TYPE = MethodType.methodType(Object.class);
    private static final MethodType SET_WEAPON_TYPE_TYPE = MethodType.methodType(void.class, WeaponType.class);
    private static final Logger LOG = Logger.getLogger(WeaponSlotConversion.class);
    private static final Map<Class<?>, MethodHandle> CLONERS = new HashMap<>();
    private static final Map<Class<?>, MethodHandle> TYPE_SETTERS = new HashMap<>();
    private static boolean failureReported;

    public record Rule(WeaponType fromType, WeaponSize slotSize, WeaponType toType) {

        boolean matches(WeaponType originalType, WeaponSize size) {
            return (fromType == null || fromType == originalType) && (slotSize == null || slotSize == size) && toType != originalType;
        }
    }

    private WeaponSlotConversion() {
    }

    public static boolean converted(ShipHullSpecAPI hullSpec) {
        return hullSpec != null && hullSpec.hasTag(HULL_TAG);
    }

    public static void sync(ShipVariantAPI variant, List<Rule> rules) {
        ShipHullSpecAPI hullSpec = variant == null ? null : variant.getHullSpec();
        if (hullSpec == null) {
            return;
        }
        Map<String, WeaponType> originalTypes = originalTypes(hullSpec);
        Map<String, WeaponType> targetTypes = variant.getSource() == VariantSource.STOCK ? Map.of() : targetTypes(hullSpec, originalTypes, rules);
        rewrite(variant, hullSpec, originalTypes, targetTypes);
    }

    public static void add(ShipVariantAPI variant, Rule rule) {
        ShipHullSpecAPI hullSpec = variant == null ? null : variant.getHullSpec();
        if (hullSpec == null || rule == null || variant.getSource() == VariantSource.STOCK) {
            return;
        }
        Map<String, WeaponType> originalTypes = originalTypes(hullSpec);
        Map<String, WeaponType> targetTypes = currentTypes(hullSpec, originalTypes);
        for (WeaponSlotAPI slot : hullSpec.getAllWeaponSlotsCopy()) {
            if (slot.isWeaponSlot() && !originalTypes.containsKey(slot.getId()) && rule.matches(slot.getWeaponType(), slot.getSlotSize())) {
                targetTypes.put(slot.getId(), rule.toType());
            }
        }
        rewrite(variant, hullSpec, originalTypes, targetTypes);
    }

    public static void restore(ShipVariantAPI variant) {
        sync(variant, List.of());
    }

    public static Set<WeaponType> conflicts(ShipHullSpecAPI hullSpec, List<Rule> rules) {
        Set<WeaponType> conflicting = EnumSet.noneOf(WeaponType.class);
        if (hullSpec == null) {
            return conflicting;
        }
        Map<String, WeaponType> originalTypes = originalTypes(hullSpec);
        for (WeaponSlotAPI slot : hullSpec.getAllWeaponSlotsCopy()) {
            WeaponType originalType = originalTypes.getOrDefault(slot.getId(), slot.getWeaponType());
            Set<WeaponType> targets = EnumSet.noneOf(WeaponType.class);
            for (Rule rule : rules) {
                if (slot.isWeaponSlot() && rule.matches(originalType, slot.getSlotSize())) {
                    targets.add(rule.toType());
                }
            }
            if (targets.size() > 1) {
                conflicting.add(originalType);
            }
        }
        return conflicting;
    }

    static Map<String, WeaponType> targetTypes(ShipHullSpecAPI hullSpec, Map<String, WeaponType> originalTypes, List<Rule> rules) {
        Map<String, WeaponType> targetTypes = new LinkedHashMap<>();
        for (WeaponSlotAPI slot : hullSpec.getAllWeaponSlotsCopy()) {
            if (!slot.isWeaponSlot()) {
                continue;
            }
            WeaponType originalType = originalTypes.getOrDefault(slot.getId(), slot.getWeaponType());
            for (Rule rule : rules) {
                if (rule.matches(originalType, slot.getSlotSize())) {
                    targetTypes.put(slot.getId(), rule.toType());
                    break;
                }
            }
        }
        return targetTypes;
    }

    static Map<String, WeaponType> originalTypes(ShipHullSpecAPI hullSpec) {
        Map<String, WeaponType> originalTypes = new LinkedHashMap<>();
        for (String tag : hullSpec.getTags()) {
            if (tag.startsWith(SLOT_TAG_PREFIX)) {
                String slotAndType = tag.substring(SLOT_TAG_PREFIX.length());
                int separator = slotAndType.lastIndexOf(ORIGINAL_TYPE_SEPARATOR);
                WeaponType originalType = separator < 0 ? null : weaponType(slotAndType.substring(separator + 1));
                if (originalType != null) {
                    originalTypes.put(slotAndType.substring(0, separator), originalType);
                }
            }
        }
        return originalTypes;
    }

    private static WeaponType weaponType(String name) {
        for (WeaponType weaponType : WeaponType.values()) {
            if (weaponType.name().equals(name)) {
                return weaponType;
            }
        }
        return null;
    }

    private static Map<String, WeaponType> currentTypes(ShipHullSpecAPI hullSpec, Map<String, WeaponType> originalTypes) {
        Map<String, WeaponType> currentTypes = new LinkedHashMap<>();
        for (String slotId : originalTypes.keySet()) {
            WeaponSlotAPI slot = hullSpec.getWeaponSlotAPI(slotId);
            if (slot != null) {
                currentTypes.put(slotId, slot.getWeaponType());
            }
        }
        return currentTypes;
    }

    private static boolean isConversionTag(String tag) {
        return tag.equals(HULL_TAG) || tag.startsWith(SLOT_TAG_PREFIX);
    }

    private static void rewrite(ShipVariantAPI variant, ShipHullSpecAPI hullSpec, Map<String, WeaponType> originalTypes,
                                Map<String, WeaponType> targetTypes) {
        if (targetTypes.equals(currentTypes(hullSpec, originalTypes))) {
            return;
        }
        ShipHullSpecAPI copy = copyOf(hullSpec);
        if (copy == null) {
            return;
        }
        for (Map.Entry<String, WeaponType> original : originalTypes.entrySet()) {
            WeaponSlotAPI slot = copy.getWeaponSlotAPI(original.getKey());
            if (slot != null && !retype(slot, original.getValue())) {
                return;
            }
        }
        copy.getTags().removeIf(WeaponSlotConversion::isConversionTag);
        for (Map.Entry<String, WeaponType> target : targetTypes.entrySet()) {
            WeaponSlotAPI slot = copy.getWeaponSlotAPI(target.getKey());
            WeaponType originalType = slot == null ? null : slot.getWeaponType();
            if (slot == null || !retype(slot, target.getValue())) {
                return;
            }
            copy.addTag(SLOT_TAG_PREFIX + target.getKey() + ORIGINAL_TYPE_SEPARATOR + originalType.name());
        }
        if (!targetTypes.isEmpty()) {
            copy.addTag(HULL_TAG);
        }
        variant.setHullSpecAPI(copy);
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost the slot change, never the ship
    @SuppressWarnings("java:S1181")
    private static ShipHullSpecAPI copyOf(ShipHullSpecAPI hullSpec) {
        MethodHandle cloner = handle(CLONERS, hullSpec.getClass(), CLONE, CLONE_TYPE);
        if (cloner == null) {
            return null;
        }
        try {
            if (cloner.invoke(hullSpec) instanceof ShipHullSpecAPI copy && copy != hullSpec) {
                return copy;
            }
            reportFailure(hullSpec.getClass().getName() + "." + CLONE + "() no longer returns a separate hull spec", null);
        } catch (Throwable e) {
            reportFailure("copying " + hullSpec.getHullId() + " failed", e);
        }
        return null;
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost the slot change, never the ship
    @SuppressWarnings("java:S1181")
    private static boolean retype(WeaponSlotAPI slot, WeaponType weaponType) {
        MethodHandle setter = slot == null ? null : handle(TYPE_SETTERS, slot.getClass(), SET_WEAPON_TYPE, SET_WEAPON_TYPE_TYPE);
        if (setter == null) {
            return false;
        }
        try {
            setter.invoke(slot, weaponType);
            return true;
        } catch (Throwable e) {
            reportFailure("retyping slot " + slot.getId() + " failed", e);
            return false;
        }
    }

    private static MethodHandle handle(Map<Class<?>, MethodHandle> cache, Class<?> owner, String methodName, MethodType methodType) {
        if (cache.containsKey(owner)) {
            return cache.get(owner);
        }
        MethodHandle found = resolve(owner, methodName, methodType);
        if (found == null) {
            reportFailure(owner.getName() + " has no public " + methodName + "()", null);
        }
        cache.put(owner, found);
        return found;
    }

    private static MethodHandle resolve(Class<?> owner, String methodName, MethodType methodType) {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        for (Class<?> candidate = owner; candidate != null; candidate = candidate.getSuperclass()) {
            try {
                return lookup.findVirtual(candidate, methodName, methodType);
            } catch (ReflectiveOperationException | LinkageError | SecurityException e) {
                LOG.debug("No reachable " + methodName + "() on " + candidate.getName(), e);
            }
        }
        return null;
    }

    private static void reportFailure(String reason, Throwable cause) {
        if (failureReported) {
            return;
        }
        failureReported = true;
        LOG.warn("Weapon slots can't change type (" + reason + "); ships keep their original slots", cause);
    }
}
