package exiledlib.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HybridBallisticSlots {

    static final String HULL_TAG = "exiledLib_hybridBallisticSlots";
    static final String SLOT_TAG_PREFIX = HULL_TAG + ":";
    static final String CLONE = "clone";
    static final String SET_WEAPON_TYPE = "setWeaponType";
    private static final MethodType CLONE_TYPE = MethodType.methodType(Object.class);
    private static final MethodType SET_WEAPON_TYPE_TYPE = MethodType.methodType(void.class, WeaponType.class);
    private static final Logger LOG = Logger.getLogger(HybridBallisticSlots.class);
    private static final Map<Class<?>, MethodHandle> CLONERS = new HashMap<>();
    private static final Map<Class<?>, MethodHandle> TYPE_SETTERS = new HashMap<>();
    private static boolean failureReported;

    private HybridBallisticSlots() {
    }

    public static boolean converted(ShipHullSpecAPI hullSpec) {
        return hullSpec != null && hullSpec.hasTag(HULL_TAG);
    }

    public static void convert(ShipVariantAPI variant) {
        ShipHullSpecAPI hullSpec = variant == null ? null : variant.getHullSpec();
        if (hullSpec == null || converted(hullSpec) || variant.getSource() == VariantSource.STOCK || ballisticSlotIds(hullSpec).isEmpty()) {
            return;
        }
        ShipHullSpecAPI copy = copyOf(hullSpec);
        if (copy == null) {
            return;
        }
        for (String slotId : ballisticSlotIds(copy)) {
            if (!retype(copy.getWeaponSlotAPI(slotId), WeaponType.HYBRID)) {
                return;
            }
            copy.addTag(SLOT_TAG_PREFIX + slotId);
        }
        copy.addTag(HULL_TAG);
        variant.setHullSpecAPI(copy);
    }

    public static void restore(ShipVariantAPI variant) {
        ShipHullSpecAPI hullSpec = variant == null ? null : variant.getHullSpec();
        if (!converted(hullSpec)) {
            return;
        }
        ShipHullSpecAPI copy = copyOf(hullSpec);
        if (copy == null) {
            return;
        }
        for (String tag : new ArrayList<>(copy.getTags())) {
            if (tag.startsWith(SLOT_TAG_PREFIX)) {
                WeaponSlotAPI slot = copy.getWeaponSlotAPI(tag.substring(SLOT_TAG_PREFIX.length()));
                if (slot != null && !retype(slot, WeaponType.BALLISTIC)) {
                    return;
                }
                copy.getTags().remove(tag);
            }
        }
        copy.getTags().remove(HULL_TAG);
        variant.setHullSpecAPI(copy);
    }

    static List<String> ballisticSlotIds(ShipHullSpecAPI hullSpec) {
        List<String> slotIds = new ArrayList<>();
        for (WeaponSlotAPI slot : hullSpec.getAllWeaponSlotsCopy()) {
            if (slot.getWeaponType() == WeaponType.BALLISTIC && !slot.isBuiltIn()) {
                slotIds.add(slot.getId());
            }
        }
        return slotIds;
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
        LOG.warn("Ballistic slots can't be made hybrid (" + reason + "); ships keep their ballistic slots", cause);
    }
}
