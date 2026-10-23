package exiledsector.charts;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import exiledsector.items.WeightedPick;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

public final class ChartSites {

    private ChartSites() {
    }

    public static SectorEntityToken addCache(StarSystemAPI system, String runId, int tier, ChartGenContext context, Vector2f at, Random random) {
        List<ChartPoi> caches = ChartPois.ofKind(ChartPois.lootAt(ChartPois.all(), tier), ChartPoi.Kind.CACHE);
        ChartPoi poi = WeightedPick.pick(caches, ChartPoi::weight, random);
        if (poi == null || at == null) {
            return null;
        }
        ChartContentEngine engine = ChartContentGenerator.engine();
        SectorEntityToken cache = engine.addSalvageEntity(system, poi.entityType(), poi.specForSalvage(), new Random(random.nextLong()));
        if (cache == null) {
            return null;
        }
        try {
            cache.setFixedLocation(at.x, at.y);
            cache.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SPEC_ID_OVERRIDE, poi.specForSalvage());
            ChartContentGenerator.tagContent(cache, runId);
            ChartVanillaLoot.addToSite(cache, ChartPoi.Kind.CACHE, engine.siteDrops(poi.specForSalvage()),
                    context == null ? ChartGenContext.none() : context, ChartTiers.clamp(tier), new Random(random.nextLong()));
        } catch (RuntimeException | LinkageError e) {
            system.removeEntity(cache);
            throw e;
        }
        return cache;
    }

    public static CargoAPI siteSalvage(SectorEntityToken site, int tier, ChartGenContext context, float mult, Random random) {
        ChartVanillaLoot.addToSite(site, ChartPoi.Kind.STATION, ChartVanillaLoot.SiteDrops.NONE, context == null ? ChartGenContext.none() : context,
                ChartTiers.clamp(tier), random);
        return SalvageEntity.generateSalvage(random, 1f, 1f, Math.max(0f, mult), 1f, site.getDropValue(), site.getDropRandom());
    }
}
