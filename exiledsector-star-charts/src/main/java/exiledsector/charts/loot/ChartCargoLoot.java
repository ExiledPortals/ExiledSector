package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.effects.ItemLootListener;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemType;
import exiledsector.items.ItemUnlock;

import java.util.List;
import java.util.Random;

public final class ChartCargoLoot {

    private ChartCargoLoot() {
    }

    public static ItemDrops.SiteLoot rollItems(SectorAPI sector, ChartRun run, ChartGenContext context, String siteId, float itemMult, Random random) {
        if (!(itemMult > 0f)) {
            return ItemDrops.SiteLoot.NONE;
        }
        ItemDrops.SiteLoot items = ItemDrops.rollSite(siteId, random, context.itemChances().scaled(itemMult),
                context.dropMult(DropType.FEEDSTOCK) * itemMult, context.dropMult(DropType.CURRENCY) * itemMult,
                ItemUnlock.available(ItemType.HULL_ITEM, sector));
        ChartDrops.Loot charts = ChartDrops.roll(ChartDrops.Source.SYSTEM_SALVAGE, run.tier(), context.dropMult(DropType.STAR_CHART) * itemMult,
                context.dropMult(DropType.CURRENCY) * itemMult, random, ChartDrops.uniquesFor(sector));
        return items.plus(charts.charts(), List.of(), charts.currencies());
    }

    public static CargoAPI roll(SectorAPI sector, ChartRun run, String scope, String siteId, float steps, float itemMult, Random random) {
        ChartGenContext context = ChartRunEffects.contextOf(run);
        ChartVanillaLoot.SiteDrops drops = ChartVanillaLoot.drops(scope, run.tier(), steps, context);
        CargoAPI cargo = SalvageEntity.generateSalvage(random, 1f, 1f, 1f, 1f, drops.value(), drops.random());
        ItemLootListener.addSiteLoot(cargo, rollItems(sector, run, context, siteId, itemMult, random));
        cargo.sort();
        return cargo;
    }
}
