package exiledsector.loot;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.items.ItemData;
import exiledsector.items.drop.ItemChances;
import exiledsector.items.schematic.ShipSchematic;

import java.util.List;
import java.util.Random;

public record SiteFind(SectorAPI sector, String dropSite, Random random, ItemChances chances, List<ItemData> items, List<ShipSchematic> schematics) {
}
