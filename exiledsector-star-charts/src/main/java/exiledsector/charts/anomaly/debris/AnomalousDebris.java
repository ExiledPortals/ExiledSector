package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.ChartIds;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.AnomalyTerrains;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemDrops;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class AnomalousDebris implements ContentPlacer, RunParticipant, LootShaper {

    public static final String ANOMALY_ID = "anomalous_debris";
    public static final String EXTRA_KEY = ChartEffect.CHART_EXTRA_ANOMALOUS_DEBRIS.anomalyTuningKey();
    public static final String CURRENCY_KEY = ChartEffect.CHART_ANOMALOUS_DEBRIS_CURRENCY_QUANTITY_PERCENT.anomalyTuningKey();
    public static final String TERRAIN_ID = "exiledSector_chart_anomalous_debris";
    public static final String FIELD_TAG = ChartIds.PREFIX + "anomalous_debris";
    public static final AnomalousDebris INSTANCE = new AnomalousDebris();
    static final long LAYOUT_SALT = 0x44425249534c41L;
    static final long FLEET_SALT = 0x44425249534654L;
    static final float POI_GAP = 300f;
    static final float POI_SPREAD = 600f;
    static final float ENTRY_CLEARANCE = 3000f;
    static final float FIELD_GAP = 800f;

    record Field(Vector2f centre, float radius) {
    }

    private AnomalousDebris() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
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
        CampaignFleetAPI fleet = DormantFleets.create(site.runId(), site.tier(), site.context(), seed);
        if (fleet == null) {
            return;
        }
        site.addFleet(fleet, field.centre());
        site.addTerrain(TERRAIN_ID, new AnomalousDebrisTerrainPlugin.Params(site.runId(), fleet.getId(), field.radius(), seed), field.centre(), FIELD_TAG);
    }

    static List<Field> pickFields(AnomalyContentSite site, int count, Random random) {
        List<Field> fields = new ArrayList<>();
        for (int field = 0; field < count; field++) {
            float radius = DebrisRules.fieldRadius(random);
            Vector2f centre = site.findSpot((poi, spotRandom) -> poi.getRadius() + radius + POI_GAP + spotRandom.nextFloat() * POI_SPREAD,
                    candidate -> fits(site, fields, candidate, radius), random);
            if (centre != null) {
                fields.add(new Field(centre, radius));
            }
        }
        return fields;
    }

    static boolean fits(AnomalyContentSite site, List<Field> placed, Vector2f candidate, float radius) {
        if (site.distanceToEntry(candidate) < radius + ENTRY_CLEARANCE) {
            return false;
        }
        for (Field field : placed) {
            if (Vector2f.sub(candidate, field.centre(), new Vector2f()).length() < radius + field.radius() + FIELD_GAP) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String intelText(AnomalyRun run) {
        return fieldsIn(run.system()).isEmpty() ? null : intelLine();
    }

    @Override
    public void preloadTextures() {
        AnomalousDebrisTerrainPlugin.preloadTextures();
    }

    @Override
    public InteractionDialogPlugin dialogFor(AnomalyRun run, SectorEntityToken target) {
        return DormantFleets.isDebrisFleet(target) ? new DormantFleetDialog() : null;
    }

    @Override
    public ItemDrops.SiteLoot extraFleetLoot(CampaignFleetAPI fleet, ChartRun run, ChartGenContext context, Random random) {
        return DormantFleetLoot.currencyFor(fleet, run.tier(), context, random);
    }

    public static List<AnomalousDebrisTerrainPlugin> fieldsIn(LocationAPI location) {
        return AnomalyTerrains.in(location, AnomalousDebrisTerrainPlugin.class);
    }
}
