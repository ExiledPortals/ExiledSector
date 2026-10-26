package exiledsector.charts.run.cleanup;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

final class ChartUiData {

    static final String GET_MAP_LOCATION = "getCampaignMapLocation";
    static final String SET_MAP_LOCATION = "setCampaignMapLocation";
    static final String SET_COURSE_TARGET = "setCourseTarget";
    private static final Logger LOG = Logger.getLogger(ChartUiData.class);
    private static Class<?> resolvedUiDataClass;
    private static MethodHandle mapLocationGetter;
    private static MethodHandle mapLocationSetter;
    private static MethodHandle courseTargetSetter;
    private static boolean failureReported;

    private ChartUiData() {
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost the map reset, never the close
    @SuppressWarnings("java:S1181")
    static LocationAPI campaignMapLocation(Object uiData) {
        if (uiData == null) {
            return null;
        }
        resolve(uiData.getClass());
        if (mapLocationGetter == null) {
            return null;
        }
        try {
            return mapLocationGetter.invoke(uiData) instanceof LocationAPI location ? location : null;
        } catch (Throwable e) {
            reportFailure("reading the campaign map location failed", e);
            return null;
        }
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost the map reset, never the close
    @SuppressWarnings("java:S1181")
    static boolean forgetCampaignMapLocation(Object uiData) {
        if (uiData == null) {
            return false;
        }
        resolve(uiData.getClass());
        if (mapLocationSetter == null) {
            return false;
        }
        try {
            mapLocationSetter.invoke(uiData, (LocationAPI) null);
            return true;
        } catch (Throwable e) {
            reportFailure("clearing the campaign map location failed", e);
            return false;
        }
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost the course reset, never the close
    @SuppressWarnings("java:S1181")
    static boolean forgetCourseTarget(Object uiData) {
        if (uiData == null) {
            return false;
        }
        resolve(uiData.getClass());
        if (courseTargetSetter == null) {
            return false;
        }
        try {
            courseTargetSetter.invoke(uiData, (SectorEntityToken) null);
            return true;
        } catch (Throwable e) {
            reportFailure("clearing the plotted course target failed", e);
            return false;
        }
    }

    private static synchronized void resolve(Class<?> uiDataClass) {
        if (uiDataClass == resolvedUiDataClass) {
            return;
        }
        resolvedUiDataClass = uiDataClass;
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        mapLocationGetter = find(lookup, uiDataClass, GET_MAP_LOCATION, MethodType.methodType(LocationAPI.class));
        mapLocationSetter = find(lookup, uiDataClass, SET_MAP_LOCATION, MethodType.methodType(void.class, LocationAPI.class));
        courseTargetSetter = find(lookup, uiDataClass, SET_COURSE_TARGET, MethodType.methodType(void.class, SectorEntityToken.class));
        if (mapLocationGetter == null || mapLocationSetter == null) {
            reportFailure(uiDataClass.getName() + " has no public " + GET_MAP_LOCATION + "/" + SET_MAP_LOCATION, null);
        }
    }

    private static MethodHandle find(MethodHandles.Lookup lookup, Class<?> owner, String name, MethodType type) {
        try {
            return lookup.findVirtual(owner, name, type);
        } catch (ReflectiveOperationException | LinkageError | SecurityException e) {
            LOG.debug("No reachable " + name + " on " + owner.getName(), e);
            return null;
        }
    }

    private static void reportFailure(String reason, Throwable cause) {
        if (failureReported) {
            return;
        }
        failureReported = true;
        LOG.warn("[ExiledSector] Star Chart cleanup can't reach the campaign UI data (" + reason
                + "); a closed system may stay in the save until the map is viewed elsewhere", cause);
    }
}
