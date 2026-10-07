package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.EngineSlotAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import exiledsector.ui.refit.UiReflection;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class FleetEngineSlots {

    record EngineFlame(float forwardOffset, float leftOffset, float angleDeg, float width, float length, Color color) {
    }

    private static final Logger LOG = Logger.getLogger(FleetEngineSlots.class);
    private static final Map<String, List<EngineFlame>> FLAMES_BY_HULL = new HashMap<>();

    private FleetEngineSlots() {
    }

    static List<EngineFlame> of(ShipHullSpecAPI hullSpec) {
        if (hullSpec == null) {
            return List.of();
        }
        return FLAMES_BY_HULL.computeIfAbsent(hullSpec.getHullId(), hullId -> read(hullSpec));
    }

    private static List<EngineFlame> read(ShipHullSpecAPI hullSpec) {
        try {
            if (!(UiReflection.call(hullSpec, "getEngineSlots") instanceof List<?> engineSlots)) {
                return List.of();
            }
            List<EngineFlame> flames = new ArrayList<>(engineSlots.size());
            for (Object engineSlot : engineSlots) {
                EngineSlotAPI slot = engineSlot instanceof EngineSlotAPI direct ? direct : slotInside(engineSlot);
                if (slot != null) {
                    Vector2f position = slot.computePosition(new Vector2f(), 0f);
                    flames.add(new EngineFlame(position.x, position.y, slot.getAngle(), slot.getWidth(), slot.getLength(), slot.getColor()));
                }
            }
            return List.copyOf(flames);
        } catch (Throwable e) {
            LOG.warn("Couldn't read engine slots for " + hullSpec.getHullId() + "; its fleet sprite flies without engine flames", e);
            return List.of();
        }
    }

    private static EngineSlotAPI slotInside(Object engineSlot) throws Throwable {
        EngineSlotAPI slot = UiReflection.callReturning(engineSlot, EngineSlotAPI.class);
        return slot != null ? slot : UiReflection.callReturning(engineSlot, EngineSlotAPI.class, true);
    }
}
