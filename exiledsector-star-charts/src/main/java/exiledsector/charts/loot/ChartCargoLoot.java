package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import exiledsector.charts.run.ChartRun;
import exiledsector.loot.DropOrigin;
import exiledsector.loot.Loot;
import exiledsector.loot.LootDelivery;
import exiledsector.loot.LootSources;

import java.util.Random;

public final class ChartCargoLoot {

    private ChartCargoLoot() {
    }

    private static Loot rollItems(SectorAPI sector, ChartLootContext loot, String siteId, float itemMult, Random random) {
        if (!(itemMult > 0f)) {
            return Loot.NONE;
        }
        Loot items = LootSources.rollSite(sector, siteId, random, loot.multipliers(itemMult));
        ChartDrops.Rolled charts = ChartDrops.roll(ChartDrops.Source.SYSTEM_SALVAGE, loot, loot.chartDropMults(itemMult), random);
        return items.plus(charts.asLoot());
    }

    public static CargoAPI roll(SectorAPI sector, ChartRun run, String scope, String siteId, float steps, float itemMult, Random random) {
        return roll(sector, ChartLootContext.of(run), scope, siteId, steps, itemMult, random);
    }

    public static CargoAPI roll(SectorAPI sector, ChartLootContext loot, String scope, String siteId, float steps, float itemMult, Random random) {
        return roll(sector, loot, scope, siteId, steps, itemMult, DropOrigin.ANYWHERE, random);
    }

    public static CargoAPI rollFromBattle(SectorAPI sector, ChartRun run, String scope, String siteId, float steps, float itemMult, Random random) {
        return roll(sector, ChartLootContext.of(run), scope, siteId, steps, itemMult, DropOrigin.BATTLE, random);
    }

    private static CargoAPI roll(SectorAPI sector, ChartLootContext loot, String scope, String siteId, float steps, float itemMult,
                                 DropOrigin origin, Random random) {
        ChartVanillaLoot.SiteDrops drops = ChartVanillaLoot.drops(scope, loot.tier(), steps, loot.effects());
        CargoAPI cargo = SalvageEntity.generateSalvage(random, 1f, 1f, 1f, 1f, drops.value(), drops.random());
        Loot items = rollItems(sector, loot, siteId, itemMult, random);
        LootDelivery.toLootScreen(cargo, items, origin);
        cargo.sort();
        return cargo;
    }
}
