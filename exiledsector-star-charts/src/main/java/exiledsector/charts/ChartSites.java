package exiledsector.charts;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import exiledsector.items.WeightedPick;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;
import java.util.function.Consumer;

public final class ChartSites {

    public record Orbit(SectorEntityToken focus, float angle, float radius, float days) {

        public Vector2f position() {
            Vector2f centre = focus == null || focus.getLocation() == null ? new Vector2f() : focus.getLocation();
            double radians = Math.toRadians(angle);
            return new Vector2f(centre.x + (float) Math.cos(radians) * radius, centre.y + (float) Math.sin(radians) * radius);
        }
    }

    private ChartSites() {
    }

    public static ChartPoi pick(ChartPoi.Kind kind, int tier, Random random) {
        return WeightedPick.pick(ChartPois.ofKind(ChartPois.lootAt(ChartPois.all(), ChartTiers.clamp(tier)), kind), ChartPoi::weight, random);
    }

    public static SectorEntityToken addCache(StarSystemAPI system, String runId, int tier, ChartGenContext context, Vector2f at, Random random) {
        ChartPoi poi = pick(ChartPoi.Kind.CACHE, tier, random);
        if (poi == null || at == null) {
            return null;
        }
        return add(system, runId, tier, null, context, poi, entity -> entity.setFixedLocation(at.x, at.y), random);
    }

    public static SectorEntityToken addSite(StarSystemAPI system, String runId, int tier, String factionId, ChartGenContext context, ChartPoi poi,
                                            Orbit orbit, Random random) {
        if (poi == null || orbit == null) {
            return null;
        }
        return add(system, runId, tier, factionId, context, poi,
                entity -> entity.setCircularOrbit(orbit.focus(), orbit.angle(), orbit.radius(), orbit.days()), random);
    }

    private static SectorEntityToken add(StarSystemAPI system, String runId, int tier, String factionId, ChartGenContext context, ChartPoi poi,
                                         Consumer<SectorEntityToken> position, Random random) {
        ChartContentEngine engine = ChartContentGenerator.engine();
        boolean derelict = poi.kind() == ChartPoi.Kind.DERELICT;
        boolean recoverable = derelict && random.nextFloat() <ChartContentGenerator.DERELICT_RECOVERY_CHANCE;
        Random entityRandom = new Random(random.nextLong());
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
            ChartVanillaLoot.addToSite(site, poi.kind(), derelict ? ChartVanillaLoot.SiteDrops.NONE : engine.siteDrops(poi.specForSalvage()),
                    context == null ? ChartGenContext.none() : context, ChartTiers.clamp(tier), new Random(random.nextLong()));
        } catch (RuntimeException | LinkageError e) {
            system.removeEntity(site);
            throw e;
        }
        return site;
    }

    public static CargoAPI siteSalvage(SectorEntityToken site, int tier, ChartGenContext context, float mult, Random random) {
        ChartVanillaLoot.addToSite(site, ChartPoi.Kind.STATION, ChartVanillaLoot.SiteDrops.NONE, context == null ? ChartGenContext.none() : context,
                ChartTiers.clamp(tier), random);
        return SalvageEntity.generateSalvage(random, 1f, 1f, Math.max(0f, mult), 1f, site.getDropValue(), site.getDropRandom());
    }
}
