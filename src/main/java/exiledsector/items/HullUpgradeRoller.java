package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.Random;
import exiledsector.skills.ShipHullSizes;

public final class HullUpgradeRoller {

    private HullUpgradeRoller() {
    }

    public static HullSize rollHullSize(Random random) {
        return ShipHullSizes.ORDER.get(random.nextInt(ShipHullSizes.ORDER.size()));
    }
}
