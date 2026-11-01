package exiledsector.loot;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.ShipCurrency;

import java.util.List;
import java.util.Random;

public record SiteFind(SectorAPI sector, String dropSite, Random random, ItemChances chances, List<ItemData> items, List<ShipCurrency> upgrades) {
}
