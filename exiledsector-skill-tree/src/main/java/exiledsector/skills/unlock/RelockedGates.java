package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.CurrentLocationChangedListener;
import exiledsector.compat.LostSectorUnlocks;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RelockedGates {

    static final String PERSISTENT_KEY = "exiledSector_relockedGates";
    private static final Logger LOG = Logger.getLogger(RelockedGates.class);

    private RelockedGates() {
    }

    public static boolean isRelocked(SectorAPI sector, UnlockConditionType conditionType) {
        Set<String> relocked = stored(sector);
        return relocked != null && relocked.contains(conditionType.name());
    }

    public static List<UnlockConditionType> relockOpenAreas(SectorAPI sector) {
        Set<UnlockConditionType> openAreaGates = new LinkedHashSet<>();
        NodeGates.all().values().forEach(conditions -> conditions.forEach(condition -> {
            UnlockConditionType conditionType = condition.getType();
            if (conditionType.isModGate() && conditionType.modGate().modEnabled().test(condition.getKey())
                    && !UnlockConditionOverrides.isDisabled(conditionType) && !isRelocked(sector, conditionType)
                    && conditionType.modGate().passed().test(sector, condition.getKey())) {
                openAreaGates.add(conditionType);
            }
        }));
        relock(sector, openAreaGates);
        return List.copyOf(openAreaGates);
    }

    public static void relock(SectorAPI sector, Collection<UnlockConditionType> conditionTypes) {
        if (sector == null || conditionTypes.isEmpty()) {
            return;
        }
        Set<String> relocked = new LinkedHashSet<>();
        Set<String> previouslyRelocked = stored(sector);
        if (previouslyRelocked != null) {
            relocked.addAll(previouslyRelocked);
        }
        conditionTypes.forEach(conditionType -> relocked.add(conditionType.name()));
        sector.getPersistentData().put(PERSISTENT_KEY, relocked);
    }

    public static boolean reopen(SectorAPI sector, UnlockConditionType conditionType) {
        Set<String> relocked = stored(sector);
        if (relocked == null || !relocked.remove(conditionType.name())) {
            return false;
        }
        if (relocked.isEmpty()) {
            sector.getPersistentData().remove(PERSISTENT_KEY);
        }
        LOG.info("[ExiledSector] Reopened the relocked " + conditionType + " gate");
        return true;
    }

    public static int reopenAll(SectorAPI sector) {
        Set<String> relocked = stored(sector);
        if (relocked == null) {
            return 0;
        }
        sector.getPersistentData().remove(PERSISTENT_KEY);
        return relocked.size();
    }

    @SuppressWarnings("unchecked") // the persistent data map is untyped; only relock() writes this key, always as a Set<String>
    private static Set<String> stored(SectorAPI sector) {
        Map<String, Object> persistentData = sector == null ? null : sector.getPersistentData();
        return persistentData != null && persistentData.get(PERSISTENT_KEY) instanceof Set<?> relocked ? (Set<String>) relocked : null;
    }

    static void recordLocation(SectorAPI sector, LocationAPI location) {
        if (location == null || stored(sector) == null) {
            return;
        }
        if (location == LostSectorUnlocks.frostSystem(sector)) {
            reopen(sector, UnlockConditionType.LOST_SECTOR_FROZEN_HEART_VISITED);
        }
    }

    public static final class LocationListener implements CurrentLocationChangedListener {

        @Override
        public void reportCurrentLocationChanged(LocationAPI previousLocation, LocationAPI currentLocation) {
            recordLocation(Global.getSector(), currentLocation);
        }
    }
}
