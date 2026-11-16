package exiledsector.charts.objective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.DropOrigin;
import exiledsector.loot.Loot;
import exiledsector.loot.LootDelivery;
import exiledsector.loot.LootSource;
import org.apache.log4j.Logger;

public final class BossDefeatLootSource implements LootSource {

    public static final BossDefeatLootSource INSTANCE = new BossDefeatLootSource();
    private static final Logger LOG = Logger.getLogger(BossDefeatLootSource.class);

    private static final class FallenBossLoot {
        private Loot loot = Loot.NONE;
    }

    private BossDefeatLootSource() {
    }

    private FallenBossLoot fallenBossLoot(BattleLoot battle) {
        return battle.state(this, FallenBossLoot::new);
    }

    @Override
    public void recordEngagement(BattleLoot battle, BattleEngagement engagement) {
        FallenBossLoot fallen = fallenBossLoot(battle);
        engagement.lostEnemiesBySourceFleet().forEach((sourceFleet, members) -> {
            try {
                fallen.loot = fallen.loot.plus(ChartObjectives.bossFellInBattle(Global.getSector(), sourceFleet, members));
            } catch (RuntimeException | LinkageError e) {
                LOG.error("[ExiledSector] Checking whether the Star Chart boss fell in battle failed", e);
            }
        });
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        LootDelivery.toLootScreen(loot, fallenBossLoot(battle).loot, DropOrigin.BATTLE);
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        LootDelivery.deliver(playerCargo, fallenBossLoot(battle).loot, DropOrigin.BATTLE);
    }
}
