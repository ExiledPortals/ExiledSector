package exiledsector.persistence;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class OpSpentSlotManager {

    public static final int SLOT_COUNT = 10000;

    private static final String SLOTS_KEY = "exiledSector_opSpentSlots";
    private static final String LEGACY_NEXT_SLOT_KEY = "exiledSector_opSpentNextSlot";

    private static boolean exhaustionReported;

    private OpSpentSlotManager() {
    }

    public static int slotFor(String shipId) {
        Map<String, Integer> slots = getSlots(Global.getSector().getPersistentData());
        Integer slot = slots.get(shipId);
        if (slot != null && slot < SLOT_COUNT) {
            return slot;
        }

        int free = lowestFreeSlot(slots);
        if (free >= SLOT_COUNT) {
            reportExhaustion(shipId);
            return SLOT_COUNT;
        }
        slots.put(shipId, free);
        return free;
    }

    public static Integer existingSlot(String shipId) {
        return getSlots(Global.getSector().getPersistentData()).get(shipId);
    }

    public static boolean isAssigned(int slot) {
        return getSlots(Global.getSector().getPersistentData()).containsValue(slot);
    }

    public static void releaseUnless(Predicate<String> stillNeeded) {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        persistentData.remove(LEGACY_NEXT_SLOT_KEY);
        getSlots(persistentData).keySet().removeIf(shipId -> !stillNeeded.test(shipId));
    }

    // persistentData is a raw Object map; this key is only ever written as Map<String, Integer>
    @SuppressWarnings("unchecked")
    private static Map<String, Integer> getSlots(Map<String, Object> persistentData) {
        return (Map<String, Integer>) persistentData.computeIfAbsent(SLOTS_KEY, key -> new HashMap<String, Integer>());
    }

    private static int lowestFreeSlot(Map<String, Integer> slots) {
        BitSet taken = new BitSet(SLOT_COUNT);
        for (Integer slot : slots.values()) {
            if (slot != null && slot >= 0 && slot < SLOT_COUNT) {
                taken.set(slot);
            }
        }
        return taken.nextClearBit(0);
    }

    private static void reportExhaustion(String shipId) {
        if (exhaustionReported) {
            return;
        }
        exhaustionReported = true;
        Logger.getLogger(OpSpentSlotManager.class).error("[ExiledSector] All " + SLOT_COUNT + " OP-reservation hullmod "
                + "slots are held by ships with paid nodes; ship " + shipId + " will not reserve ordnance points.");
    }
}
