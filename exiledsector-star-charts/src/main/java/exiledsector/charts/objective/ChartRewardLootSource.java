package exiledsector.charts.objective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.effects.ItemLootListener;
import exiledsector.items.ItemDrops;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.LootSource;
import org.apache.log4j.Logger;

public final class ChartRewardLootSource implements LootSource {

    public static final ChartRewardLootSource INSTANCE = new ChartRewardLootSource();
    private static final Logger LOG = Logger.getLogger(ChartRewardLootSource.class);

    private static final class Reward {
        private ItemDrops.SiteLoot loot = ItemDrops.SiteLoot.NONE;
    }

    private ChartRewardLootSource() {
    }

    private Reward reward(BattleLoot battle) {
        return battle.state(this, Reward::new);
    }

    @Override
    public void recordEngagement(BattleLoot battle, BattleEngagement engagement) {
        Reward reward = reward(battle);
        engagement.lostEnemiesBySourceFleet().forEach((sourceFleet, members) -> {
            try {
                reward.loot = reward.loot.plus(ChartObjectives.bossFellInBattle(Global.getSector(), sourceFleet, members));
            } catch (RuntimeException | LinkageError e) {
                LOG.error("[ExiledSector] Checking whether the Star Chart boss fell in battle failed", e);
            }
        });
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        ItemLootListener.addBattleSiteLoot(loot, reward(battle).loot);
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        ItemLootListener.deliverBattleSiteLoot(playerCargo, reward(battle).loot);
    }
}
