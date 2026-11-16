package exiledsector.charts.anomaly.listeningpost;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.data.RandomRounding;
import exiledsector.items.ItemData;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.Loot;

import java.util.List;
import java.util.Map;
import java.util.Random;

final class PicketPrototypes {

    private PicketPrototypes() {
    }

    static boolean dropsFrom(CampaignFleetAPI fleet, ChartGenContext context) {
        return (Pickets.isPicket(fleet) || Pickets.isGuard(fleet)) && DepotTuning.of(context).picketPrototypes();
    }

    static float expected(ChartGenContext context, float lootDeploymentPoints, float battleDeploymentPoints, float difficultyMultiplier) {
        BattleLootRates.Rate rate = BattleLootRates.rate(ListeningPostRules.PICKET_PROTOTYPE_RATE);
        if (rate == null || !(lootDeploymentPoints > 0f)) {
            return 0f;
        }
        return rate.expectedAtDifficulty(lootDeploymentPoints, battleDeploymentPoints, difficultyMultiplier) * context.itemChances().general();
    }

    static Loot roll(SectorAPI sector, ChartGenContext context, float lootDeploymentPoints, float battleDeploymentPoints, float difficultyMultiplier,
                     Random random) {
        int count = RandomRounding.round(expected(context, lootDeploymentPoints, battleDeploymentPoints, difficultyMultiplier), random);
        List<ItemData> prototypes = VaultPrototypes.dropped(sector, count, random);
        return prototypes.isEmpty() ? Loot.NONE : new Loot(prototypes, List.of(), Map.of());
    }
}
