package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.loot.ChartVanillaLoot;
import exiledsector.items.WeightedPick;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;
import java.util.function.Consumer;

public final class ChartSites {

    private static final Logger LOG = Logger.getLogger(ChartSites.class);

    public record Orbit(SectorEntityToken focus, float angle, float radius, float days) {

        public Vector2f position() {
            Vector2f centre = focus == null || focus.getLocation() == null ? new Vector2f() : focus.getLocation();
            return pointAround(centre, Math.toRadians(angle), radius);
        }
    }

    private ChartSites() {
    }

    public static Vector2f pointAround(Vector2f centre, double radians, float radius) {
        return new Vector2f(centre.x + (float) Math.cos(radians) * radius, centre.y + (float) Math.sin(radians) * radius);
    }

    public static SectorEntityToken centreOf(StarSystemAPI system) {
        return system.getCenter() != null ? system.getCenter() : system.getStar();
    }

    public static ChartPoi pick(ChartPoi.Kind kind, int tier, Random random) {
        return WeightedPick.pick(ChartPois.ofKind(ChartPois.lootAt(ChartPois.all(), ChartTiers.clamp(tier)), kind), ChartPoi::weight, random);
    }

    public static SectorEntityToken addCache(StarSystemAPI system, String runId, int tier, ChartGenContext context, Vector2f at, Random random) {
        ChartPoi poi = pick(ChartPoi.Kind.CACHE, tier, random);
        if (poi == null || at == null) {
            return null;
        }
        return spawn(system, runId, tier, null, context, poi, entity -> entity.setFixedLocation(at.x, at.y), random, random);
    }

    public static SectorEntityToken addSite(StarSystemAPI system, String runId, int tier, String factionId, ChartGenContext context, ChartPoi poi,
                                            Orbit orbit, Random random) {
        if (poi == null || orbit == null) {
            return null;
        }
        return spawn(system, runId, tier, factionId, context, poi,
                entity -> entity.setCircularOrbit(orbit.focus(), orbit.angle(), orbit.radius(), orbit.days()), random, random);
    }

    static SectorEntityToken spawn(StarSystemAPI system, String runId, int tier, String factionId, ChartGenContext context, ChartPoi poi,
                                   Consumer<SectorEntityToken> position, Random salvageRandom, Random lootRandom) {
        ChartContentEngine engine = ChartContentGenerator.engine();
        boolean derelict = poi.kind() == ChartPoi.Kind.DERELICT;
        boolean recoverable = salvageRandom.nextFloat() < ChartContentGenerator.DERELICT_RECOVERY_CHANCE;
        Random entityRandom = new Random(salvageRandom.nextLong());
        SectorEntityToken site = derelict ? engine.addDerelict(system, factionId, poi.derelictType(), recoverable, entityRandom)
                : engine.addSalvageEntity(system, poi.entityType(), poi.specForSalvage(), entityRandom);
        if (site == null) {
            return null;
        }
        try {
            position.accept(site);
            if (!derelict) {
                site.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SPEC_ID_OVERRIDE, poi.specForSalvage());
            }
            ChartContentGenerator.tagContent(site, runId);
        } catch (RuntimeException | LinkageError e) {
            system.removeEntity(site);
            throw e;
        }
        try {
            ChartVanillaLoot.addToSite(site, poi.kind(), derelict ? ChartVanillaLoot.SiteDrops.NONE : engine.siteDrops(poi.specForSalvage()),
                    context == null ? ChartGenContext.none() : context, ChartTiers.clamp(tier), lootRandom);
        } catch (RuntimeException | LinkageError e) {
            system.removeEntity(site);
            LOG.error("[ExiledSector] Adding loot to a Star Chart " + poi.id() + " site failed; the site is left out", e);
            return null;
        }
        return site;
    }
}
