package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.campaign.listeners.SubmarketUpdateListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import exiledsector.PlayerFleet;
import exiledsector.compat.RandomAssortmentCompat;
import exiledsector.items.ItemData;
import exiledsector.loot.LootSource;
import exiledsector.loot.LootSources;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MarketItemStock implements SubmarketUpdateListener, ColonyInteractionListener {

    static final String ROLLED_RESTOCK_KEY_PREFIX = "$exiledSector_itemStockRolledAt_";
    private static final Logger LOG = Logger.getLogger(MarketItemStock.class);
    private final Random random;

    public MarketItemStock() {
        this(new Random());
    }

    MarketItemStock(Random random) {
        this.random = random;
    }

    @Override
    public void reportSubmarketCargoAndShipsUpdated(SubmarketAPI submarket) {
        if (!justRestocked(submarket)) {
            return;
        }
        try {
            if (!claimRestock(submarket)) {
                return;
            }
            CargoAPI cargo = submarket.getCargo();
            for (LootSource source : LootSources.all()) {
                source.stockSubmarket(submarket, cargo, random);
            }
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Stocking market items failed", e);
        }
    }

    static boolean justRestocked(SubmarketAPI submarket) {
        return submarket != null && submarket.getMarket() != null && submarket.getCargo() != null
                && submarket.getPlugin() instanceof BaseSubmarketPlugin plugin && plugin.getSinceSWUpdate() == 0f;
    }

    private static boolean claimRestock(SubmarketAPI submarket) {
        if (RandomAssortmentCompat.EXOSHIP_SUBMARKET.equals(submarket.getSpecId())) {
            return true;
        }
        MemoryAPI marketMemory = submarket.getMarket().getMemoryWithoutUpdate();
        if (marketMemory == null) {
            return false;
        }
        String rolledKey = ROLLED_RESTOCK_KEY_PREFIX + submarket.getSpecId();
        long restockTimestamp = Global.getSector().getClock().getTimestamp();
        if (marketMemory.contains(rolledKey) && marketMemory.getLong(rolledKey) == restockTimestamp) {
            return false;
        }
        marketMemory.set(rolledKey, restockTimestamp);
        return true;
    }

    static void unlistBought(CargoAPI cargo) {
        if (cargo == null) {
            return;
        }
        List<ItemData> boughtItems = new ArrayList<>();
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            SpecialItemData special = stack.isSpecialStack() ? stack.getSpecialDataIfSpecial() : null;
            ItemData itemData = ItemData.isMarketListing(special) ? ItemData.of(special) : null;
            if (itemData != null) {
                float size = stack.getSize();
                cargo.removeStack(stack);
                cargo.addSpecial(itemData.toSpecialItem(), size);
                boughtItems.add(itemData);
            }
        }
        if (!boughtItems.isEmpty()) {
            LootSources.all().forEach(source -> source.itemsBought(boughtItems));
        }
    }

    private static void unlistPlayerCargo() {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        try {
            unlistBought(playerFleet == null ? null : playerFleet.getCargo());
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Converting bought market items failed", e);
        }
    }

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        unlistPlayerCargo();
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
        unlistPlayerCargo();
    }

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        unlistPlayerCargo();
    }

    @Override
    public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {
        unlistPlayerCargo();
    }
}
