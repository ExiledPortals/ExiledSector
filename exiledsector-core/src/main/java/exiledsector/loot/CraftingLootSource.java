package exiledsector.loot;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.data.RandomRounding;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.CraftingDrops;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.MaterialItems;
import exiledsector.skills.skilleffect.logistics.FoundItemQuantity;
import exiledsector.ui.CampaignMessages;
import exiledsector.ui.cargo.MaterialDropAnnouncer;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class CraftingLootSource implements LootSource {

    public static final CraftingLootSource INSTANCE = new CraftingLootSource();

    private CraftingLootSource() {
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        List<FleetLoss> losses = battle.losses().engagementLossesAtRecordTime();
        int wholeFeedstock = RandomRounding.round(feedstock(losses), battle.random());
        if (wholeFeedstock > 0) {
            MaterialItems.add(loot, ItemDisassembly.FEEDSTOCK_ITEM_ID, wholeFeedstock);
        }
        rollCraftingItems(losses, OutsideChartShare.current(), battle.random())
                .forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        List<FleetLoss> losses = battle.losses().engagementLossesAtRecordTime();
        Map<String, Integer> craftingItems = rollCraftingItems(losses, OutsideChartShare.current(), battle.random());
        craftingItems.forEach((itemId, quantity) -> MaterialItems.add(playerCargo, itemId, quantity));
        MaterialDropAnnouncer.announce(craftingItems);
        int wholeFeedstock = RandomRounding.round(feedstock(losses), battle.random());
        if (wholeFeedstock > 0) {
            MaterialItems.add(playerCargo, ItemDisassembly.FEEDSTOCK_ITEM_ID, wholeFeedstock);
            announceFeedstock(wholeFeedstock);
        }
    }

    private static void announceFeedstock(int wholeFeedstock) {
        String feedstockName = MaterialItems.displayName(ItemDisassembly.FEEDSTOCK_ITEM_ID);
        if (CampaignMessages.available()) {
            String salvageMessage = I18n.forGameText(() -> Translation.msg("socketable.salvaged").arg("name",
                    Translation.msg("socketable.techMining.material").arg("count", wholeFeedstock).arg("name", feedstockName).text()).text());
            CampaignMessages.plain(salvageMessage, Misc.getPositiveHighlightColor());
        }
    }

    static float feedstock(List<FleetLoss> engagementLosses) {
        float feedstock = 0f;
        for (FleetLoss loss : engagementLosses) {
            FleetLootRules rules = loss.rules();
            if (rules.kind() == FleetLootRules.Kind.SPENT) {
                continue;
            }
            float bonuses = rules.kind() == FleetLootRules.Kind.FEEDSTOCK_ONLY ? 1f
                    : loss.averageDifficulty() * rules.multipliers().feedstockMult();
            feedstock += loss.deploymentPoints() * BattleFeedstockSetting.feedstockPerDeploymentPoint() * bonuses;
        }
        return feedstock;
    }

    static Map<String, Integer> rollCraftingItems(List<FleetLoss> engagementLosses, float levelScaledShare, Random random) {
        BattleLootRates.Rate rate = BattleLootRates.rate(BattleLootRates.CRAFTING_ITEM_RATE);
        float playerFoundPercent = FoundItemQuantity.craftingItemPercent();
        float expectedCount = rate == null ? 0f : rate.expectedFromAll(engagementLosses, levelScaledShare,
                loss -> craftingItemMult(loss.rules(), playerFoundPercent));
        return CraftingDrops.roll(expectedCount, CraftingDrops.OUTSIDE_CHARTS, CraftingDrops.NO_BAND_SHIFT, random);
    }

    private static float craftingItemMult(FleetLootRules rules, float playerFoundPercent) {
        return rules.kind().rollsBattleCraftingItems() ? rules.multipliers().craftingItems().plusPercent(playerFoundPercent).mult() : 0f;
    }
}
