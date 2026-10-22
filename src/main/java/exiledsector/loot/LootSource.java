package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import exiledsector.effects.TechMiningItemListener;
import exiledsector.items.ItemChances;

import java.util.Random;

public interface LootSource {

    default void recordEngagement(BattleLoot battle, BattleEngagement engagement) {
    }

    default void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
    }

    default void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
    }

    default ItemChances itemChancesFor(CampaignFleetAPI fleet) {
        return null;
    }

    default LootModifiers modifiersAt(LocationAPI location) {
        return null;
    }

    default void addSalvageLoot(SectorAPI sector, SectorEntityToken salvageEntity, CargoAPI loot) {
    }

    default void addTechMiningFinds(TechMiningItemListener.Find find) {
    }

    default void stockSubmarket(SubmarketAPI submarket, CargoAPI cargo, Random random) {
    }
}
