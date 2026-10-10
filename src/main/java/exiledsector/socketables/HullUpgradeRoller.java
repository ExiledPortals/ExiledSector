package exiledsector.socketables;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class HullUpgradeRoller {

    static final float TWO_SOCKET_SHARE = 0.7f;
    static final float THREE_SOCKET_SHARE = 0.2f;

    private HullUpgradeRoller() {
    }

    public static int rollSocketCount(Random random) {
        float roll = random.nextFloat();
        if (roll < TWO_SOCKET_SHARE) {
            return 2;
        }
        return roll < TWO_SOCKET_SHARE + THREE_SOCKET_SHARE ? 3 : 4;
    }

    public static HullSize rollHullSize(Random random) {
        return HullUpgradeData.HULL_SIZES.get(random.nextInt(HullUpgradeData.HULL_SIZES.size()));
    }

    public static List<SocketType> rollTypes(int socketCount, List<SocketType> allowedTypes, Random random) {
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
