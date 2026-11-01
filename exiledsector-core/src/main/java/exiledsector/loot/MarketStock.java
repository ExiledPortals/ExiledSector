package exiledsector.loot;

import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import exiledsector.items.ItemData;

import java.util.List;
import java.util.Random;

public interface MarketStock {

    boolean isOwnListing(ItemData listing);

    List<ItemData> restock(SubmarketAPI submarket, Random random);
}
