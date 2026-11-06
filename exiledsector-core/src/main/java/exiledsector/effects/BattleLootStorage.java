package exiledsector.effects;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.ExiledSector;
import exiledsector.PlayerFleet;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.items.ItemStore;
import org.apache.log4j.Logger;

public final class BattleLootStorage extends BaseCampaignEventListener {

    private static final Logger LOG = Logger.getLogger(BattleLootStorage.class);

    public BattleLootStorage() {
        super(false);
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        CargoAPI playerCargo = PlayerFleet.cargoOrNull();
        if (battle == null || !battle.isPlayerInvolved() || playerCargo == null) {
            return;
        }
        try {
            ItemStore.get().absorbFrom(playerCargo);
            ExiledSector.enabledDomains().forEach(ExiledSectorDomain::battleLootTaken);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Moving battle loot from cargo into storage failed", e);
        }
    }
}
