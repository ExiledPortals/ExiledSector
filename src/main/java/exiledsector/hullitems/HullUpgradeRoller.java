package exiledsector.hullitems;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import exiledsector.skills.ShipHullSizes;

public final class HullUpgradeRoller {

    static final float TWO_SOCKET_SHARE = 0.7f;
    static final float THREE_SOCKET_SHARE = 0.2f;

    private HullUpgradeRoller() {
    }

    public static int rollSectionCount(Random random) {
        float roll = random.nextFloat();
        if (roll < TWO_SOCKET_SHARE) {
            return 2;
        }
        return roll < TWO_SOCKET_SHARE + THREE_SOCKET_SHARE ? 3 : 4;
    }

    public static HullSize rollHullSize(Random random) {
        return ShipHullSizes.ORDER.get(random.nextInt(ShipHullSizes.ORDER.size()));
    }

    public static List<ShipSection> rollSections(int sectionCount, List<ShipSection> allowedSections, Random random) {
        List<ShipSection> shuffledSections = new ArrayList<>();
        for (ShipSection allowedSection : allowedSections) {
            if (!shuffledSections.contains(allowedSection)) {
                shuffledSections.add(allowedSection);
            }
        }
        Collections.shuffle(shuffledSections, random);
        return List.copyOf(shuffledSections.subList(0, Math.min(sectionCount, shuffledSections.size())));
    }
}
