package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomaly;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class AnomalousDebris implements ChartAnomaly {

    public static final String ID = "anomalous_debris";
    public static final String EXTRA_KEY = ID + ".extra";
    public static final String CURRENCY_KEY = ID + ".currency";
    public static final String TERRAIN_ID = "exiledSector_chart_anomalous_debris";
    public static final String FIELD_TAG = ChartIds.PREFIX + "anomalous_debris";
    public static final AnomalousDebris INSTANCE = new AnomalousDebris();
    static final long LAYOUT_SALT = 0x44425249534c41L;
    static final long FLEET_SALT = 0x44425249534654L;
    static final int CENTRE_ATTEMPTS = 24;
    static final float POI_GAP = 300f;
    static final float POI_SPREAD = 600f;
    static final float MIN_OPEN_DISTANCE = 2000f;
    static final float MAX_OPEN_DISTANCE = 8000f;
    static final float ENTRY_CLEARANCE = 3000f;
    static final float FIELD_GAP = 800f;

    record Field(Vector2f centre, float radius) {
    }

    private AnomalousDebris() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        Random layout = site.random(LAYOUT_SALT);
        int count = DebrisRules.fieldCount(layout, Math.round(site.context().anomalyPercent(EXTRA_KEY)));
        List<Field> fields = pickFields(site, count, layout);
        if (fields.isEmpty()) {
            throw new IllegalStateException("no clear spot for anomalous debris");
        }
        for (int index = 0; index < fields.size(); index++) {
            placeField(site, fields.get(index), site.random(FLEET_SALT + index).nextLong());
        }
    }

    private static void placeField(AnomalyContentSite site, Field field, long seed) {
        StarSystemAPI system = site.system();
        CampaignFleetAPI fleet = DormantFleets.create(site.runId(), site.tier(), site.context(), seed);
        if (fleet == null) {
            return;
        }
        system.addEntity(fleet);
        site.track(fleet);
        fleet.setLocation(field.centre().x, field.centre().y);
        SectorEntityToken terrain = system.addTerrain(TERRAIN_ID, new AnomalousDebrisTerrainPlugin.Params(site.runId(), fleet.getId(), field.radius(), seed));
        site.track(terrain);
        terrain.setFixedLocation(field.centre().x, field.centre().y);
        terrain.addTag(ChartIds.ENTITY_TAG);
        terrain.addTag(ChartIds.runTag(site.runId()));
        terrain.addTag(FIELD_TAG);
    }

    static List<Field> pickFields(AnomalyContentSite site, int count, Random random) {
        List<Field> fields = new ArrayList<>();
        List<SectorEntityToken> pois = site.pois();
        for (int field = 0; field < count; field++) {
            float radius = DebrisRules.fieldRadius(random);
            for (int attempt = 0; attempt < CENTRE_ATTEMPTS; attempt++) {
                Vector2f candidate = attempt < CENTRE_ATTEMPTS / 2 && !pois.isEmpty()
                        ? nearPoi(pois.get(random.nextInt(pois.size())), radius, random)
                        : offset(new Vector2f(), MIN_OPEN_DISTANCE + random.nextFloat() * (MAX_OPEN_DISTANCE - MIN_OPEN_DISTANCE), random);
                if (candidate != null && fits(site, fields, candidate, radius)) {
                    fields.add(new Field(candidate, radius));
                    break;
                }
            }
        }
        return fields;
    }

    static boolean fits(AnomalyContentSite site, List<Field> placed, Vector2f candidate, float radius) {
        if (!site.isClear(candidate) || site.distanceToEntry(candidate) < radius + ENTRY_CLEARANCE) {
            return false;
        }
        for (Field field : placed) {
            if (Vector2f.sub(candidate, field.centre(), new Vector2f()).length() < radius + field.radius() + FIELD_GAP) {
                return false;
            }
        }
        return true;
    }

    private static Vector2f nearPoi(SectorEntityToken poi, float radius, Random random) {
        if (poi.getLocation() == null) {
            return null;
        }
        return offset(poi.getLocation(), poi.getRadius() + radius + POI_GAP + random.nextFloat() * POI_SPREAD, random);
    }

    private static Vector2f offset(Vector2f from, float distance, Random random) {
        double angle = random.nextDouble() * Math.PI * 2d;
        return new Vector2f(from.x + (float) Math.cos(angle) * distance, from.y + (float) Math.sin(angle) * distance);
    }

    @Override
    public boolean isActiveIn(LocationAPI location) {
        return !fieldsIn(location).isEmpty();
    }

    public static List<AnomalousDebrisTerrainPlugin> fieldsIn(LocationAPI location) {
        List<AnomalousDebrisTerrainPlugin> fields = new ArrayList<>();
        if (location == null) {
            return fields;
        }
        for (CampaignTerrainAPI terrain : location.getTerrainCopy()) {
            if (terrain.getPlugin() instanceof AnomalousDebrisTerrainPlugin field) {
                fields.add(field);
            }
        }
        return fields;
    }
}
