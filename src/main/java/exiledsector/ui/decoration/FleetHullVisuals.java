package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.EngineSlotAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import exiledsector.ui.refit.UiReflection;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

record FleetHullVisuals(float centerX, float centerY, List<EngineFlame> flames) {

    record EngineFlame(float forwardOffset, float leftOffset, float angleDeg, float width, float length, Color color) {
    }

    static final FleetHullVisuals NONE = new FleetHullVisuals(Float.NaN, Float.NaN, List.of());

    private static final Logger LOG = Logger.getLogger(FleetHullVisuals.class);
    private static final String HULL_FILE_FOLDER = "data/hulls/";
    private static final String HULL_FILE_SUFFIX = ".ship";
    private static final Map<String, FleetHullVisuals> VISUALS_BY_HULL = new HashMap<>();

    boolean hasCenter() {
        return !Float.isNaN(centerX) && !Float.isNaN(centerY);
    }

    static FleetHullVisuals of(ShipHullSpecAPI hullSpec) {
        if (hullSpec == null) {
            return NONE;
        }
        return VISUALS_BY_HULL.computeIfAbsent(hullSpec.getHullId(), hullId -> read(hullSpec));
    }

    private static FleetHullVisuals read(ShipHullSpecAPI hullSpec) {
        float[] center = spriteCenter(hullSpec);
        return new FleetHullVisuals(center[0], center[1], flames(hullSpec));
    }

    private static float[] spriteCenter(ShipHullSpecAPI hullSpec) {
        for (String hullId : new String[]{hullSpec.getHullId(), hullSpec.getBaseHullId()}) {
            try {
                JSONObject hullFile = Global.getSettings().loadJSON(HULL_FILE_FOLDER + hullId + HULL_FILE_SUFFIX);
                JSONArray center = hullFile.optJSONArray("center");
                if (center != null && center.length() == 2) {
                    return new float[]{(float) center.getDouble(0), (float) center.getDouble(1)};
                }
            } catch (Exception e) {
                LOG.debug("No hull file at " + HULL_FILE_FOLDER + hullId + HULL_FILE_SUFFIX + ": " + e.getMessage());
            }
        }
        return new float[]{Float.NaN, Float.NaN};
    }

    private static List<EngineFlame> flames(ShipHullSpecAPI hullSpec) {
        try {
            if (!(UiReflection.call(hullSpec, "getEngineSlots") instanceof List<?> engineSlots)) {
                return List.of();
            }
            List<EngineFlame> flames = new ArrayList<>(engineSlots.size());
            for (Object engineSlot : engineSlots) {
                EngineSlotAPI slot = engineSlot instanceof EngineSlotAPI direct ? direct : campaignSlot(engineSlot);
                if (slot != null && !Boolean.TRUE.equals(UiReflection.call(slot, "isSystemActivated"))) {
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

    private static EngineSlotAPI campaignSlot(Object engineSlot) throws Throwable {
        EngineSlotAPI slot = UiReflection.callReturning(engineSlot, EngineSlotAPI.class, true);
        return slot != null ? slot : UiReflection.callReturning(engineSlot, EngineSlotAPI.class);
    }
}
