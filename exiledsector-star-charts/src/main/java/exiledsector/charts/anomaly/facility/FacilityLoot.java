package exiledsector.charts.anomaly.facility;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import exiledsector.charts.loot.ChartCargoLoot;
import exiledsector.charts.run.ChartRun;
import exiledsector.compat.VaultsOfKnowledgeCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class FacilityLoot {

    private FacilityLoot() {
    }

    public static CargoAPI roll(SectorAPI sector, ChartRun run, Random random) {
        CargoAPI cargo = ChartCargoLoot.roll(sector, run, FacilityRules.LOOT_SCOPE, FacilityRules.LOOT_SITE,
                FacilityRules.lootSteps(run.tier()), FacilityRules.ITEM_MULT, random);
        if (VaultsOfKnowledgeCompat.isModEnabled()) {
            cargo.addAll(SalvageEntity.generateSalvage(random, 1f, 1f, 1f, 1f, new ArrayList<>(), databankDrops(run.tier())));
            cargo.sort();
        }
        return cargo;
    }

    static List<DropData> databankDrops(int tier) {
        DropData drop = new DropData();
        drop.group = VaultsOfKnowledgeCompat.DATABANK_DROP_GROUP;
        drop.chances = FacilityRules.databankRolls(tier);
        List<DropData> drops = new ArrayList<>();
        drops.add(drop);
        return drops;
    }
}
