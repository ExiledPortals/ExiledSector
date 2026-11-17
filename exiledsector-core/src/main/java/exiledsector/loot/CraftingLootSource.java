package exiledsector.loot;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.data.RandomRounding;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.crafting.ItemDisassembly;
import exiledsector.items.crafting.material.MaterialItems;
import exiledsector.items.drop.CraftingDrops;
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
        BattleLootRates.Rate rate = BattleLootRates.rate(BattleLootRates.FEEDSTOCK_RATE);
        if (rate == null) {
            return 0f;
        }
        float battleDeploymentPoints = FleetLoss.totalDeploymentPoints(engagementLosses);
        float feedstock = 0f;
        for (FleetLoss loss : engagementLosses) {
            FleetLootRules rules = loss.rules();
            if (rules.kind() == FleetLootRules.Kind.SPENT) {
                continue;
            }
            float expected = rules.kind() == FleetLootRules.Kind.FEEDSTOCK_ONLY ? rate.expected(loss.deploymentPoints(), battleDeploymentPoints)
                    : rate.expectedAtDifficulty(loss.deploymentPoints(), battleDeploymentPoints, loss.averageDifficulty());
            feedstock += expected * rules.multipliers().quantity(DropType.FEEDSTOCK);
        }
        return feedstock;
    }

    static Map<String, Integer> rollCraftingItems(List<FleetLoss> engagementLosses, float levelScaledShare, Random random) {
        BattleLootRates.Rate rate = BattleLootRates.rate(BattleLootRates.CRAFTING_ITEM_RATE);
        CraftingDrops.Split split = CraftingDrops.split(CraftingDrops.OUTSIDE_CHARTS, FleetLootRules.LEVEL_SCALED.multipliers());
        float expectedCount = rate == null ? 0f : rate.expectedFromAll(engagementLosses, levelScaledShare,
                loss -> loss.rules().kind().rollsBattleCraftingItems() ? split.meanQuantity() : 0f);
        return split.roll(expectedCount, random);
    }
}
