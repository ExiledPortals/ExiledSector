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
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableDrops;
import exiledsector.socketables.SocketableItemData;

import java.util.List;
import java.util.Random;

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
            MemoryAPI memory = market.getMemoryWithoutUpdate();
            int months = monthsMined(memory, techMining, decay);
            Random random = new Random((sector.getSeedString() + "|" + market.getId() + "|" + months).hashCode());
            List<SocketableItemData> items = months == 0
                    ? SocketableDrops.roll(SocketableDrops.TECH_MINING_FIRST_FIND, random)
                    : SocketableDrops.roll(SocketableDrops.TECH_MINING_MONTHLY, random, (float) Math.pow(decay, months - 1));
            memory.set(MONTHS_KEY, months + 1);
            deliver(market, items);
        }
    }

    static int monthsMined(MemoryAPI memory, Industry techMining, float decay) {
        if (memory.contains(MONTHS_KEY)) {
            return memory.getInt(MONTHS_KEY);
        }
        if (techMining instanceof TechMining vanilla && decay > 0f && decay < 1f) {
            float mult = vanilla.getTechMiningMult();
            if (mult > 0f && mult < 1f) {
                int vanillaFinds = Math.round((float) (Math.log(mult) / Math.log(decay)));
                return Math.max(0, vanillaFinds - 1);
            }
        }
        return 0;
    }

    private static void deliver(MarketAPI source, List<SocketableItemData> items) {
        if (items.isEmpty()) {
            return;
        }
        MarketAPI destination = Global.getSector().getPlayerFaction().getProduction().getGatheringPoint();
        CargoAPI storage = destination == null ? null : Misc.getStorageCargo(destination);
        if (storage == null) {
            destination = source;
            storage = Misc.getStorageCargo(source);
        }
        if (storage == null) {
            destination = null;
            storage = Global.getSector().getPlayerFleet().getCargo();
        }
        for (SocketableItemData item : items) {
            storage.addSpecial(item.toSpecialItem(), 1f);
            Socketable preview = item.preview();
            if (preview != null && Global.getSector().getCampaignUI() != null) {
                MarketAPI storedAt = destination;
                String message = I18n.forGameText(() -> {
                    String where = storedAt == null ? Translation.text("socketable.techMining.cargo")
                            : Translation.msg("socketable.techMining.storage").arg("market", storedAt.getName()).text();
                    return Translation.msg("socketable.techMining.found").arg("colony", source.getName()).arg("name", preview.name())
                            .arg("destination", where).text();
                });
                Global.getSector().getCampaignUI().addMessage(message, Misc.getPositiveHighlightColor());
            }
        }
    }
}
