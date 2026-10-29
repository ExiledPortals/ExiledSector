package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.TechMining;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.util.Misc;
import exiledsector.Seeds;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemDrops;
import exiledsector.items.MaterialItems;
import exiledsector.items.ItemData;
import exiledsector.items.ShipCurrency;
import exiledsector.loot.LootSources;
import exiledsector.ui.CampaignMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public class TechMiningItemListener implements EconomyTickListener {

    static final String MONTHS_KEY = "$exiledSector_socketTechMiningMonths";
    private static final String DECAY_SETTING = "techMiningDecay";

    public record Find(SectorAPI sector, MarketAPI market, int minedMonths, String dropSite, float chanceMult, Random random, List<ItemData> items,
                       List<ShipCurrency> upgrades) {
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
    }

    @Override
    public void reportEconomyMonthEnd() {
        SectorAPI sector = Global.getSector();
        if (sector == null || sector.getEconomy() == null) {
            return;
        }
        float decay = Global.getSettings().getFloat(DECAY_SETTING);
        for (MarketAPI market : sector.getEconomy().getMarketsCopy()) {
            Industry techMining = market.isPlayerOwned() ? market.getIndustry(Industries.TECHMINING) : null;
            if (techMining == null || !techMining.isFunctional()) {
                continue;
            }
            MemoryAPI marketMemory = market.getMemoryWithoutUpdate();
            int minedMonths = monthsMined(marketMemory, techMining, decay);
            Random random = Seeds.random(sector.getSeedString() + "|" + market.getId() + "|" + minedMonths);
            String dropSite = minedMonths == 0 ? ItemDrops.TECH_MINING_FIRST_FIND : ItemDrops.TECH_MINING_MONTHLY;
            float chanceMult = minedMonths == 0 ? 1f : (float) Math.pow(decay, minedMonths - 1);
            Find find = new Find(sector, market, minedMonths, dropSite, chanceMult, random, new ArrayList<>(ItemDrops.roll(dropSite, random, chanceMult)),
                    new ArrayList<>());
            Map<String, Integer> foundMaterials = ItemDrops.rollMaterials(dropSite, random, chanceMult);
            LootSources.all().forEach(source -> source.addTechMiningFinds(find));
            marketMemory.set(MONTHS_KEY, minedMonths + 1);
            deliver(market, find.items(), foundMaterials, find.upgrades());
        }
    }

    static int monthsMined(MemoryAPI memory, Industry techMining, float decay) {
        if (memory.contains(MONTHS_KEY)) {
            return memory.getInt(MONTHS_KEY);
        }
        if (techMining instanceof TechMining vanillaTechMining && decay > 0f && decay < 1f) {
            float vanillaMult = vanillaTechMining.getTechMiningMult();
            if (vanillaMult > 0f && vanillaMult < 1f) {
                int vanillaFinds = Math.round((float) (Math.log(vanillaMult) / Math.log(decay)));
                return Math.max(0, vanillaFinds - 1);
            }
        }
        return 0;
    }

    private static void deliver(MarketAPI minedMarket, List<ItemData> foundItems, Map<String, Integer> foundMaterials,
                                List<ShipCurrency> foundUpgrades) {
        if (foundItems.isEmpty() && foundMaterials.isEmpty() && foundUpgrades.isEmpty()) {
            return;
        }
        MarketAPI storageMarket = Global.getSector().getPlayerFaction().getProduction().getGatheringPoint();
        CargoAPI storageCargo = storageMarket == null ? null : Misc.getStorageCargo(storageMarket);
        if (storageCargo == null) {
            storageMarket = minedMarket;
            storageCargo = Misc.getStorageCargo(minedMarket);
        }
        if (storageCargo == null) {
            storageMarket = null;
            storageCargo = Global.getSector().getPlayerFleet().getCargo();
        }
        MarketAPI storedAt = storageMarket;
        ItemLootListener.deliverSiteLoot(storageCargo, new ItemDrops.SiteLoot(foundItems, foundUpgrades, foundMaterials),
                itemName -> announce(minedMarket, storedAt, itemName),
                materials -> materials.forEach((itemId, quantity) -> {
                    String materialName = MaterialItems.displayName(itemId);
                    announce(minedMarket, storedAt, () -> Translation.msg("socketable.techMining.material").arg("count", quantity)
                            .arg("name", materialName).text());
                }));
    }

    private static void announce(MarketAPI minedMarket, MarketAPI storedAt, Supplier<String> itemName) {
        if (!CampaignMessages.available()) {
            return;
        }
        String foundMessage = I18n.forGameText(() -> {
            String storageText = storedAt == null ? Translation.text("socketable.techMining.cargo")
                    : Translation.msg("socketable.techMining.storage").arg("market", storedAt.getName()).text();
            return Translation.msg("socketable.techMining.found").arg("colony", minedMarket.getName()).arg("name", itemName.get())
                    .arg("destination", storageText).text();
        });
        CampaignMessages.plain(foundMessage, Misc.getPositiveHighlightColor());
    }
}
