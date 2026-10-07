package exiledsector.socketables;

import exiledsector.skills.ShipSkillData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class NpcSocketables {

    public static final String ID_PREFIX = SocketableCodec.NPC_PREFIX;
    static final float BASE_CHANCE = 0.05f;
    static final float CHANCE_PER_PLAYER_LEVEL = 0.01f;
    static final int SECOND_ROLL_PLAYER_LEVEL = 15;
    static final float SECOND_CHANCE = 0.05f;
    static final float UNIQUE_SHARE = 0.02f;
    private static final Map<String, Socketable> PREVIEWS = new ConcurrentHashMap<>();

    private NpcSocketables() {
    }

    public static String id(String definitionId, long seed) {
        return SocketableCodec.npc(definitionId, seed);
    }

    public static String id(SocketableItemData itemData) {
        return SocketableCodec.npc(itemData);
    }

    public static boolean isNpcId(String socketableId) {
        return SocketableCodec.isNpc(socketableId);
    }

    public static SocketableItemData item(String socketableId) {
        return isNpcId(socketableId) ? SocketableCodec.decode(socketableId) : null;
    }

    static Socketable resolve(String socketableId) {
        SocketableItemData itemData = item(socketableId);
        if (itemData == null || itemData.definition() == null) {
            return null;
        }
        return PREVIEWS.computeIfAbsent(socketableId, itemData::create);
    }

    public static float firstChance(int playerLevel) {
        return BASE_CHANCE + CHANCE_PER_PLAYER_LEVEL * Math.max(0, playerLevel);
    }

    public static float secondChance(int playerLevel) {
        return playerLevel >= SECOND_ROLL_PLAYER_LEVEL ? SECOND_CHANCE : 0f;
    }

    public static int rollCount(int playerLevel, Random random) {
        int socketableCount = random.nextFloat() < firstChance(playerLevel) ? 1 : 0;
        if (random.nextFloat() < secondChance(playerLevel)) {
            socketableCount++;
        }
        return socketableCount;
    }

    public static SocketableDefinition pickDefinition(Random random, Predicate<SocketableDefinition> uniqueAllowed) {
        if (random.nextFloat() < UNIQUE_SHARE) {
            SocketableDefinition uniqueDefinition = SocketableDrops.pickUnique(random,
                    definition -> definition.kind() == SocketType.SUBROUTINE && uniqueAllowed.test(definition));
            if (uniqueDefinition != null) {
                return uniqueDefinition;
            }
        }
        return SocketableDrops.pickBasic(random);
    }

    public static List<Socketable> uniquesCarriedBy(ShipSkillData shipData) {
        List<Socketable> carriedUniques = new ArrayList<>();
        for (String socketableId : shipData == null ? List.<String>of() : shipData.getSocketedItems().values()) {
            SocketableItemData itemData = item(socketableId);
            if (itemData != null && itemData.definition() != null && itemData.definition().unique()) {
                Socketable socketable = resolve(socketableId);
                if (socketable != null) {
                    carriedUniques.add(socketable);
                }
            }
        }
        return carriedUniques;
    }

    public static List<SocketableItemData> carriedBy(ShipSkillData shipData) {
        List<SocketableItemData> carriedItems = new ArrayList<>();
        if (shipData == null) {
            return carriedItems;
        }
        for (String socketableId : shipData.getSocketedItems().values()) {
            SocketableItemData itemData = item(socketableId);
            if (itemData != null) {
                carriedItems.add(itemData);
            }
        }
        return carriedItems;
    }

    public static void claimForPlayer(ShipSkillData shipData) {
        for (Map.Entry<String, String> socketedEntry : new ArrayList<>(shipData.getSocketedItems().entrySet())) {
            SocketableItemData itemData = item(socketedEntry.getValue());
            if (itemData == null) {
                continue;
            }
            shipData.unsocketItem(socketedEntry.getKey());
            Socketable ownedSocketable = SocketableStore.get().add(itemData);
            if (ownedSocketable != null) {
                shipData.socketItem(socketedEntry.getKey(), ownedSocketable.id());
            }
        }
    }

    public static void storeForPlayer(ShipSkillData shipData) {
        for (SocketableItemData itemData : carriedBy(shipData)) {
            SocketableStore.get().add(itemData);
        }
    }

    public static void clearCache() {
        PREVIEWS.clear();
    }
}
