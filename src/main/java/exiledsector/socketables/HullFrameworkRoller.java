package exiledsector.socketables;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class HullFrameworkRoller {

    static final float COMMON_SHARE = 0.7f;
    static final float RARE_THREE_SOCKET_SHARE = 0.2f;
    static final int COMMON_SOCKETS = 2;
    static final int RARE_MIN_SOCKETS = 3;
    static final int RARE_MAX_SOCKETS = 4;
    static final List<HullSize> HULL_SIZES = List.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP);

    private HullFrameworkRoller() {
    }

    public static int rollSocketCount(Random random) {
        float roll = random.nextFloat();
        if (roll < COMMON_SHARE) {
            return COMMON_SOCKETS;
        }
        return roll < COMMON_SHARE + RARE_THREE_SOCKET_SHARE ? RARE_MIN_SOCKETS : RARE_MAX_SOCKETS;
    }

    public static SocketableRarity rarityFor(int socketCount) {
        return socketCount <= COMMON_SOCKETS ? SocketableRarity.COMMON : SocketableRarity.RARE;
    }

    public static HullSize rollHullSize(Random random) {
        return HULL_SIZES.get(random.nextInt(HULL_SIZES.size()));
    }

    public static HullFrameworkData roll(HullSize hullSize, Random random) {
        return roll(hullSize, SocketType.frameworkTypes(), random);
    }

    public static HullFrameworkData roll(HullSize hullSize, List<SocketType> allowedTypes, Random random) {
        int socketCount = rollSocketCount(random);
        long seed = random.nextLong();
        List<SocketType> rolledTypes = rollTypes(socketCount, allowedTypes, new Random(seed));
        if (rolledTypes.isEmpty()) {
            return null;
        }
        return new HullFrameworkData(hullSize, rarityFor(rolledTypes.size()), rolledTypes, seed);
    }

    static List<SocketType> rollTypes(int socketCount, List<SocketType> allowedTypes, Random random) {
        List<SocketType> shuffledTypes = new ArrayList<>();
        for (SocketType allowedType : allowedTypes) {
            if (allowedType.isFramework() && !shuffledTypes.contains(allowedType)) {
                shuffledTypes.add(allowedType);
            }
        }
        Collections.shuffle(shuffledTypes, random);
        return List.copyOf(shuffledTypes.subList(0, Math.min(socketCount, shuffledTypes.size())));
    }
}
