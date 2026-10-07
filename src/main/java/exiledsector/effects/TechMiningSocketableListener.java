package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.TechMining;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.socketables.HullFrameworkData;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableDrops;
import exiledsector.socketables.SocketableItemData;
import exiledsector.socketables.SocketableUnlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public class TechMiningSocketableListener implements EconomyTickListener {

    static final String MONTHS_KEY = "$exiledSector_socketTechMiningMonths";
    private static final String DECAY_SETTING = "techMiningDecay";

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
            Random random = new Random((sector.getSeedString() + "|" + market.getId() + "|" + minedMonths).hashCode());
            String dropSite = minedMonths == 0 ? SocketableDrops.TECH_MINING_FIRST_FIND : SocketableDrops.TECH_MINING_MONTHLY;
            float chanceMult = minedMonths == 0 ? 1f : (float) Math.pow(decay, minedMonths - 1);
            List<SocketableItemData> foundItems = SocketableDrops.roll(dropSite, random, chanceMult);
            Map<String, Integer> foundMaterials = SocketableDrops.rollMaterials(dropSite, random, chanceMult);
            SocketableDrops.FrameworkLoot frameworkLoot = SocketableUnlock.frameworksOpen(sector)
                    ? SocketableDrops.rollFrameworkLoot(dropSite, random, chanceMult) : SocketableDrops.FrameworkLoot.NONE;
            marketMemory.set(MONTHS_KEY, minedMonths + 1);
            List<SocketableItemData> allFoundItems = new ArrayList<>(foundItems);
            allFoundItems.addAll(frameworkLoot.items());
            deliver(market, allFoundItems, foundMaterials, frameworkLoot.frameworks());
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

    private static void deliver(MarketAPI minedMarket, List<SocketableItemData> foundItems, Map<String, Integer> foundMaterials,
                                List<HullFrameworkData> foundFrameworks) {
        if (foundItems.isEmpty() && foundMaterials.isEmpty() && foundFrameworks.isEmpty()) {
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
        for (SocketableItemData item : foundItems) {
            storageCargo.addSpecial(item.toSpecialItem(), 1f);
            Socketable preview = item.preview();
            if (preview != null) {
                announce(minedMarket, storageMarket, preview::name);
            }
        }
        for (HullFrameworkData framework : foundFrameworks) {
            storageCargo.addSpecial(framework.toSpecialItem(), 1f);
            announce(minedMarket, storageMarket, () -> framework.preview().name());
        }
        for (Map.Entry<String, Integer> material : foundMaterials.entrySet()) {
            storageCargo.addCommodity(material.getKey(), material.getValue());
            CommoditySpecAPI commoditySpec = Global.getSettings().getCommoditySpec(material.getKey());
            String commodityName = commoditySpec == null ? material.getKey() : commoditySpec.getName();
            announce(minedMarket, storageMarket, () -> Translation.msg("socketable.techMining.material").arg("count", material.getValue())
                    .arg("name", commodityName).text());
        }
    }

    private static void announce(MarketAPI minedMarket, MarketAPI storedAt, Supplier<String> itemName) {
        if (Global.getSector().getCampaignUI() == null) {
            return;
        }
        String foundMessage = I18n.forGameText(() -> {
            String storageText = storedAt == null ? Translation.text("socketable.techMining.cargo")
                    : Translation.msg("socketable.techMining.storage").arg("market", storedAt.getName()).text();
            return Translation.msg("socketable.techMining.found").arg("colony", minedMarket.getName()).arg("name", itemName.get())
                    .arg("destination", storageText).text();
        });
        Global.getSector().getCampaignUI().addMessage(foundMessage, Misc.getPositiveHighlightColor());
    }
}
