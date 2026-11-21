package exiledsector.loot;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.crafting.material.MaterialItems;
import exiledsector.items.modifier.ItemRarity;
import exiledsector.items.schematic.ShipSchematic;
import exiledsector.ui.CampaignMessages;
import exiledsector.ui.cargo.MaterialDropAnnouncer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class LootDelivery {

    public interface Announcer {

        void announceItems(List<ItemData> items);

        void announceSchematics(List<ShipSchematic> schematics);

        void announceMaterials(Map<String, Integer> materials);
    }

    public static final Announcer EACH_FIND = eachFind(LootDelivery::announceSalvaged, MaterialDropAnnouncer::announce);
    public static final Announcer ITEMS_BY_RARITY = new Announcer() {

        @Override
        public void announceItems(List<ItemData> items) {
            announceByRarity(items);
        }

        @Override
        public void announceSchematics(List<ShipSchematic> schematics) {
            EACH_FIND.announceSchematics(schematics);
        }

        @Override
        public void announceMaterials(Map<String, Integer> materials) {
            EACH_FIND.announceMaterials(materials);
        }
    };

    private LootDelivery() {
    }

    public static Announcer eachFind(Consumer<Supplier<String>> announceFind, Consumer<Map<String, Integer>> announceMaterials) {
        return new Announcer() {

            @Override
            public void announceItems(List<ItemData> items) {
                for (ItemData item : items) {
                    Item previewItem = item.preview();
                    if (previewItem != null) {
                        announceFind.accept(previewItem::name);
                    }
                }
            }

            @Override
            public void announceSchematics(List<ShipSchematic> schematics) {
                schematics.forEach(schematic -> announceFind.accept(schematic::displayName));
            }

            @Override
            public void announceMaterials(Map<String, Integer> materials) {
                announceMaterials.accept(materials);
            }
        };
    }

    public static void toLootScreen(CargoAPI cargo, Loot loot, DropOrigin origin) {
        add(cargo, shaped(loot, origin));
    }

    public static void deliver(CargoAPI cargo, Loot loot, DropOrigin origin) {
        deliver(cargo, loot, origin, EACH_FIND);
    }

    public static void deliver(CargoAPI cargo, Loot loot, DropOrigin origin, Announcer announcer) {
        addAndAnnounce(cargo, shaped(loot, origin), announcer);
    }

    public static void deliverFoundAt(CargoAPI cargo, Loot loot, LocationAPI foundAt, Announcer announcer) {
        addAndAnnounce(cargo, loot.withItems(LootSources.shape(loot.items(), DropOrigin.ANYWHERE, foundAt)), announcer);
    }

    private static void addAndAnnounce(CargoAPI cargo, Loot loot, Announcer announcer) {
        add(cargo, loot);
        announcer.announceItems(loot.items());
        announcer.announceSchematics(loot.schematics());
        announcer.announceMaterials(loot.materials());
    }

    private static Loot shaped(Loot loot, DropOrigin origin) {
        return loot.withItems(LootSources.shape(loot.items(), origin));
    }

    private static void add(CargoAPI cargo, Loot loot) {
        loot.items().forEach(item -> cargo.addSpecial(item.toSpecialItem(), 1f));
        loot.schematics().forEach(schematic -> cargo.addSpecial(schematic.toSpecialItem(), 1f));
        loot.materials().forEach((itemId, quantity) -> MaterialItems.add(cargo, itemId, quantity));
    }

    private static void announceByRarity(List<ItemData> items) {
        Map<ItemRarity, List<Item>> previewsByRarity = new EnumMap<>(ItemRarity.class);
        for (ItemData item : items) {
            Item previewItem = item.preview();
            if (previewItem != null) {
                previewsByRarity.computeIfAbsent(previewItem.rarity(), rarity -> new ArrayList<>()).add(previewItem);
            }
        }
        previewsByRarity.forEach((rarity, previews) -> {
            if (rarity == ItemRarity.UNIQUE || previews.size() == 1) {
                previews.forEach(previewItem -> announceSalvaged(previewItem::name));
            } else if (CampaignMessages.available()) {
                String messageKey = rarity == ItemRarity.COMMON ? "socketable.salvagedCommon" : "socketable.salvagedRare";
                String salvageMessage = I18n.forGameText(() -> Translation.msg(messageKey).arg("count", previews.size()).text());
                CampaignMessages.plain(salvageMessage, Misc.getPositiveHighlightColor());
            }
        });
    }

    public static void announceSalvaged(Supplier<String> itemName) {
        if (CampaignMessages.available()) {
            String salvageMessage = I18n.forGameText(() -> Translation.msg("socketable.salvaged").arg("name", itemName.get()).text());
            CampaignMessages.plain(salvageMessage, Misc.getPositiveHighlightColor());
        }
    }
}
