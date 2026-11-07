package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import java.util.ArrayList;
import java.util.List;

public final class DiyPlanetsCompat {

    public static final String MOD_ID = "diyplanets";
    public static final List<String> TESTED_VERSIONS = List.of("1.0.30");
    public static final CompatTarget TARGET = new CompatTarget("DIY Planets", List.of(MOD_ID), TESTED_VERSIONS, DiyPlanetsCompat::missingFeatures);
    static final String LOCATION_CLASS = "com.fs.starfarer.campaign.BaseLocation";
    static final String VISIT_FIELD = "lastPlayerVisitTimestamp";
    private static final PrivateField<Object> LAST_VISIT = PrivateField.named(LOCATION_CLASS, VISIT_FIELD, long.class,
            "Star Chart systems can't be marked as recently visited, so DIY Planets bar quests may place their stations in one and lose them when it closes");

    private DiyPlanetsCompat() {
    }

    public static void keepBarQuestsOut(StarSystemAPI chartSystem) {
        SectorAPI sector = Global.getSector();
        if (chartSystem == null || sector == null || sector.getClock() == null || !TARGET.isEnabled()) {
            return;
        }
        LAST_VISIT.write(chartSystem, sector.getClock().getTimestamp());
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>();
        if (!LAST_VISIT.reachable()) {
            missing.add(LOCATION_CLASS + "." + VISIT_FIELD + ", set so its bar quests skip open Star Chart systems");
        }
        return missing;
    }
}
