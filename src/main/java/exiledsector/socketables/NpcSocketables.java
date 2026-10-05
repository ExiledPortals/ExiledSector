package exiledsector.socketables;

import exiledsector.skills.ShipSkillData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcSocketables {

    public static final String ID_PREFIX = "npc:";
    private static final String SEED_SEPARATOR = "/";
    static final float BASE_CHANCE = 0.05f;
    static final float CHANCE_PER_PLAYER_LEVEL = 0.01f;
    static final int SECOND_ROLL_PLAYER_LEVEL = 15;
    static final float SECOND_CHANCE = 0.05f;
    private static final Map<String, Socketable> PREVIEWS = new ConcurrentHashMap<>();

    private NpcSocketables() {
    }

    public static String id(String definitionId, long seed) {
        return ID_PREFIX + definitionId + SEED_SEPARATOR + seed;
    }

    public static boolean isNpcId(String id) {
        return id != null && id.startsWith(ID_PREFIX);
    }

    public static SocketableItemData item(String id) {
        if (!isNpcId(id)) {
            return null;
        }
        String body = id.substring(ID_PREFIX.length());
        int separator = body.lastIndexOf(SEED_SEPARATOR);
        if (separator <= 0) {
            return null;
        }
        try {
            return new SocketableItemData(body.substring(0, separator), Long.parseLong(body.substring(separator + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Socketable resolve(String id) {
        SocketableItemData item = item(id);
        if (item == null || item.definition() == null) {
            return null;
        }
        return PREVIEWS.computeIfAbsent(id, key -> {
            SocketableDefinition definition = item.definition();
            return definition.kind().create(key, definition.id(), item.seed(), SocketableRoller.roll(definition, item.seed()));
        });
    }

    public static float firstChance(int playerLevel) {
        return BASE_CHANCE + CHANCE_PER_PLAYER_LEVEL * Math.max(0, playerLevel);
    }

    public static float secondChance(int playerLevel) {
        return playerLevel >= SECOND_ROLL_PLAYER_LEVEL ? SECOND_CHANCE : 0f;
    }

    public static int rollCount(int playerLevel, Random random) {
        int count = random.nextFloat() < firstChance(playerLevel) ? 1 : 0;
        if (random.nextFloat() < secondChance(playerLevel)) {
            count++;
        }
        return count;
    }

    public static SocketableDefinition pickDefinition(Random random) {
        return SocketableDrops.pickBasic(random);
    }

    public static List<SocketableItemData> carriedBy(ShipSkillData data) {
        List<SocketableItemData> items = new ArrayList<>();
        if (data == null) {
            return items;
        }
        for (String id : data.getSocketedItems().values()) {
            SocketableItemData item = item(id);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    public static void claimForPlayer(ShipSkillData data) {
        for (Map.Entry<String, String> socketed : new ArrayList<>(data.getSocketedItems().entrySet())) {
            SocketableItemData item = item(socketed.getValue());
            if (item == null) {
                continue;
            }
            data.unsocketItem(socketed.getKey());
            SocketableDefinition definition = item.definition();
            if (definition != null) {
                data.socketItem(socketed.getKey(), SocketableStore.get().add(definition, item.seed()).id());
            }
        }
    }

    public static void storeForPlayer(ShipSkillData data) {
        for (SocketableItemData item : carriedBy(data)) {
            SocketableDefinition definition = item.definition();
            if (definition != null) {
                SocketableStore.get().add(definition, item.seed());
            }
        }
    }

    public static void clearCache() {
        PREVIEWS.clear();
    }
}
