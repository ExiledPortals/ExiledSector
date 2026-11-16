package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.loot.DropOrigin;
import exiledsector.loot.Loot;
import exiledsector.loot.LootDelivery;
import exiledsector.loot.LootMultipliers;
import exiledsector.loot.LootSources;

import java.util.Random;

public final class ChartCargoLoot {

    private ChartCargoLoot() {
    }

    private static Loot rollItems(SectorAPI sector, ChartRun run, ChartGenContext context, String siteId, float itemMult,
                                  Random random) {
        if (!(itemMult > 0f)) {
            return Loot.NONE;
        }
        LootMultipliers multipliers = context.lootMultipliers(itemMult, run.tier());
        Loot items = LootSources.rollSite(sector, siteId, random, multipliers);
        ChartDrops.Loot charts = ChartDrops.roll(ChartDrops.Source.SYSTEM_SALVAGE, run.tier(), ChartDrops.Mults.of(context, itemMult), random);
        return items.plus(charts.asLoot());
    }

    public static CargoAPI roll(SectorAPI sector, ChartRun run, String scope, String siteId, float steps, float itemMult, Random random) {
        return roll(sector, run, ChartRunEffects.contextOf(run), scope, siteId, steps, itemMult, random);
    }

    public static CargoAPI roll(SectorAPI sector, ChartRun run, ChartGenContext context, String scope, String siteId, float steps, float itemMult,
                                Random random) {
        return roll(sector, run, context, scope, siteId, steps, itemMult, DropOrigin.ANYWHERE, random);
    }

    public static CargoAPI rollFromBattle(SectorAPI sector, ChartRun run, String scope, String siteId, float steps, float itemMult, Random random) {
        return roll(sector, run, ChartRunEffects.contextOf(run), scope, siteId, steps, itemMult, DropOrigin.BATTLE, random);
    }

    private static CargoAPI roll(SectorAPI sector, ChartRun run, ChartGenContext context, String scope, String siteId, float steps, float itemMult,
                                 DropOrigin origin, Random random) {
        ChartVanillaLoot.SiteDrops drops = ChartVanillaLoot.drops(scope, run.tier(), steps, context);
        CargoAPI cargo = SalvageEntity.generateSalvage(random, 1f, 1f, 1f, 1f, drops.value(), drops.random());
        Loot items = rollItems(sector, run, context, siteId, itemMult, random);
        LootDelivery.toLootScreen(cargo, items, origin);
        cargo.sort();
        return cargo;
    }
}
