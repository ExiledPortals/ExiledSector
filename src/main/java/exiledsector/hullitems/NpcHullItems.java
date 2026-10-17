package exiledsector.hullitems;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.FrameworkSlots;
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

public final class NpcHullItems {

    public static final String ID_PREFIX = HullItemCodec.NPC_PREFIX;
    static final float BASE_CHANCE = 0.05f;
    static final float CHANCE_PER_PLAYER_LEVEL = 0.01f;
    static final int SECOND_ROLL_PLAYER_LEVEL = 15;
    static final float SECOND_CHANCE = 0.05f;
    static final float UNIQUE_SHARE = 0.02f;
    static final float FRAMEWORK_CHANCE = 0.5f;
    static final float UPGRADE_DROP_CHANCE = 0.1f;
    private static final Map<String, HullItem> PREVIEWS = new ConcurrentHashMap<>();

    private NpcHullItems() {
    }

    public static String id(String definitionId, long seed) {
        return HullItemCodec.npc(definitionId, seed);
    }

    public static String id(HullItemData itemData) {
        return HullItemCodec.npc(itemData);
    }

    public static boolean isNpcId(String hullItemId) {
        return HullItemCodec.isNpc(hullItemId);
    }

    public static HullItemData item(String hullItemId) {
        return isNpcId(hullItemId) ? HullItemCodec.decode(hullItemId) : null;
    }

    static HullItem resolve(String hullItemId) {
        HullItemData itemData = item(hullItemId);
        if (itemData == null || itemData.definition() == null) {
            return null;
        }
        return PREVIEWS.computeIfAbsent(hullItemId, itemData::create);
    }

    public static float firstChance(int playerLevel) {
        return BASE_CHANCE + CHANCE_PER_PLAYER_LEVEL * Math.max(0, playerLevel);
    }

    public static float secondChance(int playerLevel) {
        return playerLevel >= SECOND_ROLL_PLAYER_LEVEL ? SECOND_CHANCE : 0f;
    }

    public static int rollCount(int playerLevel, Random random) {
        int hullItemCount = random.nextFloat() < firstChance(playerLevel) ? 1 : 0;
        if (random.nextFloat() < secondChance(playerLevel)) {
            hullItemCount++;
        }
        return hullItemCount;
    }

    public static HullItemDefinition pickDefinition(Random random, Predicate<HullItemDefinition> uniqueAllowed) {
        if (random.nextFloat() < UNIQUE_SHARE) {
            HullItemDefinition uniqueDefinition = HullItemDrops.pickUnique(random,
                    definition -> definition.kind() == HullItemType.SUBROUTINE && uniqueAllowed.test(definition));
            if (uniqueDefinition != null) {
                return uniqueDefinition;
            }
        }
        return HullItemDrops.pickBasic(random);
    }

    public static void rollFramework(ShipSkillData shipData, HullSize hullSize, Supplier<ShipProfile> currentFit, int playerLevel,
                                     Random random) {
        if (!HullItemUnlock.frameworksOpen(playerLevel) || random.nextFloat() >= FRAMEWORK_CHANCE) {
            return;
        }
        ShipProfile fit = currentFit.get();
        List<HullItemType> fittingTypes = new ArrayList<>();
        for (HullItemType socketType : HullItemType.frameworkTypes()) {
            if (FrameworkSlots.unmetRequirement(socketType, fit) == null) {
                fittingTypes.add(socketType);
            }
        }
        List<HullItemType> socketTypes = HullUpgradeRoller.rollTypes(HullUpgradeRoller.rollSocketCount(random), fittingTypes, new Random(random.nextLong()));
        for (HullItemType socketType : socketTypes) {
            shipData.grantUnlockedSocketType(socketType.id());
            if (random.nextFloat() < firstChance(playerLevel)) {
                HullItemDefinition definition = HullItemDrops.pickFrameworkBasic(socketType, random);
                if (definition != null) {
                    shipData.socketFrameworkItem(socketType.id(), id(HullItemData.rolled(definition, random.nextLong())));
                }
            }
        }
    }

    public static boolean carriesFramework(ShipSkillData shipData) {
        return shipData != null && !shipData.getUnlockedSocketTypeIds().isEmpty();
    }

    public static HullUpgradeData rollUpgradeDrop(ShipSkillData shipData, HullSize hullSize, Random random) {
        return carriesFramework(shipData) && hullSize != null && random.nextFloat() < UPGRADE_DROP_CHANCE ? new HullUpgradeData(hullSize) : null;
    }

    public static List<HullItem> uniquesCarriedBy(ShipSkillData shipData) {
        List<HullItem> carriedUniques = new ArrayList<>();
        for (String hullItemId : shipData == null ? List.<String>of() : shipData.getSocketedItems().values()) {
            HullItemData itemData = item(hullItemId);
            if (itemData != null && itemData.definition() != null && itemData.definition().unique()) {
                HullItem hullItem = resolve(hullItemId);
                if (hullItem != null) {
                    carriedUniques.add(hullItem);
                }
            }
        }
        return carriedUniques;
    }

    public static List<HullItemData> carriedBy(ShipSkillData shipData) {
        return shipData == null ? new ArrayList<>() : itemsFor(shipData.getInstalledHullItemIds());
    }

    public static List<HullItemData> subroutinesCarriedBy(ShipSkillData shipData) {
        return shipData == null ? new ArrayList<>() : itemsFor(shipData.getSocketedItems().values());
    }

    private static List<HullItemData> itemsFor(Collection<String> hullItemIds) {
        List<HullItemData> carriedItems = new ArrayList<>();
        for (String hullItemId : hullItemIds) {
            HullItemData itemData = item(hullItemId);
            if (itemData != null) {
                carriedItems.add(itemData);
            }
        }
        return carriedItems;
    }

    public static void claimForPlayer(ShipSkillData shipData) {
        for (Map.Entry<String, String> socketedEntry : new ArrayList<>(shipData.getSocketedItems().entrySet())) {
            HullItemData itemData = item(socketedEntry.getValue());
            if (itemData == null) {
                continue;
            }
            shipData.unsocketItem(socketedEntry.getKey());
            HullItem ownedHullItem = HullItemStore.get().add(itemData);
            if (ownedHullItem != null) {
                shipData.socketItem(socketedEntry.getKey(), ownedHullItem.id());
            }
        }
        Map<String, String> frameworkItems = Map.copyOf(shipData.getFrameworkSocketedItems());
        frameworkItems.forEach((socketTypeId, hullItemId) -> {
            HullItemData itemData = item(hullItemId);
            if (itemData == null) {
                return;
            }
            shipData.unsocketFrameworkItem(socketTypeId);
            HullItem ownedHullItem = HullItemStore.get().add(itemData);
            if (ownedHullItem != null) {
                shipData.socketFrameworkItem(socketTypeId, ownedHullItem.id());
            }
        });
    }

    public static void storeForPlayer(ShipSkillData shipData) {
        for (HullItemData itemData : carriedBy(shipData)) {
            HullItemStore.get().add(itemData);
        }
    }

    public static void clearCache() {
        PREVIEWS.clear();
    }
}
