package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.EngineSlotAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import exiledsector.ui.perf.LoadCounters;
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

record FleetHullVisuals(float centerX, float centerY, List<EngineFlame> flames, List<HullPoint> outline) {

    record EngineFlame(float forwardOffset, float leftOffset, float angleDeg, float width, float length, Color color) {
    }

    static final FleetHullVisuals NONE = new FleetHullVisuals(Float.NaN, Float.NaN, List.of(), List.of());

    static final Color DEFAULT_FLAME_COLOR = new Color(255, 125, 25);
    private static final Logger LOG = Logger.getLogger(FleetHullVisuals.class);
    private static final String HULL_FILE_FOLDER = "data/hulls/";
    private static final String HULL_FILE_SUFFIX = ".ship";
    private static final Map<String, FleetHullVisuals> VISUALS_BY_HULL = new HashMap<>();
    private static final Map<String, HullFile> HULL_FILES_BY_ID = new HashMap<>();

    private record HullFile(float centerX, float centerY, List<HullPoint> outline) {

        static final HullFile MISSING = new HullFile(Float.NaN, Float.NaN, List.of());
    }

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
        HullFile hullFile = hullFile(hullSpec);
        return new FleetHullVisuals(hullFile.centerX(), hullFile.centerY(), flames(hullSpec), hullFile.outline());
    }

    private static HullFile hullFile(ShipHullSpecAPI hullSpec) {
        for (String hullId : new String[]{hullSpec.getHullId(), hullSpec.getBaseHullId()}) {
            HullFile hullFile = hullId == null ? HullFile.MISSING : HULL_FILES_BY_ID.computeIfAbsent(hullId, FleetHullVisuals::readHullFile);
            if (hullFile != HullFile.MISSING) {
                return hullFile;
            }
        }
        return HullFile.MISSING;
    }

    private static HullFile readHullFile(String hullId) {
        String path = HULL_FILE_FOLDER + hullId + HULL_FILE_SUFFIX;
        JSONObject hullJson;
        long startNanos = System.nanoTime();
        try {
            hullJson = Global.getSettings().loadJSON(path);
        } catch (Exception e) {
            LOG.debug("No hull file at " + path + ": " + e.getMessage());
            return HullFile.MISSING;
        } finally {
            LoadCounters.hullFileRead(System.nanoTime() - startNanos);
        }
        if (hullJson == null) {
            return HullFile.MISSING;
        }
        JSONArray center = hullJson.optJSONArray("center");
        float centerX = Float.NaN;
        float centerY = Float.NaN;
        if (center != null && center.length() == 2) {
            centerX = (float) center.optDouble(0, Float.NaN);
            centerY = (float) center.optDouble(1, Float.NaN);
        }
        return new HullFile(centerX, centerY, outline(hullJson));
    }

    private static List<HullPoint> outline(JSONObject hullFile) {
        JSONArray bounds = hullFile.optJSONArray("bounds");
        if (bounds == null || bounds.length() < 6) {
            return List.of();
        }
        List<HullPoint> outline = new ArrayList<>(bounds.length() / 2);
        for (int i = 0; i + 1 < bounds.length(); i += 2) {
            outline.add(new HullPoint((float) bounds.optDouble(i, 0.0), (float) bounds.optDouble(i + 1, 0.0)));
        }
        return List.copyOf(outline);
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
                    Color flameColor = slot.getColor() == null ? DEFAULT_FLAME_COLOR : slot.getColor();
                    flames.add(new EngineFlame(position.x, position.y, slot.getAngle(), slot.getWidth(), slot.getLength(), flameColor));
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
