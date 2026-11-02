package exiledsector.charts.anomaly.listeningpost;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.anomaly.AnomalyLoot;
import exiledsector.charts.run.ChartRun;

import java.util.Random;

public final class VaultLoot {

    static final AnomalyLoot LOOT = new AnomalyLoot(ListeningPostRules.LOOT_SCOPE, ListeningPostRules.LOOT_SITE, ListeningPostRules.LOOT_STEPS, ListeningPostRules.LOOT_STEPS_PER_TIER,
            ListeningPostRules.ITEM_MULT, ListeningPost.LOOT_KEY, ListeningPostRules.LEAN_PERCENT);

    private VaultLoot() {
    }

    public static CargoAPI roll(SectorAPI sector, ChartRun run, float share, Random random) {
        return LOOT.roll(sector, run, ListeningPostRules.LEANS, share, random);
    }
}
