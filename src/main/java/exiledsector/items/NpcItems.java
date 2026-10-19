package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.tags.ShipProfile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class NpcItems {

    public static final String ID_PREFIX = ItemCodec.NPC_PREFIX;
    static final float BASE_CHANCE = 0.05f;
    static final float CHANCE_PER_PLAYER_LEVEL = 0.01f;
    static final int SECOND_ROLL_PLAYER_LEVEL = 15;
    static final float SECOND_CHANCE = 0.05f;
    static final float UNIQUE_SHARE = 0.02f;
    static final float SECTION_ITEM_CHANCE = 0.5f;
    static final float UPGRADE_DROP_CHANCE = 0.1f;
    private static final Map<String, Item> PREVIEWS = new ConcurrentHashMap<>();

    private NpcItems() {
    }

    public static String id(String definitionId, long seed) {
        return ItemCodec.npc(definitionId, seed);
    }

    public static String id(ItemData itemData) {
        return ItemCodec.npc(itemData);
    }

    public static boolean isNpcId(String itemId) {
        return ItemCodec.isNpc(itemId);
    }

    public static ItemData item(String itemId) {
        return isNpcId(itemId) ? ItemCodec.decode(itemId) : null;
    }

    static Item resolve(String itemId) {
        ItemData itemData = item(itemId);
        if (itemData == null || itemData.definition() == null) {
            return null;
        }
        return PREVIEWS.computeIfAbsent(itemId, itemData::create);
    }

    public static float firstChance(int playerLevel) {
        return BASE_CHANCE + CHANCE_PER_PLAYER_LEVEL * Math.max(0, playerLevel);
    }

    public static float secondChance(int playerLevel) {
        return playerLevel >= SECOND_ROLL_PLAYER_LEVEL ? SECOND_CHANCE : 0f;
    }

    public static int rollCount(int playerLevel, Random random) {
        int itemCount = random.nextFloat() < firstChance(playerLevel) ? 1 : 0;
        if (random.nextFloat() < secondChance(playerLevel)) {
            itemCount++;
        }
        return itemCount;
    }

    public static ItemDefinition pickDefinition(Random random, Predicate<ItemDefinition> uniqueAllowed) {
        return pickDefinition(random, null, uniqueAllowed);
    }

    public static ItemDefinition pickDefinition(Random random, String fleetFactionId, Predicate<ItemDefinition> uniqueAllowed) {
        if (random.nextFloat() < UNIQUE_SHARE) {
            ItemDefinition uniqueDefinition = ItemDrops.pickUniqueFrom(UniqueSource.Kind.FACTION, fleetFactionId, random,
                    definition -> definition.type() == ItemType.SUBROUTINE && uniqueAllowed.test(definition));
            if (uniqueDefinition != null) {
                return uniqueDefinition;
            }
        }
        return ItemDrops.pickBasic(random);
    }

    public static void rollSectionItems(ShipSkillData shipData, HullSize hullSize, Supplier<ShipProfile> currentFit, int playerLevel,
                                     Random random) {
        if (!ItemUnlock.sectionsOpen(playerLevel) || random.nextFloat() >= SECTION_ITEM_CHANCE) {
            return;
        }
        ShipProfile fit = currentFit.get();
        List<ShipSection> fittingSections = new ArrayList<>();
        for (ShipSection section : ShipSection.all()) {
            if (ShipSections.unmetRequirement(section, fit) == null) {
                fittingSections.add(section);
            }
        }
        List<ShipSection> unlockedSections = HullUpgradeRoller.rollSections(HullUpgradeRoller.rollSectionCount(random), fittingSections,
                new Random(random.nextLong()));
        for (ShipSection section : unlockedSections) {
            shipData.grantUnlockedSocketType(section.id());
            if (random.nextFloat() < firstChance(playerLevel)) {
                ItemDefinition definition = ItemDrops.pickSectionItemBasic(section.itemType(), random);
                if (definition != null) {
                    shipData.installInSection(section.id(), id(ItemData.rolled(definition, random.nextLong())));
                }
            }
        }
    }

    public static boolean carriesSectionItem(ShipSkillData shipData) {
        return shipData != null && !shipData.getUnlockedSocketTypeIds().isEmpty();
    }

    public static HullUpgradeData rollUpgradeDrop(ShipSkillData shipData, HullSize hullSize, Random random) {
        return carriesSectionItem(shipData) && hullSize != null && random.nextFloat() < UPGRADE_DROP_CHANCE ? new HullUpgradeData(hullSize) : null;
    }

    public static List<Item> uniquesCarriedBy(ShipSkillData shipData) {
        List<Item> carriedUniques = new ArrayList<>();
        for (String itemId : shipData == null ? List.<String>of() : shipData.getSocketedItems().values()) {
            ItemData itemData = item(itemId);
            if (itemData != null && itemData.definition() != null && itemData.definition().unique()) {
                Item item = resolve(itemId);
                if (item != null) {
                    carriedUniques.add(item);
                }
            }
        }
        return carriedUniques;
    }

    public static List<ItemData> carriedBy(ShipSkillData shipData) {
        return shipData == null ? new ArrayList<>() : itemsFor(shipData.getInstalledItemIds());
    }

    public static List<ItemData> subroutinesCarriedBy(ShipSkillData shipData) {
        return shipData == null ? new ArrayList<>() : itemsFor(shipData.getSocketedItems().values());
    }

    private static List<ItemData> itemsFor(Collection<String> itemIds) {
        List<ItemData> carriedItems = new ArrayList<>();
        for (String itemId : itemIds) {
            ItemData itemData = item(itemId);
            if (itemData != null) {
                carriedItems.add(itemData);
            }
        }
        return carriedItems;
    }

    public static void claimForPlayer(ShipSkillData shipData) {
        for (Map.Entry<String, String> socketedEntry : new ArrayList<>(shipData.getSocketedItems().entrySet())) {
            ItemData itemData = item(socketedEntry.getValue());
            if (itemData == null) {
                continue;
            }
            shipData.unsocketItem(socketedEntry.getKey());
            Item ownedItem = ItemStore.get().add(itemData);
            if (ownedItem != null) {
                shipData.socketItem(socketedEntry.getKey(), ownedItem.id());
            }
        }
        Map<String, String> sectionItems = Map.copyOf(shipData.getSectionItems());
        sectionItems.forEach((socketTypeId, itemId) -> {
            ItemData itemData = item(itemId);
            if (itemData == null) {
                return;
            }
            shipData.removeFromSection(socketTypeId);
            Item ownedItem = ItemStore.get().add(itemData);
            if (ownedItem != null) {
                shipData.installInSection(socketTypeId, ownedItem.id());
            }
        });
    }

    public static void storeForPlayer(ShipSkillData shipData) {
        for (ItemData itemData : carriedBy(shipData)) {
            ItemStore.get().add(itemData);
        }
    }

    public static void clearCache() {
        PREVIEWS.clear();
    }
}
