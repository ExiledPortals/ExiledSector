package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.effects.TechMiningItemListener;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

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

    default LootModifiers modifiersFor(SectorEntityToken site) {
        return site == null ? null : modifiersAt(site.getContainingLocation());
    }

    default void addSalvageLoot(SectorAPI sector, SectorEntityToken salvageEntity, CargoAPI loot) {
    }

    default List<ItemData> bossDrops(int chartTier, String bossFactionId, float chanceMult, Random random,
                                       Predicate<ItemDefinition> allowed) {
        return List.of();
    }

    default void addShowcaseLoot(CargoAPI loot, Random random) {
    }

    default void addSiteFinds(SiteFind find) {
    }

    default void addTechMiningFinds(TechMiningItemListener.Find find) {
    }

    default void itemsBought(List<ItemData> boughtItems) {
    }
}
