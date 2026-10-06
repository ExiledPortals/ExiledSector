package exiledsector.socketables;

import exiledsector.skills.ShipSkillData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class NpcSocketables {

    public static final String ID_PREFIX = "npc:";
    private static final String SEED_SEPARATOR = "/";
    private static final String EFFECT_MARKER = ":";
    static final float BASE_CHANCE = 0.05f;
    static final float CHANCE_PER_PLAYER_LEVEL = 0.01f;
    static final int SECOND_ROLL_PLAYER_LEVEL = 15;
    static final float SECOND_CHANCE = 0.05f;
    static final float UNIQUE_SHARE = 0.02f;
    private static final Map<String, Socketable> PREVIEWS = new ConcurrentHashMap<>();

    private NpcSocketables() {
    }

    public static String id(String definitionId, long seed) {
        return ID_PREFIX + definitionId + SEED_SEPARATOR + seed;
    }

    public static String id(SocketableItemData item) {
        String id = id(item.definitionId(), item.seed());
        return item.effects() == null ? id : id + SEED_SEPARATOR + RolledEffect.encode(item.effects());
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
        String last = body.substring(separator + 1);
        if (last.isEmpty() || last.contains(EFFECT_MARKER)) {
            SocketableItemData legacy = item(ID_PREFIX + body.substring(0, separator));
            List<RolledEffect> effects = RolledEffect.decode(last);
            return legacy == null || effects == null ? null : new SocketableItemData(legacy.definitionId(), legacy.seed(), effects, null);
        }
        try {
            return new SocketableItemData(body.substring(0, separator), Long.parseLong(last));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Socketable resolve(String id) {
        SocketableItemData item = item(id);
        if (item == null || item.definition() == null) {
            return null;
        }
        return PREVIEWS.computeIfAbsent(id, item::create);
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

    public static SocketableDefinition pickDefinition(Random random, Predicate<SocketableDefinition> uniqueAllowed) {
        if (random.nextFloat() < UNIQUE_SHARE) {
            SocketableDefinition unique = SocketableDrops.pickUnique(random, uniqueAllowed);
            if (unique != null) {
                return unique;
            }
        }
        return SocketableDrops.pickBasic(random);
    }

    public static List<Socketable> uniquesCarriedBy(ShipSkillData data) {
        List<Socketable> uniques = new ArrayList<>();
        for (String id : data == null ? List.<String>of() : data.getSocketedItems().values()) {
            SocketableItemData item = item(id);
            if (item != null && item.definition() != null && item.definition().unique()) {
                Socketable socketable = resolve(id);
                if (socketable != null) {
                    uniques.add(socketable);
                }
            }
        }
        return uniques;
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
            Socketable owned = SocketableStore.get().add(item);
            if (owned != null) {
                data.socketItem(socketed.getKey(), owned.id());
            }
        }
    }

    public static void storeForPlayer(ShipSkillData data) {
        for (SocketableItemData item : carriedBy(data)) {
            SocketableStore.get().add(item);
        }
    }

    public static void clearCache() {
        PREVIEWS.clear();
    }
}
