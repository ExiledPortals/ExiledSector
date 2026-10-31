package exiledsector.charts.anomaly.heist;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.anomaly.AnomalyLoot;
import exiledsector.charts.run.ChartRun;

import java.util.Random;

public final class VaultLoot {

    static final AnomalyLoot LOOT = new AnomalyLoot(HeistRules.LOOT_SCOPE, HeistRules.LOOT_SITE, HeistRules.LOOT_STEPS, HeistRules.LOOT_STEPS_PER_TIER,
            HeistRules.ITEM_MULT, ListeningPost.LOOT_KEY, HeistRules.LEAN_PERCENT);

    private VaultLoot() {
    }

    public static CargoAPI roll(SectorAPI sector, ChartRun run, float share, Random random) {
        return LOOT.roll(sector, run, HeistRules.LEANS, share, random);
    }
}
